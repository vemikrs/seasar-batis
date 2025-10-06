/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL(Testcontainers) 上で SQL ファイルを検証する統合テストです。
 * <p>
 * Docker 環境が有効な場合に sbtest_users テーブルの初期化とクエリ検証を実施します。
 * </p>
 *
 * @version 1.0.0-beta.2
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationPostgresTest {

    private static PostgreSQLContainer<?> postgresContainer;
    private static DataSource postgresDataSource;
    private static SBJdbcManager postgresManager;

    /**
     * Docker 利用可否を判定し、PostgreSQL コンテナを初期化します。
     *
     * @throws Exception 初期化処理中に致命的なエラーが発生した場合
     */
    @BeforeAll
    static void setUpDatabase() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker が利用できないため PostgreSQL 統合テストをスキップします。");

        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
                .withDatabaseName("sbtest")
                .withUsername("test")
                .withPassword("test");
        try {
            container.start();
            postgresContainer = container;
            postgresDataSource = createPostgresDataSource();
            initializePostgresData(postgresDataSource);
            postgresManager = new SBJdbcManager(postgresDataSource);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false,
                    "PostgreSQL コンテナを起動できないためテストをスキップします: " + ex.getMessage());
        }
    }

    /**
     * PostgreSQL 上で SQL ファイルを実行し、期待結果が返ることを確認します。
     */
    @Test
    void testSelectBySqlFileOnPostgres() {
        Assumptions.assumeTrue(postgresManager != null, "PostgreSQL マネージャーが初期化されていないためスキップします。");
        Map<String, Object> params = buildParameterMap();
        List<?> rows = postgresManager.selectBySqlFile("sql/complex-users-query.sql", params, Map.class).getResultList();
        assertEquals(2, rows.size());
        assertFalse(rows.isEmpty());
        assertTrue(rows.get(0) instanceof Map);
    }

    /**
     * PostgreSQL コンテナを停止します。
     */
    @AfterAll
    static void tearDown() {
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
    }

    private static DataSource createPostgresDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(postgresContainer.getJdbcUrl());
        dataSource.setUser(postgresContainer.getUsername());
        dataSource.setPassword(postgresContainer.getPassword());
        return dataSource;
    }

    private static void initializePostgresData(DataSource dataSource) throws java.sql.SQLException {
        try (java.sql.Connection connection = dataSource.getConnection();
                java.sql.Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS sbtest_users");
            statement.execute("CREATE TABLE sbtest_users (" +
                    "id SERIAL PRIMARY KEY," +
                    "sequence_no INT," +
                    "amount NUMERIC(10,2)," +
                    "rate REAL," +
                    "score DOUBLE PRECISION," +
                    "is_active BOOLEAN," +
                    "name VARCHAR(100)," +
                    "description TEXT," +
                    "memo TEXT," +
                    "char_code CHAR(3)," +
                    "created_at TIMESTAMP," +
                    "updated_at TIMESTAMP," +
                    "birth_date DATE," +
                    "work_time TIME," +
                    "status VARCHAR(20)," +
                    "user_type VARCHAR(50)," +
                    "preferences JSONB)");
            statement.execute("CREATE INDEX idx_sbtest_users_name ON sbtest_users (name)");
            statement.execute("CREATE INDEX idx_sbtest_users_status ON sbtest_users (status)");

            executeInsert(statement,
                    "INSERT INTO sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) " +
                            "VALUES (1, 2000.00, 0.05, 85.0, TRUE, 'テストユーザー1', '一般ユーザー1の説明', 'メモ1', 'JP1', NOW(), NOW(), CURRENT_DATE, '09:00:00', 'ACTIVE', 'USER', '{\"lang\": \"ja\", \"theme\": \"dark\"}'::jsonb)");
            executeInsert(statement,
                    "INSERT INTO sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) " +
                            "VALUES (2, 1500.00, 0.03, 70.0, FALSE, 'テストユーザー2', '管理者ユーザーの説明', 'メモ2', 'JP2', NOW(), NOW(), CURRENT_DATE, '10:00:00', 'INACTIVE', 'ADMIN,USER', '{\"lang\": \"en\", \"theme\": \"light\"}'::jsonb)");
            executeInsert(statement,
                    "INSERT INTO sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) " +
                            "VALUES (3, 3000.00, 0.04, 90.0, TRUE, 'テストユーザー3', '複雑クエリ用のユーザー', 'メモ3', 'JP3', NOW(), NOW(), CURRENT_DATE, '11:00:00', 'ACTIVE', 'VIP', '{\"lang\": \"ja\", \"theme\": \"system\"}'::jsonb)");
        }
    }

    private static void executeInsert(java.sql.Statement statement, String sql) throws java.sql.SQLException {
        statement.execute(sql);
    }

    private static Map<String, Object> buildParameterMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", List.of("ACTIVE", "VIP"));
        params.put("keyword", "%テスト%");
        params.put("minScore", 80.0);
        params.put("includeInactive", Boolean.FALSE);
        return params;
    }
}
