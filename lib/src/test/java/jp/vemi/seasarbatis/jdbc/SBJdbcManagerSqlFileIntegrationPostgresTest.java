/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * PostgreSQL 向け統合テストのプレースホルダークラスです。
 * <p>
 * 今後 Testcontainers などを利用して PostgreSQL での動作確認を行うための土台を用意します。
 * </p>
 *
 * @version 1.0.0-beta.2
 * @author VEMI
 */
@Tag("integration")
class SBJdbcManagerSqlFileIntegrationPostgresTest {

    /**
     * PostgreSQL 用統合テストがまだ準備段階であることを示し、実行をスキップします。
     */
    @Test
    void testPostgresNotImplementedYet() {
        Assumptions.assumeTrue(false, "PostgreSQL 統合テストは現在未対応のためスキップします。");
    }
}
