/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc.manager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Time;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;

import jp.vemi.seasarbatis.core.criteria.OrderDirection;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;
import jp.vemi.seasarbatis.jdbc.SBJdbcManagerFactory;
import jp.vemi.seasarbatis.test.entity.TestSbUser;

/**
 * SBJdbcManagerのテストクラス（v0.0.1互換性テスト）。
 * <p>
 * このテストは旧API（SB*クラス）の互換性を確認するためのものです。
 * v0.0.2以降は新API（BatisFluid, JdbcFlow等）の使用を推奨します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 */
@Tag("v0.0.1")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SbJdbcManagerTest {

    private SBJdbcManager jdbcManager;

    @BeforeAll
    void setUpOnce() throws Exception {
        SBJdbcManagerFactory factory = new SBJdbcManagerFactory("mybatis-test-config.xml");
        jdbcManager = factory.create();

        initializeDatabase();
    }

    @BeforeEach
    void setUp() throws Exception {
        // 各テスト前には何もしない（データベースは@BeforeAllで初期化済み）
    }

    private void initializeDatabase() throws Exception {
        // H2使用時とMySQL使用時で異なるDDLを使用
        String databaseType = System.getProperty("test.database", "h2");
        String schemaFile = databaseType.equals("mysql") 
            ? "/ddl/01_create_test_schema.sql" 
            : "/ddl/01_create_h2_schema.sql";
        String dataFile = databaseType.equals("mysql") 
            ? "/ddl/02_insert_initial_data.sql" 
            : "/ddl/02_insert_h2_data.sql";

        // スキーマ作成と初期データの投入
        executeSqlScript(schemaFile);
        executeSqlScript(dataFile);
    }

    private void executeSqlScript(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("SQLファイルが見つかりません: " + resourcePath);
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            // セミコロンで分割して、空行以外を実行
            String[] commands = sql.split(";");
            try (SqlSession session = jdbcManager.getSqlSessionFactory().openSession(true);
                    Connection conn = session.getConnection();
                    Statement stmt = conn.createStatement()) {
                // タイムアウトを設定
                stmt.setQueryTimeout(5);
                for (String cmd : commands) {
                    String trimmed = cmd.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            int affected = stmt.executeUpdate(trimmed);
                            String preview = trimmed.substring(0, Math.min(50, trimmed.length()));
                            System.out.println("[INFO] SQL実行成功: " + preview + "... (影響行数: " + affected + ")");
                        } catch (Exception e) {
                            String preview = trimmed.substring(0, Math.min(50, trimmed.length()));
                            System.err.println("[ERROR] SQL実行失敗: " + preview);
                            e.printStackTrace();
                            throw e;
                        }
                    }
                }
                
                // insertの場合、データが正しく挿入されたか確認
                if (resourcePath.contains("insert")) {
                    try {
                        Statement verifyStmt = conn.createStatement();
                        java.sql.ResultSet rs = verifyStmt.executeQuery("SELECT COUNT(*) as cnt FROM sbtest_users");
                        if (rs.next()) {
                            System.out.println("[INFO] テーブル内データ件数: " + rs.getInt("cnt"));
                        }
                        rs.close();
                        
                        // 実際に挿入されたデータの詳細を確認
                        java.sql.ResultSet dataRs = verifyStmt.executeQuery("SELECT id, name, sequence_no FROM sbtest_users ORDER BY id");
                        System.out.println("[INFO] 挿入されたデータ詳細:");
                        while (dataRs.next()) {
                            System.out.println("  ID: " + dataRs.getLong("id") + ", Name: " + dataRs.getString("name") + ", Sequence: " + dataRs.getInt("sequence_no"));
                        }
                        dataRs.close();
                        verifyStmt.close();
                    } catch (Exception e) {
                        System.out.println("[WARN] データ件数確認でエラー: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Test
    @Order(1)
    @org.junit.jupiter.api.Tag("smoke")
    void testFindByPk() {
        // テストデータ期待値
        TestSbUser expected = TestSbUser.builder()
                .id(1L)
                .sequenceNo(1)
                .name("テストユーザー1")
                .build();

        // テストの実行
        TestSbUser actual = jdbcManager.findByPk(TestSbUser.builder().id(1L).build()).getSingleResult();
        System.out.println(actual);

        // 検証
        assertNotNull(actual);
        // NotNullチェック
        assertNotNull(actual.getId());
        assertNotNull(actual.getSequenceNo());
        assertNotNull(actual.getAmount());
        assertNotNull(actual.getRate());
        assertNotNull(actual.getScore());
        assertNotNull(actual.getIsActive());
        assertNotNull(actual.getName());
        assertNotNull(actual.getDescription());
        assertNotNull(actual.getMemo());
        assertNotNull(actual.getCharCode());
        assertNotNull(actual.getCreatedAt());
        assertNotNull(actual.getUpdatedAt());
        assertNotNull(actual.getBirthDate());
        assertNotNull(actual.getWorkTime());
        assertNotNull(actual.getStatus());
        assertNotNull(actual.getUserType());
        assertNotNull(actual.getPreferences());
        // 一致チェック（一部）
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getSequenceNo(), actual.getSequenceNo());
        assertEquals(expected.getName(), actual.getName());
    }

    @Test
    @Order(2)
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
    @Order(3)
    void testWhere() {
        // Where条件の作成
        SimpleWhere where = jdbcManager.where()
                .eq("name", "テストユーザー1")
                .eq("is_active", true);

        // テストの実行
        List<TestSbUser> results = jdbcManager.from(TestSbUser.class)
                .where(where)
                .getResultList();

        // 検証
        assertFalse(results.isEmpty());
        assertEquals(1, results.size(), "1件のみ取得されること");
        assertEquals("テストユーザー1", results.get(0).getName(), "名前が一致すること");
        assertTrue(results.get(0).getIsActive(), "有効なユーザーであること");
    }

    @Test
    @Order(4)
    void testInsert() {
        // 事前のレコード数を取得
        int countBefore = jdbcManager.findAll(TestSbUser.class).size();
        try {
            jdbcManager.transaction((manager) -> {
                TestSbUser user = TestSbUser.builder()
                        .id(101L)
                        .sequenceNo(101)
                        .amount(1000.0)
                        .rate(0.12F)
                        .score(100.0)
                        .isActive(true)
                        .name("新規ユーザー")
                        .description("一時INSERTユーザです")
                        .memo("メモ～")
                        .charCode("JPX")
                        .createdAt(new java.sql.Timestamp(System.currentTimeMillis()))
                        .updatedAt(new java.sql.Timestamp(System.currentTimeMillis()))
                        .birthDate(new java.sql.Date(System.currentTimeMillis()))
                        .workTime(Time.valueOf("09:00:00"))
                        .status("ACTIVE")
                        .userType("USER")
                        .preferences("{}")
                        .build();
                TestSbUser inserted = manager.insert(user);
                assertNotNull(inserted.getId());
                assertEquals(user.getName(), inserted.getName());
                // 強制的な例外発生の前にトランザクションが確実にアクティブであることを確認
                assertTrue(manager.getTransactionManager().isActive(), "トランザクションがアクティブであること");

                throw new RuntimeException("強制ロールバック");
            }, true); // 独立したトランザクションとして実行
        } catch (RuntimeException e) {
            // ロールバックされたことを想定
        }

        // トランザクションが確実に終了していることを確認
        assertFalse(jdbcManager.getTransactionManager().isActive(), "トランザクションが終了していること");

        int countAfter = jdbcManager.findAll(TestSbUser.class).size();
        assertEquals(countBefore, countAfter, "INSERT処理がロールバックされていることを確認");
    }

    @Test
    @Order(5)
    void testUpdate() {
        // 事前のデータを取得
        TestSbUser original = jdbcManager.findByPk(TestSbUser.builder()
                .id(1L).build()).getSingleResult();
        String originalName = original.getName();
        RuntimeException exception = null;
        try {
            jdbcManager.transaction(manager -> {
                TestSbUser user = manager.findByPk(TestSbUser.builder()
                        .id(1L).build()).getSingleResult();
                user.setName("更新後の名前");
                TestSbUser updated = manager.update(user);
                assertEquals("更新後の名前", updated.getName());
                // 強制的な例外発生でトランザクションをロールバック
                throw new RuntimeException("強制ロールバック");
            }, true);
        } catch (RuntimeException e) {
            // ロールバックされたことを想定
            exception = e;
        }
        // 例外メッセージの検証
        assertNotNull(exception, "RuntimeExceptionがスローされていること");
        assertEquals("強制ロールバック", exception.getCause().getMessage(), "例外メッセージが一致すること");

        // 事後、更新前の値に戻っていることを確認
        TestSbUser afterUpdate = jdbcManager.findByPk(TestSbUser.builder()
                .id(1L).build()).getSingleResult();
        assertEquals(originalName, afterUpdate.getName(), "UPDATE処理がロールバックされていることを確認");

    }

    @Test
    @Order(6)
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
    @Order(7)
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
    @Order(8)
    void testTransaction() {
        // トランザクションテスト
        assertDoesNotThrow(() -> {
            jdbcManager.transaction(manager -> {
                // 新規ユーザーの作成
                TestSbUser user = TestSbUser.builder()
                        .id(102L)
                        .sequenceNo(102)
                        .name("トランザクションテスト")
                        .isActive(true)
                        .build();
                TestSbUser inserted = manager.insert(user);

                // 作成したユーザーの更新
                inserted.setName("更新済み");
                TestSbUser updated = manager.update(inserted);

                // ここでエラーが発生した場合はロールバック
                if (updated.getId() == null) {
                    throw new RuntimeException("テストエラー");
                }
                assertEquals(updated.getName(), "更新済み", "名前が更新されること？");

                int deleted = jdbcManager.delete(TestSbUser.builder().id(102L).build());
                assertEquals(deleted, 1, "Insertされたレコードが削除されること");
            });
        });

    }

    @Test
    @Order(9) // 最後に実行
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