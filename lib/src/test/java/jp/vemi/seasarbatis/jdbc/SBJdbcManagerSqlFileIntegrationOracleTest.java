/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Oracle Database 向け統合テストのプレースホルダーです。
 * <p>
 * 将来的に Oracle 用データソースと初期化処理を追加するための枠を提供します。
 * </p>
 *
 * @version 1.0.0-beta.2
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationOracleTest {

    /**
     * Oracle 用統合テストが未実装であることを明示します。
     */
    @Test
    void testOracleNotImplementedYet() {
        Assumptions.assumeTrue(false, "Oracle 統合テストは現在未対応のためスキップします。");
    }
}
