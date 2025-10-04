package jp.vemi.seasarbatis.sql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;
import jp.vemi.seasarbatis.core.sql.processor.SBSqlParser;

class SBSqlParserTest {

    @Test
    @Tag("smoke")
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
        System.out.println(result);
        assertTrue(result.contains("amount > #{amount}"));
        assertTrue(result.contains("score >= #{score}"));
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
        System.out.println(result);
        assertTrue(result.contains("status = #{status}"));
        assertTrue(result.contains("FIND_IN_SET(#{user_type}") && result.contains(", user_type)"));
    }

    @Test
    void testDateTimeConditions() {
        Map<String, Object> params = new HashMap<>();
        params.put("birthDate", Date.valueOf("1990-01-01"));
        params.put("createdAt", "2023-01-01 10:00:00");

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF birthDate != null*/
                AND birth_date >= /*birthDate*/'1990-01-01'
                /*END*/
                /*IF createdAt != null*/
                AND created_at >= /*createdAt*/'2023-01-01 10:00:00'
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        System.out.println(result);
        assertTrue(result.contains("birth_date >= #{birthDate}"));
        assertTrue(result.contains("created_at >= #{createdAt}"));
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));
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
        assertTrue(result.contains("id = #{id}"));
        assertTrue(result.contains("name = #{name}"));
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
        params.put("namePattern", "test");
        params.put("minAmount", -1000.00);
        params.put("nullableStatus", null);

        String sql = """
                SELECT * FROM sbtest_users
                /*BEGIN*/
                WHERE 1=1
                /*IF ids != null*/
                AND id IN /*ids*/(1,2,3)
                /*END*/
                /*IF namePattern != null*/
                AND name LIKE /*namePattern*/'%test%'
                /*END*/
                /*IF minAmount != null*/
                AND amount > /*minAmount*/-500.00
                /*END*/
                /*IF nullableStatus == null*/
                AND status IS /*nullableStatus*/null
                /*END*/
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();
        System.out.println(result);
        assertTrue(result.contains("id IN #{ids}"));
        assertTrue(result.contains("name LIKE #{namePattern}"));
        assertTrue(result.contains("amount > #{minAmount}"));
        assertTrue(result.contains("status IS #{nullableStatus}"));
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));
    }

    @Test
    void testDummyValuePatterns() {
        Map<String, Object> params = Map.of("id", 1, "name", "test user", "createAt", "2025-01-01 10:00:00");

        // ダミー値なしのパターン
        assertEquals(normalizeWhitespace("SELECT * FROM users WHERE id = #{id} AND name = #{name}"),
                normalizeWhitespace(SBSqlParser
                        .parse("SELECT * FROM users WHERE id = /*id*/ AND name = /*name*/'test'", params).getSql()));

        // 日時など空白を含むダミー値のパターン
        assertEquals(normalizeWhitespace("SELECT * FROM users WHERE created_at >= #{createAt}"),
                normalizeWhitespace(SBSqlParser
                        .parse("SELECT * FROM users" + " WHERE created_at >= /*createAt*/'2025-01-01 10:00:00'", params)
                        .getSql()));

        // 複数行のSQLのパターン
        assertEquals(
                normalizeWhitespace(
                        "SELECT * FROM users WHERE id = #{id} AND name LIKE #{name} AND created_at >= #{createAt}"),
                normalizeWhitespace(SBSqlParser.parse("SELECT *\n" + "  FROM users\n" + " WHERE id = /*id*/1\n"
                        + "   AND name LIKE /*name*/'%test%'\n"
                        + "   AND created_at >= /*createAt*/'2025-01-01 10:00:00'", params).getSql()));
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

        // 全てのパラメータがMyBatisのバインド変数形式に置換されていることを確認
        assertTrue(result.contains("id = #{malicious_id}"), "malicious_idが正しく置換されていること");
        assertTrue(result.contains("name = #{union_attack}"), "union_attackが正しく置換されていること");
        assertTrue(result.contains("type = #{comment_attack}"), "comment_attackが正しく置換されていること");
        assertTrue(result.contains("status = #{quote_attack}"), "quote_attackが正しく置換されていること");
    }

    /**
     * SQL文の空白文字を正規化します。
     * 
     * <p>
     * 以下の正規化を行います：
     * <ul>
     * <li>改行を空白に変換</li>
     * <li>連続する空白を1つの空白に置換</li>
     * <li>文字列の前後の空白を削除</li>
     * </ul>
     * </p>
     *
     * @param sql 正規化対象のSQL文
     * @return 正規化されたSQL文
     */
    private String normalizeWhitespace(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}