/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * 不正な状態を表す例外クラスです。
 * <p>
 * メソッドの実行に必要な前提条件が満たされていない場合などにスローされます。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBIllegalStateException extends SBException {
    public SBIllegalStateException(String message) {
        super(message);
    }

    public SBIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }
}