/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.sql.builder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;
import jp.vemi.seasarbatis.core.sql.processor.SBSqlParser;

/**
 * SQL パーサーの性能を簡易的に検証するテストクラスです。
 * <p>
 * <strong>English:</strong> Provides a lightweight guardrail ensuring the refactored parser keeps
 * throughput within acceptable bounds for release gating purposes.
 * </p>
 */
class SBSqlParserPerformanceTest {

    @Test
    @Tag("performance")
    /**
     * 複雑な SQL を短時間で繰り返し解析できることを検証します。
     * <p>
     * English: Parses the compatibility SQL multiple times and fails if it exceeds the 2 second budget.
     * </p>
     */
    void testParserThroughputUnderBudget() throws IOException {
        String sql = SBSqlFileLoader.load("sql/complex-users-query.sql");
        Map<String, Object> params = new HashMap<>();
        params.put("statuses", Arrays.asList("ACTIVE", "VIP", "DELETED"));
        params.put("keyword", "%テスト%");
        params.put("minScore", 75.0);
        params.put("includeInactive", Boolean.FALSE);

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            for (int i = 0; i < 500; i++) {
                ParsedSql parsedSql = SBSqlParser.parse(sql, params);
                assertNotNull(parsedSql.getSql());
            }
        });
    }
}
