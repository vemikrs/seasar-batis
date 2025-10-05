/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.dialect;

/**
 * Oracle Database 23ai 向けの Dialect 実装。
 * <p>
 * DO 範囲: 基本型(文字列/数値/日付/タイムスタンプ)
 * DON'T 範囲: OBJECT型、VARRAY、Nested Table、XML Type、PL/SQL構文
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.1.0
 * @since 2025/10/06
 */
public class OracleDialect implements SBDialect {

    @Override
    public String formatString(String value) {
        if (value == null) {
            return "NULL";
        }
        // Oracleシングルクォートをエスケープ
        String escaped = value.replace("'", "''");
        return "'" + escaped + "'";
    }

    @Override
    public String formatDate(String value) {
        if (value == null) {
            return "NULL";
        }
        // TO_DATE('yyyy-MM-dd HH:mm:ss', 'YYYY-MM-DD HH24:MI:SS')
        return "TO_DATE('" + value + "', 'YYYY-MM-DD HH24:MI:SS')";
    }

    @Override
    public String formatTimestamp(String value) {
        if (value == null) {
            return "NULL";
        }
        // TO_TIMESTAMP('yyyy-MM-dd HH:mm:ss.ffffff', 'YYYY-MM-DD HH24:MI:SS.FF6')
        return "TO_TIMESTAMP('" + value + "', 'YYYY-MM-DD HH24:MI:SS.FF6')";
    }

    @Override
    public String formatArray(String formattedElements) {
        if (formattedElements == null || formattedElements.isEmpty()) {
            return "";
        }
        return formattedElements;
    }

    @Override
    public String getDatabaseProductName() {
        return "Oracle";
    }
}
