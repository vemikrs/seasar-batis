/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * SQL Server 向け統合テストのプレースホルダーです。
 * <p>
 * WSL2 上での Testcontainers 対応を見据えて、実装前の枠組みのみ提供します。
 * </p>
 *
 * @version 1.0.0-beta.2
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationSqlServerTest {

    /**
     * SQL Server 用統合テストが未実装であることを明示し、実行をスキップします。
     */
    @Test
    void testSqlServerNotImplementedYet() {
        Assumptions.assumeTrue(false, "SQL Server 統合テストは現在未対応のためスキップします。");
    }
}
