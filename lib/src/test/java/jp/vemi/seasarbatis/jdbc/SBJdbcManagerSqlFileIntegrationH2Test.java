/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * H2 データベース上で SQL ファイルを利用した統合テストを実施します。
 * <p>
 * SeasarBatis が動的 SQL を生成し、H2 上で期待どおりの結果を返すことを確認します。
 * </p>
 *
 * @version 0.0.1
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationH2Test {

    private static DataSource h2DataSource;
    private static SBJdbcManager h2Manager;

    /**
     * H2 の組み立てと初期データ投入を行います。
     *
     * @throws Exception H2 初期化に失敗した場合
     */
    @BeforeAll
    static void setUpDatabase() throws Exception {
        h2DataSource = createH2DataSource();
        runScripts(h2DataSource, "ddl/01_create_h2_schema.sql", "ddl/02_insert_h2_data.sql");
        h2Manager = new SBJdbcManager(h2DataSource);
    }

    /**
     * H2 上で SQL ファイルを実行し、結果件数とデータ整合性を検証します。
     */
    @Test
    void testSelectBySqlFileOnH2() {
        Map<String, Object> params = buildParameterMap();
        List<?> rows = h2Manager.selectBySqlFile("sql/complex-users-query.sql", params, Map.class).getResultList();
        assertEquals(2, rows.size());
        assertFalse(rows.isEmpty());
        assertTrue(rows.get(0) instanceof Map);
    }

    private static Map<String, Object> buildParameterMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", List.of("ACTIVE", "VIP"));
        params.put("keyword", "%テスト%");
        params.put("minScore", 80.0);
        params.put("includeInactive", Boolean.FALSE);
        return params;
    }

    private static JdbcDataSource createH2DataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sbtest;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void runScripts(DataSource dataSource, String... resources) throws IOException, SQLException {
        for (String resource : resources) {
            String sql = loadResource(resource);
            executeStatements(dataSource, sql);
        }
    }

    private static String loadResource(String resource) throws IOException {
        ClassLoader loader = SBJdbcManagerSqlFileIntegrationH2Test.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("リソースが見つかりません: " + resource);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private static void executeStatements(DataSource dataSource, String sql) throws SQLException {
        String[] statements = sql.split(";");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String raw : statements) {
                String trimmed = raw.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.execute(trimmed);
            }
        }
    }
}
