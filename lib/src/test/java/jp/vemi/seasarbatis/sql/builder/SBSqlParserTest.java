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

/**
 * SQLパーサーの動作を検証する単体テストクラスです。
 * <p>
 * English: Provides regression tests that document the supported S2JDBC-style directives
 * handled by {@link SBSqlParser}, covering nested IF/BEGIN blocks, default literal fallbacks,
 * complex boolean expressions, and safe placeholder substitution rules.
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0-beta.2
 * @since 2025/01/01
 */
class SBSqlParserTest {

    @Test
    @Tag("smoke")
    /**
     * SQLファイルの読み込みが成功することを検証します。
     * <p>
     * English: Verifies that S2JDBC-style SQL resources can be loaded from the classpath.
     * </p>
     *
     * @throws IOException リソース読み込みに失敗した場合
     */
    void testFileLoading() throws IOException {
        String sql = SBSqlFileLoader.load("test-query.sql");
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
        assertTrue(sql.contains("FROM sbtest_users"));
    }

    @Test
    /**
     * 数値条件のダイナミック置換が行われることを検証します。
     * <p>
     * English: Confirms numeric predicates become MyBatis placeholders when parameters exist.
     * </p>
     */
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
    /**
     * ENUM や SET 相当の条件が正しく置換されることを検証します。
     * <p>
     * English: Ensures scalar expressions like FIND_IN_SET resolve to parameter bindings safely.
     * </p>
     */
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
    /**
     * 日付や日時条件の変換が行われることを検証します。
     * <p>
     * English: Validates temporal predicates render as #{param} placeholders across types.
     * </p>
     */
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
    /**
     * BEGIN/END ブロックと単純条件の評価結果を検証します。
     * <p>
     * English: Confirms nested BEGIN/IF directives emit clauses only when parameters exist.
     * </p>
     */
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
    /**
     * null 判定条件の展開を検証します。
     * <p>
     * English: Ensures equality checks against null translate to IS NULL / IS NOT NULL clauses.
     * </p>
     */
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
    /**
     * 存在しない SQL ファイル読み込み時に例外が発生することを検証します。
     * <p>
     * English: Guards against silent failures when resources are missing.
     * </p>
     */
    void testInvalidSQLFile() {
        assertThrows(IOException.class, () -> {
            SBSqlFileLoader.load("non-existent.sql");
        });
    }

    @Test
    /**
     * 拡張的なバインド置換ロジックを検証します。
     * <p>
     * English: Covers collection IN clauses, LIKE patterns, and null sentinels.
     * </p>
     */
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
        assertTrue(result.contains("id IN (#{ids_0}, #{ids_1}, #{ids_2})"));
        assertTrue(result.contains("name LIKE #{namePattern}"));
        assertTrue(result.contains("amount > #{minAmount}"));
        assertTrue(result.contains("status IS #{nullableStatus}"));
        assertFalse(result.contains("/*"));
        assertFalse(result.contains("*/"));

        Map<String, Object> expanded = parsedSql.getParameterValues();
        assertEquals(3, expanded.entrySet().stream().filter(e -> e.getKey().startsWith("ids_")).count());
        assertEquals(1, expanded.get("ids_0"));
        assertEquals(2, expanded.get("ids_1"));
        assertEquals(3, expanded.get("ids_2"));
        assertTrue(expanded.containsKey("namePattern"));
        assertTrue(expanded.containsKey("minAmount"));
        assertTrue(expanded.containsKey("nullableStatus"));
    }

    @Test
    /**
     * ネストした IF ブロックの評価を検証します。
     * <p>
     * English: Demonstrates nested IF directives that selectively contribute predicates.
     * </p>
     */
    void testNestedIfBlocks() {
    Map<String, Object> params = new HashMap<>();
    params.put("status", "ACTIVE");
    params.put("role", null);
    params.put("vip", Boolean.TRUE);

        String sql = """
                SELECT * FROM users
                /*BEGIN*/
                WHERE 1=1
                /*IF status != null*/
                AND status = /*status*/'ACTIVE'
                    /*IF role != null*/
                    AND role = /*role*/'ADMIN'
                    /*END*/
                    /*IF vip == true*/
                    AND vip_flag = /*vip*/0
                    /*END*/
                /*END*/
                /*END*/
                ORDER BY id
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();

        assertTrue(result.contains("status = #{status}"), "status 条件が展開されること");
        assertTrue(result.contains("vip_flag = #{vip}"), "ネストした IF が展開されること");
        assertFalse(result.contains("role ="), "role 条件は展開されないこと");
        assertTrue(result.contains("ORDER BY id"));
    }

    @Test
    /**
     * パラメータ未指定時にダミー値へフォールバックする挙動を検証します。
     * <p>
     * English: Ensures default literals remain when a parameter is absent.
     * </p>
     */
    void testPlaceholderFallbackToDefault() {
        Map<String, Object> params = Map.of();

        String sql = "SELECT * FROM users WHERE type = /*type*/'USER' AND deleted = /*deleted*/0";

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();

        assertTrue(result.contains("type = 'USER'"), "パラメータ未指定でもデフォルト値が残ること");
        assertTrue(result.contains("deleted = 0"));
        assertFalse(result.contains("#{type}"));
    }

    @Test
    /**
     * 複合的な条件式の評価を検証します。
     * <p>
     * English: Validates support for parentheses and logical operator precedence.
     * </p>
     */
    void testComplexConditionEvaluation() {
        Map<String, Object> params = new HashMap<>();
        params.put("score", 85);
        params.put("rank", "A");

        String sql = """
                SELECT * FROM users
                /*IF (score >= 80 AND rank == 'A') OR (score >= 90 AND rank == 'B')*/
                WHERE score >= /*score*/0
                /*END*/
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = parsedSql.getSql();

        assertTrue(result.contains("WHERE score >= #{score}"));
    }

    @Test
    /**
     * 空の BEGIN ブロックが削除されることを検証します。
     * <p>
     * English: Confirms scaffolding clauses like WHERE 1=1 vanish when no dynamic child matches.
     * </p>
     */
    void testBeginBlockRemovedWhenEmpty() {
        Map<String, Object> params = Map.of();

        String sql = """
                SELECT * FROM users
                /*BEGIN*/
                WHERE 1=1
                /*IF id != null*/
                AND id = /*id*/1
                /*END*/
                /*END*/
                ORDER BY id
                """;

        ParsedSql parsedSql = SBSqlParser.parse(sql, params);
        String result = normalizeWhitespace(parsedSql.getSql());

        assertEquals("SELECT * FROM users ORDER BY id", result);
    }

    @Test
    /**
     * ダミー値パターンの置換整合性を検証します。
     * <p>
     * English: Checks various literal formats around placeholders remain intact post-parse.
     * </p>
     */
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
    /**
     * SQL インジェクション対策としての安全な置換を検証します。
     * <p>
     * English: Ensures malicious payloads are never concatenated as raw SQL fragments.
     * </p>
     */
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