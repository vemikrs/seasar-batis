package jp.vemi.seasarbatis.jdbc.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.seasarbatis.criteria.SimpleWhere;
import jp.vemi.seasarbatis.test.entity.TestSbUser;
import jp.vemi.seasarbatis.criteria.OrderDirection;

@ExtendWith(MockitoExtension.class)
class SbJdbcManagerTest {

    @Mock
    private SqlSessionFactory sqlSessionFactory;

    private SBJdbcManager jdbcManager;

    @BeforeEach
    void setUp() {
        jdbcManager = new SBJdbcManager(sqlSessionFactory);
    }

    @Test
    void testFindByPk() {
        // テストデータ作成
        TestSbUser expected = TestSbUser.builder()
                .id(1L)
                .name("テストユーザー1")
                .isActive(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
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
                        .build());

        // テストの実行
        List<TestSbUser> actual = jdbcManager.findAll(TestSbUser.class);

        // 検証
        assertEquals(2, actual.size());
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
        // テストデータ作成
        TestSbUser user = TestSbUser.builder()
                .name("新規ユーザー")
                .isActive(true)
                .sequenceNo(1)
                .amount(1000.0)
                .build();

        // テストの実行
        TestSbUser inserted = jdbcManager.insert(user);

        // 検証
        assertNotNull(inserted.getId());
        assertEquals(user.getName(), inserted.getName());
    }

    @Test
    void testUpdate() {
        // テストデータ作成
        TestSbUser user = jdbcManager.findByPk(TestSbUser.class, 1L);
        user.setName("更新後の名前");

        // テストの実行
        TestSbUser updated = jdbcManager.updateByPk(user);

        // 検証
        assertEquals("更新後の名前", updated.getName());
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