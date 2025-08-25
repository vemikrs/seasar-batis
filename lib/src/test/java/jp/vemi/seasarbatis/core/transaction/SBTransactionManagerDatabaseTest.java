/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.transaction;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import jp.vemi.seasarbatis.jdbc.SBJdbcManager;
import jp.vemi.seasarbatis.exception.SBTransactionException;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/**
 * SBTransactionManagerの包括的なデータベーステストクラス。
 * <p>
 * H2データベースを使用して、実際のデータベース操作での
 * トランザクション管理の動作を検証します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SBTransactionManagerDatabaseTest {

    private SqlSessionFactory sqlSessionFactory;
    private SBTransactionManager txManager;
    private SBJdbcManager jdbcManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        // H2インメモリデータベースのセットアップ
        dataSource = new UnpooledDataSource(
            "org.h2.Driver",
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );
        
        // テストテーブルの作成
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS test_users");
            stmt.execute("""
                CREATE TABLE test_users (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    active BOOLEAN DEFAULT TRUE
                )
            """);
        }
        
        // MyBatis設定
        try (Reader reader = Resources.getResourceAsReader("mybatis-test-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            sqlSessionFactory.getConfiguration().setEnvironment(
                new Environment("test", new JdbcTransactionFactory(), dataSource)
            );
        }
        
        txManager = new SBTransactionManager(sqlSessionFactory);
        jdbcManager = new SBJdbcManager(sqlSessionFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        // クリーンアップ
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS test_users");
            }
        }
    }

    @Test
    @Order(1)
    void testBasicTransactionCommit() {
        // 基本的なトランザクションコミットテスト
        final AtomicBoolean executed = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                assertTrue(txManager.isActive(), "トランザクションが有効であること");
                
                // データベースにレコードを挿入
                SqlSession session = txManager.getTransactionOperation().getCurrentSession();
                session.update("insertUser", new TestUser(1L, "テストユーザー1", "test1@example.com", true));
                
                executed.set(true);
                return "success";
            }
        });
        
        assertTrue(executed.get(), "トランザクションが実行されたこと");
        assertEquals("success", result, "正常な結果が返されること");
        assertFalse(txManager.isActive(), "トランザクションが終了していること");
        
        // データが正常にコミットされていることを確認
        verifyUserExists(1L, "テストユーザー1");
    }

    @Test
    @Order(2)
    void testTransactionRollback() {
        // トランザクションロールバックテスト
        final AtomicBoolean executed = new AtomicBoolean(false);
        
        assertThrows(SBTransactionException.class, () -> {
            txManager.executeWithTransaction(false, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    assertTrue(txManager.isActive(), "トランザクションが有効であること");
                    
                    // データベースにレコードを挿入
                    SqlSession session = txManager.getTransactionOperation().getCurrentSession();
                    session.update("insertUser", new TestUser(2L, "テストユーザー2", "test2@example.com", true));
                    
                    executed.set(true);
                    // 意図的に例外を発生させる
                    throw new RuntimeException("テストエラー");
                }
            });
        });
        
        assertTrue(executed.get(), "トランザクション内の処理が実行されたこと");
        assertFalse(txManager.isActive(), "トランザクションが終了していること");
        
        // データがロールバックされていることを確認
        verifyUserNotExists(2L);
    }

    @Test
    @Order(3)
    void testIndependentTransactionIsolation() {
        // 独立トランザクションの分離テスト
        final AtomicBoolean mainTxExecuted = new AtomicBoolean(false);
        final AtomicBoolean independentTxExecuted = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                assertTrue(txManager.isActive(), "メイントランザクションが有効であること");
                
                // メイントランザクションでデータを挿入
                SqlSession mainSession = txManager.getTransactionOperation().getCurrentSession();
                mainSession.update("insertUser", new TestUser(3L, "メインユーザー", "main@example.com", true));
                mainTxExecuted.set(true);
                
                // 独立トランザクションを実行
                String independentResult = txManager.executeWithTransaction(true, new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        // 独立トランザクション内でデータを挿入
                        SqlSession independentSession = txManager.getTransactionOperation().getCurrentSession();
                        independentSession.update("insertUser", new TestUser(4L, "独立ユーザー", "independent@example.com", true));
                        independentTxExecuted.set(true);
                        return "independent_success";
                    }
                });
                
                // メイントランザクションが依然として有効であることを確認
                assertTrue(txManager.isActive(), "メイントランザクションが依然として有効であること");
                assertEquals("independent_success", independentResult, "独立トランザクションが正常に完了すること");
                
                return "main_success";
            }
        });
        
        assertTrue(mainTxExecuted.get(), "メイントランザクションが実行されたこと");
        assertTrue(independentTxExecuted.get(), "独立トランザクションが実行されたこと");
        assertEquals("main_success", result, "メイントランザクションが正常に完了すること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
        
        // 両方のデータがコミットされていることを確認
        verifyUserExists(3L, "メインユーザー");
        verifyUserExists(4L, "独立ユーザー");
    }

    @Test
    @Order(4)
    void testIndependentTransactionErrorDoesNotAffectMain() {
        // 独立トランザクションのエラーがメイントランザクションに影響しないことのテスト
        final AtomicBoolean mainTxExecuted = new AtomicBoolean(false);
        final AtomicBoolean independentTxExecuted = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                assertTrue(txManager.isActive(), "メイントランザクションが有効であること");
                
                // メイントランザクションでデータを挿入
                SqlSession mainSession = txManager.getTransactionOperation().getCurrentSession();
                mainSession.update("insertUser", new TestUser(5L, "メインユーザー2", "main2@example.com", true));
                mainTxExecuted.set(true);
                
                // 独立トランザクションでエラーを発生させる
                try {
                    txManager.executeWithTransaction(true, new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            SqlSession independentSession = txManager.getTransactionOperation().getCurrentSession();
                            independentSession.update("insertUser", new TestUser(6L, "独立ユーザー2", "independent2@example.com", true));
                            independentTxExecuted.set(true);
                            throw new RuntimeException("独立トランザクションエラー");
                        }
                    });
                    fail("例外がスローされるべき");
                } catch (SBTransactionException e) {
                    // 期待される例外 - SBTransactionExceptionでラップされる
                    assertTrue(e.getCause() instanceof RuntimeException);
                    assertEquals("独立トランザクションエラー", e.getCause().getMessage());
                }
                
                // メイントランザクションが依然として有効であることを確認
                assertTrue(txManager.isActive(), "メイントランザクションが依然として有効であること");
                
                return "main_success_after_error";
            }
        });
        
        assertTrue(mainTxExecuted.get(), "メイントランザクションが実行されたこと");
        assertTrue(independentTxExecuted.get(), "独立トランザクションが実行されたこと");
        assertEquals("main_success_after_error", result, "メイントランザクションが正常に完了すること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
        
        // メイントランザクションのデータはコミットされ、独立トランザクションのデータはロールバックされていることを確認
        verifyUserExists(5L, "メインユーザー2");
        verifyUserNotExists(6L);
    }

    @Test
    @Order(5)
    void testMultipleIndependentTransactions() {
        // 複数の独立トランザクションのテスト
        final AtomicInteger executedCount = new AtomicInteger(0);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() throws Exception {
                assertTrue(txManager.isActive(), "メイントランザクションが有効であること");
                
                // メイントランザクションでデータを挿入
                SqlSession mainSession = txManager.getTransactionOperation().getCurrentSession();
                mainSession.update("insertUser", new TestUser(7L, "メインユーザー3", "main3@example.com", true));
                
                // 複数の独立トランザクションを実行
                for (int i = 0; i < 3; i++) {
                    final int index = i;
                    txManager.executeWithTransaction(true, new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            SqlSession independentSession = txManager.getTransactionOperation().getCurrentSession();
                            independentSession.update("insertUser", new TestUser(
                                8L + index, 
                                "独立ユーザー" + (index + 1), 
                                "independent" + (index + 1) + "@example.com", 
                                true
                            ));
                            executedCount.incrementAndGet();
                            return "independent_" + index;
                        }
                    });
                    
                    // 各独立トランザクション後もメイントランザクションが有効であることを確認
                    assertTrue(txManager.isActive(), "メイントランザクションが依然として有効であること");
                }
                
                return "main_with_multiple_independent";
            }
        });
        
        assertEquals(3, executedCount.get(), "3つの独立トランザクションが実行されたこと");
        assertEquals("main_with_multiple_independent", result, "メイントランザクションが正常に完了すること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
        
        // 全てのデータがコミットされていることを確認
        verifyUserExists(7L, "メインユーザー3");
        verifyUserExists(8L, "独立ユーザー1");
        verifyUserExists(9L, "独立ユーザー2");
        verifyUserExists(10L, "独立ユーザー3");
    }

    @Test
    @Order(6)
    void testJdbcManagerTransactionIntegration() {
        // SBJdbcManagerとの統合テスト
        final AtomicBoolean executed = new AtomicBoolean(false);
        
        assertDoesNotThrow(() -> {
            jdbcManager.transaction(manager -> {
                executed.set(true);
                // この部分では実際のentityクラスの代わりにSQLを直接実行
                // 実際のプロジェクトではentityクラスを使用
                assertTrue(manager.getTransactionManager().isActive(), "トランザクションが有効であること");
            });
        });
        
        assertTrue(executed.get(), "トランザクションコールバックが実行されたこと");
        assertFalse(jdbcManager.getTransactionManager().isActive(), "トランザクションが終了していること");
    }

    // ヘルパーメソッド
    private void verifyUserExists(Long id, String expectedName) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestUser user = session.selectOne("selectUserById", id);
            assertNotNull(user, "ユーザーが存在すること");
            assertEquals(expectedName, user.getName(), "ユーザー名が正しいこと");
        }
    }

    private void verifyUserNotExists(Long id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TestUser user = session.selectOne("selectUserById", id);
            assertNull(user, "ユーザーが存在しないこと");
        }
    }

    // テスト用のユーザークラス
    public static class TestUser {
        private Long id;
        private String name;
        private String email;
        private Boolean active;

        public TestUser() {}

        public TestUser(Long id, String name, String email, Boolean active) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.active = active;
        }

        // Getter/Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }
}