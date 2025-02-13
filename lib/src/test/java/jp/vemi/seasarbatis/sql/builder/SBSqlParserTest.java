package jp.vemi.seasarbatis.sql.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.sql.ParsedSql;
import jp.vemi.seasarbatis.sql.SBSqlFileLoader;
import jp.vemi.seasarbatis.sql.SBSqlParser;

class SBSqlParserTest {

    @Test
    void testFileLoading() throws IOException {
        String sql = SBSqlFileLoader.load("test-query.sql");
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM sbtest_users"));
    }

    @Test
    void testNumericConditions() {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", 10000.00);
        params.put("score", 85.5);

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF amount != null*/
                AND amount > /*amount*/5000.00
                /*END*/
                /*IF score != null*/
                AND score >= /*score*/80.0
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        assertTrue(result.contains("amount > ?"));
        assertTrue(result.contains("score >= ?"));
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));
    }

    @Test
    void testEnumAndSetConditions() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "ACTIVE");
        params.put("user_type", "ADMIN");

        String sql = """
                SELECT * FROM sbtest_users
                WHERE status = /*status*/'ACTIVE'
                AND FIND_IN_SET(/*user_type*/'ADMIN', user_type)
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        assertTrue(result.contains("status = ?"));
        assertTrue(result.contains("FIND_IN_SET(?, user_type)"));
    }

    @Test
    void testDateTimeConditions() {
        Map<String, Object> params = new HashMap<>();
        params.put("birth_date", Date.valueOf("1990-01-01"));
        params.put("created_at", "2023-01-01 10:00:00");

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF birth_date != null*/
                AND birth_date >= /*birth_date*/'1990-01-01'
                /*END*/
                /*IF created_at != null*/
                AND created_at >= /*created_at*/'2023-01-01 10:00:00'
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        String expectedSql = result.replaceAll("\\s+", " ").trim();

        assertTrue(expectedSql.contains("birth_date >= ?"));
        assertTrue(expectedSql.contains("created_at >= ?"));
        assertFalse(expectedSql.contains("/*"));
        assertFalse(expectedSql.contains("*/"));
    }

    @Test
    void testConditionsWithBeginEnd() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1001);
        params.put("name", "テストユーザー");

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF id != null*/
                AND id = /*id*/1
                /*END*/
                /*IF name != null*/
                AND name = /*name*/'test'
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        assertTrue(result.contains("id = ?"));
        assertTrue(result.contains("name = ?"));
        assertFalse(result.contains("/*BEGIN*/"));
        assertFalse(result.contains("/*END*/"));
    }

    @Test
    void testNullConditions() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", null);
        params.put("description", "テスト説明");

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF name == null*/
                AND name IS NULL
                /*END*/
                /*IF description != null*/
                AND description IS NOT NULL
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        assertTrue(result.contains("name IS NULL"));
        assertTrue(result.contains("description IS NOT NULL"));
    }

    @Test
    void testInvalidSQLFile() {
        assertThrows(IOException.class, () -> {
            SBSqlFileLoader.load("non-existent.sql");
        });
    }

    @Test
    void testAdvancedBindings() {
        Map<String, Object> params = new HashMap<>();
        params.put("ids", Arrays.asList(1, 2, 3));
        params.put("name_pattern", "test");
        params.put("min_amount", -1000.00);
        params.put("nullable_status", null);

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF ids != null*/
                AND id IN /*ids*/(1,2,3)
                /*END*/
                /*IF name_pattern != null*/
                AND name LIKE /*name_pattern*/'%test%'
                /*END*/
                /*IF min_amount != null*/
                AND amount > /*min_amount*/-500.00
                /*END*/
                /*IF nullable_status == null*/
                AND status IS /*nullable_status*/null
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        assertTrue(result.contains("id IN ?"));
        assertTrue(result.contains("name LIKE ?"));
        assertTrue(result.contains("amount > ?"));
        assertTrue(result.contains("status IS ?"));
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));
    }

    @Test
    void testSQLInjectionPrevention() {
        Map<String, Object> params = new HashMap<>();
        // SQLインジェクションを試みるパラメータ
        params.put("malicious_id", "1; DROP TABLE sbtest_users;");
        params.put("union_attack", "1' UNION SELECT * FROM users--");
        params.put("comment_attack", "*/; DROP TABLE sbtest_users; /*");
        params.put("quote_attack", "' OR '1'='1");

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF malicious_id != null*/
                AND id = /*malicious_id*/1
                /*END*/
                /*IF union_attack != null*/
                AND name = /*union_attack*/'test'
                /*END*/
                /*IF comment_attack != null*/
                AND type = /*comment_attack*/'USER'
                /*END*/
                /*IF quote_attack != null*/
                AND status = /*quote_attack*/'ACTIVE'
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();

        // 危険な文字列が含まれていないことを確認
        assertFalse(result.contains(";"));
        assertFalse(result.contains("--"));
        assertFalse(result.contains("DROP"));
        assertFalse(result.contains("UNION"));

        // 全てのパラメータが?に置換されていることを確認
        assertTrue(result.matches(".*id = \\?.*"));
        assertTrue(result.matches(".*name = \\?.*"));
        assertTrue(result.matches(".*type = \\?.*"));
        assertTrue(result.matches(".*status = \\?.*"));
    }
}