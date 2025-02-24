/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * SQL解析時の例外クラスです。
 * <p>
 * SQLの構文解析やパラメータ解決時に発生する例外を表します。
 * 主にSQLコメントの形式不正や条件式の構文エラーなどで発生します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBSqlParseException extends SBException {
    public SBSqlParseException(String message) {
        super(message);
    }

    public SBSqlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}