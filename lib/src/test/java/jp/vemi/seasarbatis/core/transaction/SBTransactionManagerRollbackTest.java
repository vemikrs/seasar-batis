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

import jp.vemi.seasarbatis.exception.SBTransactionException;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Reader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/**
 * 独立トランザクションのロールバック動作を単独でテストするクラス。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
class SBTransactionManagerRollbackTest {

    private SqlSessionFactory sqlSessionFactory;
    private SBTransactionManager txManager;
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        // H2インメモリデータベースのセットアップ（新しいインスタンス）
        String dbName = "rollbacktest_" + System.currentTimeMillis();
        dataSource = new UnpooledDataSource(
            "org.h2.Driver",
            "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );
        
        // テストテーブルの作成
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS rollback_test");
            stmt.execute("""
                CREATE TABLE rollback_test (
                    id BIGINT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL
                )
            """);
        }
        
        // MyBatis設定
        try (Reader reader = Resources.getResourceAsReader("mybatis-test-config.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
            sqlSessionFactory.getConfiguration().setEnvironment(
                new Environment("rollbacktest", new JdbcTransactionFactory(), dataSource)
            );
        }
        
        txManager = new SBTransactionManager(sqlSessionFactory);
    }

    @AfterEach
    void tearDown() throws Exception {
        // クリーンアップ
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS rollback_test");
            }
        }
    }

    @Test
    void testIndependentTransactionRollback() {
        // 独立トランザクションでエラーが発生した場合のロールバックテスト
        
        // エラーが発生する独立トランザクションを実行
        assertThrows(SBTransactionException.class, () -> {
            txManager.executeWithTransaction(true, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    // データベースにレコードを挿入
                    try (SqlSession session = sqlSessionFactory.openSession()) {
                        session.update("insertRollbackTest", new RollbackTestData(1L, "ロールバックテスト"));
                        session.commit();
                    }
                    
                    // 意図的に例外を発生させる
                    throw new RuntimeException("テストエラー");
                }
            });
        });
        
        // データがロールバックされていることを確認
        try (SqlSession session = sqlSessionFactory.openSession()) {
            RollbackTestData data = session.selectOne("selectRollbackTestById", 1L);
            assertNull(data, "データがロールバックされていること");
        }
    }

    // テスト用のデータクラス
    public static class RollbackTestData {
        private Long id;
        private String name;

        public RollbackTestData() {}

        public RollbackTestData(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        // Getter/Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}