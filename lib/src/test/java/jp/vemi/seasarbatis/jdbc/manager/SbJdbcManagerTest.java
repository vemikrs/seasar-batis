package jp.vemi.seasarbatis.jdbc.manager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.seasarbatis.criteria.SimpleWhere;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;
import jp.vemi.seasarbatis.test.entity.TestSbUser;
import jp.vemi.seasarbatis.criteria.OrderDirection;

@ExtendWith(MockitoExtension.class)
class SbJdbcManagerTest {

    private SqlSessionFactory sqlSessionFactory;

    private SBJdbcManager jdbcManager;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/mybatis-test-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        }
        jdbcManager = new SBJdbcManager(sqlSessionFactory);

        // initializeDatabase();
    }

    @SuppressWarnings("unused")
    private void initializeDatabase() throws Exception {

        // スキーマ作成と初期データの投入
        executeSqlScript("/ddl/01_create_test_schema.sql");
        executeSqlScript("/ddl/02_insert_initial_data.sql");
    }

    private void executeSqlScript(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("SQLファイルが見つかりません: " + resourcePath);
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            // セミコロンで分割して、空行以外を実行
            String[] commands = sql.split(";");
            try (SqlSession session = sqlSessionFactory.openSession();
                    Connection conn = session.getConnection();
                    Statement stmt = conn.createStatement()) {
                for (String cmd : commands) {
                    if (!cmd.trim().isEmpty()) {
                        try {
                            int affected = stmt.executeUpdate(cmd);
                            System.out.println("[INFO] SQL実行成功: " + cmd.trim() + " (影響行数: " + affected + ")");
                        } catch (Exception e) {
                            System.err.println("[ERROR] SQL実行失敗: " + cmd.trim());
                            e.printStackTrace();
                            throw e;
                        }
                    }
                }
                session.commit();
            }
        }
    }

    @Test
    void testFindByPk() {
        // テストデータ作成
        TestSbUser expected = TestSbUser.builder()
                .id(1L)
                .name("テストユーザー1")
                .isActive(true)
                .createdAt(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();

        // テストの実行
        TestSbUser actual = jdbcManager.findByPk(TestSbUser.class, 1L);

        // 検証
        assertNotNull(actual);
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
    }

    @Test
    void testFindAll() {
        // テストデータ作成
        List<TestSbUser> expected = Arrays.asList(
                TestSbUser.builder()
                        .id(1L)
                        .name("テストユーザー1")
                        .isActive(true)
                        .build(),
                TestSbUser.builder()
                        .id(2L)
                        .name("テストユーザー2")
                        .isActive(false)
                        .build(),
                TestSbUser.builder()
                        .id(3L)
                        .name("テストユーザー3")
                        .isActive(true)
                        .build());

        // テストの実行
        List<TestSbUser> actual = jdbcManager.findAll(TestSbUser.class);

        // 検証
        assertEquals(3, actual.size());
        assertEquals(expected.get(0).getName(), actual.get(0).getName());
        assertEquals(expected.get(1).getName(), actual.get(1).getName());
    }

    @Test
    void testWhere() {
        // Where条件の作成
        SimpleWhere where = jdbcManager.where()
                .eq("name", "テストユーザー1")
                .eq("is_active", true);

        // テストの実行
        List<TestSbUser> results = jdbcManager.where(TestSbUser.class, where);

        // 検証
        assertFalse(results.isEmpty());
        assertEquals("テストユーザー1", results.get(0).getName());
        assertTrue(results.get(0).getIsActive());
    }

    @Test
    void testInsert() {
        // 事前のレコード数を取得
        int countBefore = jdbcManager.findAll(TestSbUser.class).size();
        try {
            jdbcManager.transaction(manager -> {
                TestSbUser user = TestSbUser.builder()
                        .name("新規ユーザー")
                        .isActive(true)
                        .sequenceNo(1)
                        .amount(1000.0)
                        .build();
                TestSbUser inserted = manager.insert(user);
                assertNotNull(inserted.getId());
                assertEquals(user.getName(), inserted.getName());
                // 強制的な例外発生でトランザクションをロールバック
                throw new RuntimeException("強制ロールバック");
            });
        } catch (RuntimeException e) {
            // ロールバックされたことを想定
        }
        // 事後のレコード数は変わっていないはず
        int countAfter = jdbcManager.findAll(TestSbUser.class).size();
        assertEquals(countBefore, countAfter, "INSERT処理がロールバックされていることを確認");
    }

    @Test
    void testUpdate() {
        // 事前のデータを取得
        TestSbUser original = jdbcManager.findByPk(TestSbUser.class, 1L);
        String originalName = original.getName();
        try {
            jdbcManager.transaction(manager -> {
                TestSbUser user = manager.findByPk(TestSbUser.class, 1L);
                user.setName("更新後の名前");
                TestSbUser updated = manager.updateByPk(user);
                assertEquals("更新後の名前", updated.getName());
                // 強制的な例外発生でトランザクションをロールバック
                throw new RuntimeException("強制ロールバック");
            });
        } catch (RuntimeException e) {
            // ロールバックが想定される
        }
        // 事後、更新前の値に戻っていることを確認
        TestSbUser afterUpdate = jdbcManager.findByPk(TestSbUser.class, 1L);
        assertEquals(originalName, afterUpdate.getName(), "UPDATE処理がロールバックされていることを確認");
    }

    @Test
    void testFrom() {
        // テストの実行
        List<TestSbUser> users = jdbcManager
                .from(TestSbUser.class)
                .where(jdbcManager.where()
                        .eq("is_active", true)
                        .gt("amount", 1000.0))
                .orderBy("sequence_no", OrderDirection.ASC)
                .orderBy("amount", OrderDirection.DESC)
                .getResultList();

        // 検証
        assertFalse(users.isEmpty());
        assertTrue(users.get(0).getIsActive());
        assertTrue(users.get(0).getAmount() > 1000.0);
    }

    @Test
    void testComplexQuery() {
        // 複雑なクエリのテスト
        List<TestSbUser> users = jdbcManager
                .from(TestSbUser.class)
                .where(jdbcManager
                        .complexWhere()
                        .add(jdbcManager.where()
                                .eq("status", "ACTIVE")
                                .gt("score", 80.0))
                        .or()
                        .add(jdbcManager.where()
                                .eq("user_type", "VIP")
                                .isNotNull("preferences")))
                .orderBy("created_at", OrderDirection.DESC)
                .orderBy("score", OrderDirection.DESC)
                .getResultList();

        // 検証
        assertNotNull(users);
    }

    @Test
    void testTransaction() {
        // トランザクションテスト
        assertDoesNotThrow(() -> {
            jdbcManager.transaction(manager -> {
                // 新規ユーザーの作成
                TestSbUser user = TestSbUser.builder()
                        .name("トランザクションテスト")
                        .isActive(true)
                        .build();
                TestSbUser inserted = manager.insert(user);

                // 作成したユーザーの更新
                inserted.setName("更新済み");
                manager.updateByPk(inserted);

                // ここでエラーが発生した場合はロールバック
                if (inserted.getId() == null) {
                    throw new RuntimeException("テストエラー");
                }
            });
        });
    }

    @Test
    void testOrderBy() {
        // 複数のORDER BY句のテスト
        List<TestSbUser> users = jdbcManager
                .from(TestSbUser.class)
                .orderBy("sequence_no", OrderDirection.ASC)
                .orderBy("created_at", OrderDirection.DESC)
                .orderBy("name", OrderDirection.ASC)
                .getResultList();

        // 検証
        assertNotNull(users);
        assertTrue(users.size() > 0);
    }
}