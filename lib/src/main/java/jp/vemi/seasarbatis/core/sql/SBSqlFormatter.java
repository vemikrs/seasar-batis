/*
 * Copyright(c) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql;

/**
 * SQL文を整形するユーティリティクラス。
 */
public class SBSqlFormatter {

    /**
     * SQL文をシンプルに整形します。
     * - 複数のホワイトスペースを1つに統一
     * - 複数行の改行を1行に統一
     * - ホワイトスペースのみの行を削除
     *
     * @param sql 整形前のSQL文
     * @return 整形後のSQL文
     */
    public static String simplify(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }

        return sql
                // 複数行の改行を1行に
                .replaceAll("\\n{2,}", "\n")
                // ホワイトスペースのみの行を削除
                .replaceAll("(?m)^[ \t]*\r?\n", "")
                // 複数のホワイトスペースを1つに
                .replaceAll("\\s+", " ")
                .trim();
    }
}