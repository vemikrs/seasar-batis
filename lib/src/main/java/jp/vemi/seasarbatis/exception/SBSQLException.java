/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * SQL実行時の例外クラスです。
 * <p>
 * SQL文の実行時に発生するデータベース関連の例外を表します。
 * 国際化対応のメッセージを提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/01/01
 */
public class SBSQLException extends SBException {
    
    /**
     * メッセージキーから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     */
    public SBSQLException(String messageKey) {
        super(SBMessageManager.getInstance().getMessage(messageKey));
    }
    
    /**
     * メッセージキーとパラメータから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param args メッセージパラメータ
     */
    public SBSQLException(String messageKey, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args));
    }

    /**
     * メッセージキーと元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     */
    public SBSQLException(String messageKey, Throwable cause) {
        super(SBMessageManager.getInstance().getMessage(messageKey), cause);
    }
    
    /**
     * メッセージキー、パラメータ、元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     * @param args メッセージパラメータ
     */
    public SBSQLException(String messageKey, Throwable cause, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args), cause);
    }
}