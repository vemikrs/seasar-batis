/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * トランザクション操作時の例外クラスです。
 * <p>
 * トランザクションの開始、コミット、ロールバック時に発生した例外を表します。
 * 国際化対応のメッセージを提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/08/23
 */
public class SBTransactionException extends SBException {
    
    /**
     * メッセージキーから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     */
    public SBTransactionException(String messageKey) {
        super(SBMessageManager.getInstance().getMessage(messageKey));
    }
    
    /**
     * メッセージキーとパラメータから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param args メッセージパラメータ
     */
    public SBTransactionException(String messageKey, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args));
    }

    /**
     * メッセージキーと元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     */
    public SBTransactionException(String messageKey, Throwable cause) {
        super(SBMessageManager.getInstance().getMessage(messageKey), cause);
    }
    
    /**
     * メッセージキー、パラメータ、元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     * @param args メッセージパラメータ
     */
    public SBTransactionException(String messageKey, Throwable cause, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args), cause);
    }
}