/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.dialect;

/**
 * データベース方言を表すインタフェース。
 * <p>
 * SQL リテラル生成に特化し、各データベース固有のフォーマット処理を提供します。
 * ResultSet → Java オブジェクト変換は MyBatis TypeHandler に委譲します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.1.0
 * @since 2025/10/06
 */
public interface SBDialect {

    /**
     * 文字列をエスケープしてSQL リテラルとして返します。
     *
     * @param value エスケープする文字列
     * @return エスケープされたSQL リテラル(クォート含む)
     */
    String formatString(String value);

    /**
     * 日付をフォーマットしてSQL リテラルとして返します。
     *
     * @param value フォーマットする日付文字列(yyyy-MM-dd HH:mm:ss 形式)
     * @return フォーマットされたSQL リテラル
     */
    String formatDate(String value);

    /**
     * タイムスタンプをフォーマットしてSQL リテラルとして返します。
     *
     * @param value フォーマットするタイムスタンプ文字列(yyyy-MM-dd HH:mm:ss 形式)
     * @return フォーマットされたSQL リテラル
     */
    String formatTimestamp(String value);

    /**
     * 配列をフォーマットしてSQL リテラルとして返します。
     *
     * @param formattedElements 各要素をフォーマット済みの文字列リスト
     * @return フォーマットされたSQL リテラル
     */
    String formatArray(String formattedElements);

    /**
     * このDialectがサポートするデータベース製品名を返します。
     *
     * @return データベース製品名(例: "PostgreSQL", "Oracle")
     */
    String getDatabaseProductName();
}
