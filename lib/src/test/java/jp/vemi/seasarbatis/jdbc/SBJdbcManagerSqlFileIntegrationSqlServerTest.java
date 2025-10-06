/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * SQL Server(Testcontainers) 上で SQL ファイルの互換性を検証する統合テストです。
 * <p>
 * sbtest_users テーブルを T-SQL で初期化し、MyBatis 経由の実行結果を確認します。
 * </p>
 *
 * @version 0.0.1
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationSqlServerTest {

    private static MSSQLServerContainer<?> sqlServerContainer;
    private static DataSource sqlServerDataSource;
    private static SBJdbcManager sqlServerManager;

    /**
     * Docker 利用可否を判定し、SQL Server コンテナを初期化します。
     *
     * @throws Exception 初期化中に致命的なエラーが発生した場合
     */
    @BeforeAll
    static void setUpDatabase() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker が利用できないため SQL Server 統合テストをスキップします。");

        MSSQLServerContainer<?> container = new MSSQLServerContainer<>(
                DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
                        .acceptLicense()
                        .withPassword("Password123!");
        try {
            container.start();
            sqlServerContainer = container;
            sqlServerDataSource = createSqlServerDataSource();
            initializeSqlServerData(sqlServerDataSource);
            sqlServerManager = new SBJdbcManager(sqlServerDataSource);
        } catch (Exception ex) {
            Assumptions.assumeTrue(false,
                    "SQL Server コンテナを起動できないためテストをスキップします: " + ex.getMessage());
        }
    }

    /**
     * SQL Server 上で SQL ファイルを実行し、期待結果を検証します。
     */
    @Test
    void testSelectBySqlFileOnSqlServer() {
        Assumptions.assumeTrue(sqlServerManager != null, "SQL Server マネージャーが初期化されていないためスキップします。");
        Map<String, Object> params = buildParameterMap();
        List<?> rows = sqlServerManager.selectBySqlFile("sql/complex-users-query.sql", params, Map.class).getResultList();
        assertEquals(2, rows.size());
        assertFalse(rows.isEmpty());
        assertTrue(rows.get(0) instanceof Map);
    }

    /**
     * SQL Server コンテナを停止します。
     */
    @AfterAll
    static void tearDown() {
        if (sqlServerContainer != null) {
            sqlServerContainer.stop();
        }
    }

    private static DataSource createSqlServerDataSource() {
        com.microsoft.sqlserver.jdbc.SQLServerDataSource dataSource = new com.microsoft.sqlserver.jdbc.SQLServerDataSource();
        dataSource.setURL(sqlServerContainer.getJdbcUrl());
        dataSource.setUser(sqlServerContainer.getUsername());
        dataSource.setPassword(sqlServerContainer.getPassword());
        dataSource.setTrustServerCertificate(true);
        return dataSource;
    }

    private static void initializeSqlServerData(DataSource dataSource) throws SQLException {
        try (java.sql.Connection connection = dataSource.getConnection();
                java.sql.Statement statement = connection.createStatement()) {
            statement.execute("IF OBJECT_ID('dbo.sbtest_users', 'U') IS NOT NULL DROP TABLE dbo.sbtest_users;");
            statement.execute("CREATE TABLE dbo.sbtest_users (" +
                    "id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "sequence_no INT," +
                    "amount DECIMAL(10,2)," +
                    "rate FLOAT," +
                    "score FLOAT," +
                    "is_active BIT," +
                    "name NVARCHAR(100)," +
                    "description NVARCHAR(MAX)," +
                    "memo NVARCHAR(MAX)," +
                    "char_code CHAR(3)," +
                    "created_at DATETIME2," +
                    "updated_at DATETIME2," +
                    "birth_date DATE," +
                    "work_time TIME," +
                    "status NVARCHAR(20)," +
                    "user_type NVARCHAR(50)," +
                    "preferences NVARCHAR(1000))");
            statement.execute("CREATE INDEX idx_sbtest_users_name ON dbo.sbtest_users (name)");
            statement.execute("CREATE INDEX idx_sbtest_users_status ON dbo.sbtest_users (status)");

            statement.execute("INSERT INTO dbo.sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) "
                    + "VALUES (1, 2000.00, 0.05, 85.0, 1, N'テストユーザー1', N'一般ユーザー1の説明', N'メモ1', 'JP1', SYSDATETIME(), SYSDATETIME(), CAST(GETDATE() AS DATE), CAST('09:00:00' AS TIME), 'ACTIVE', 'USER', N'{\"lang\": \"ja\", \"theme\": \"dark\"}')");
            statement.execute("INSERT INTO dbo.sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) "
                    + "VALUES (2, 1500.00, 0.03, 70.0, 0, N'テストユーザー2', N'管理者ユーザーの説明', N'メモ2', 'JP2', SYSDATETIME(), SYSDATETIME(), CAST(GETDATE() AS DATE), CAST('10:00:00' AS TIME), 'INACTIVE', 'ADMIN,USER', N'{\"lang\": \"en\", \"theme\": \"light\"}')");
            statement.execute("INSERT INTO dbo.sbtest_users (sequence_no, amount, rate, score, is_active, name, description, memo, char_code, created_at, updated_at, birth_date, work_time, status, user_type, preferences) "
                    + "VALUES (3, 3000.00, 0.04, 90.0, 1, N'テストユーザー3', N'複雑クエリ用のユーザー', N'メモ3', 'JP3', SYSDATETIME(), SYSDATETIME(), CAST(GETDATE() AS DATE), CAST('11:00:00' AS TIME), 'ACTIVE', 'VIP', N'{\"lang\": \"ja\", \"theme\": \"system\"}')");
        }
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
