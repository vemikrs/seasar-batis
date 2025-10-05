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
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.mysql.cj.jdbc.MysqlDataSource;

/**
 * MySQL(Testcontainers) 上で SQL ファイル経由のクエリを検証する統合テストです。
 * <p>
 * Docker が利用できる環境に限定して実データベースとの整合性を確認します。
 * </p>
 *
 * @version 1.0.0-beta.2
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationMySQLTest {

    private static MySQLContainer<?> mysqlContainer;
    private static SBJdbcManager mysqlManager;

    /**
     * Docker 利用可否を判定し、MySQL コンテナを初期化します。
     *
     * @throws Exception 初期化時に致命的なエラーが発生した場合
     */
    @BeforeAll
    static void setUpDatabase() throws Exception {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker が利用できないため MySQL 統合テストをスキップします。");

        @SuppressWarnings("resource")
        MySQLContainer<?> container = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.36"))
                .withDatabaseName("sbtest")
                .withUsername("test")
                .withPassword("test")
                .withInitScript("ddl/mysql_init.sql");
        try {
            container.start();
            mysqlContainer = container;
            mysqlManager = new SBJdbcManager(createMysqlDataSource());
        } catch (Exception ex) {
            Assumptions.assumeTrue(false, "MySQL コンテナを起動できないためテストをスキップします: " + ex.getMessage());
        }
    }

    /**
     * MySQL 上で SQL ファイルを実行し、H2 と同一結果が得られるかを検証します。
     */
    @Test
    void testSelectBySqlFileOnMySQL() {
        Assumptions.assumeTrue(mysqlManager != null, "MySQL マネージャーが初期化されていないためスキップします。");
        Map<String, Object> params = buildParameterMap();
        List<?> rows = mysqlManager.selectBySqlFile("sql/complex-users-query.sql", params, Map.class).getResultList();
        assertEquals(2, rows.size());
        assertFalse(rows.isEmpty());
        assertTrue(rows.get(0) instanceof Map);
    }

    /**
     * MySQL コンテナを停止します。
     */
    @AfterAll
    static void tearDown() {
        if (mysqlContainer != null) {
            mysqlContainer.stop();
        }
    }

    private static DataSource createMysqlDataSource() {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(mysqlContainer.getJdbcUrl());
        dataSource.setUser(mysqlContainer.getUsername());
        dataSource.setPassword(mysqlContainer.getPassword());
        return dataSource;
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
