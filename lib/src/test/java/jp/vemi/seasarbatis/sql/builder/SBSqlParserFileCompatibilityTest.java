/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.sql.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.core.sql.ProcessedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;
import jp.vemi.seasarbatis.core.sql.processor.SBSqlParser;
import jp.vemi.seasarbatis.core.sql.processor.SBSqlProcessor;

/**
 * SQL ファイル互換性検証用のテストクラスです。
 * <p>
 * <strong>English:</strong> Validates that complex S2JDBC-style SQL files are parsed and processed
 * consistently, covering nested IF/BEGIN、コレクション IN 展開、および LIKE 条件を含むシナリオ。
 * </p>
 */
class SBSqlParserFileCompatibilityTest {

    @Test
    /**
     * 複雑な SQL ファイルを読み込み、解析結果と最終生成 SQL を検証します。
     * <p>
     * English: Ensures the compatibility SQL sample renders expected placeholders and concrete SQL output.
     * </p>
     */
    void testComplexSqlFile() throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", Arrays.asList("ACTIVE", "VIP"));
        params.put("keyword", "%テスト%");
        params.put("minScore", 80.0);
        params.put("includeInactive", Boolean.FALSE);

        String sql = SBSqlFileLoader.load("sql/complex-users-query.sql");
        ParsedSql parsedSql = SBSqlParser.parse(sql, params);

        String normalized = normalize(parsedSql.getSql());
        String expected = normalize("""
                SELECT id, name, status, score
                FROM sbtest_users
                WHERE 1=1
                AND status IN (#{statuses_0}, #{statuses_1})
                AND ( name LIKE #{keyword} OR description LIKE #{keyword} )
                AND score >= #{minScore}
                AND status != 'INACTIVE'
                ORDER BY id
                """);
        assertEquals(expected, normalized);

        Map<String, Object> captured = parsedSql.getParameterValues();
        assertNotNull(captured);
        assertEquals(List.of("ACTIVE", "VIP"),
                List.of(captured.get("statuses_0"), captured.get("statuses_1")));
        assertTrue(captured.containsKey("keyword"));

        SBSqlProcessor processor = new SBSqlProcessor(new Configuration());
        ProcessedSql processed = processor.process(sql, params);
        String finalSql = normalize(processed.getSql());
        assertTrue(finalSql.contains("status IN ('ACTIVE', 'VIP')"));
        assertTrue(finalSql.contains("name LIKE '%テスト%'"));
        assertFalse(finalSql.contains("#{"));
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
