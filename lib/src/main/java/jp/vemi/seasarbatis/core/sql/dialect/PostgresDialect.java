/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.dialect;

/**
 * PostgreSQL 17.x および H2 (PostgreSQL Mode) 向けの Dialect 実装。
 * <p>
 * DO 範囲: 基本型(文字列/数値/日付/タイムスタンプ/配列)、JSONB、ARRAY
 * DON'T 範囲: jsonb_set, array_agg 等の関数、Composite型、Range型
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.1.0
 * @since 2025/10/06
 */
public class PostgresDialect implements SBDialect {

    @Override
    public String formatString(String value) {
        if (value == null) {
            return "NULL";
        }
        // PostgreSQL標準: シングルクォートをエスケープ
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }

    @Override
    public String formatDate(String value) {
        if (value == null) {
            return "NULL";
        }
        // TIMESTAMP 'yyyy-MM-dd HH:mm:ss' 形式
        return "TIMESTAMP '" + value + "'";
    }

    @Override
    public String formatTimestamp(String value) {
        if (value == null) {
            return "NULL";
        }
        // TIMESTAMP 'yyyy-MM-dd HH:mm:ss' 形式
        return "TIMESTAMP '" + value + "'";
    }

    @Override
    public String formatArray(String formattedElements) {
        if (formattedElements == null || formattedElements.isEmpty()) {
            return "ARRAY[]";
        }
        // ARRAY[...] 構文
        return "ARRAY[" + formattedElements + "]";
    }

    @Override
    public String getDatabaseProductName() {
        return "PostgreSQL";
    }
}
