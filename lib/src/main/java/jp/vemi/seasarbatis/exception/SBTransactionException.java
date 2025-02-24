/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * トランザクション操作時の例外クラスです。
 * <p>
 * トランザクションの開始、コミット、ロールバック時に発生した例外を表します。
 * </p>
 */
public class SBTransactionException extends SBException {
    public SBTransactionException(String message) {
        super(message);
    }

    public SBTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}