/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * エンティティ操作に関する例外クラスです。
 * <p>
 * エンティティのメタデータ取得や値の設定時に発生する例外を表します。
 * 国際化対応のメッセージを提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/08/23
 */
public class SBEntityException extends SBException {
    
    /**
     * メッセージキーから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     */
    public SBEntityException(String messageKey) {
        super(SBMessageManager.getInstance().getMessage(messageKey));
    }
    
    /**
     * メッセージキーとパラメータから国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param args メッセージパラメータ
     */
    public SBEntityException(String messageKey, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args));
    }

    /**
     * メッセージキーと元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     */
    public SBEntityException(String messageKey, Throwable cause) {
        super(SBMessageManager.getInstance().getMessage(messageKey), cause);
    }
    
    /**
     * メッセージキー、パラメータ、元の例外から国際化されたメッセージで例外を生成します。
     * 
     * @param messageKey メッセージキー
     * @param cause 元の例外
     * @param args メッセージパラメータ
     */
    public SBEntityException(String messageKey, Throwable cause, Object... args) {
        super(SBMessageManager.getInstance().getMessage(messageKey, args), cause);
    }
}
