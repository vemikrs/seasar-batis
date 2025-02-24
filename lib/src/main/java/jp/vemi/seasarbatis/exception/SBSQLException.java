/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * SQL実行時の例外クラスです。
 * <p>
 * SQL文の実行時に発生するデータベース関連の例外を表します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBSQLException extends SBException {
    public SBSQLException(String message) {
        super(message);
    }

    public SBSQLException(String message, Throwable cause) {
        super(message, cause);
    }
}