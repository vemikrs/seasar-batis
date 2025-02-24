/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * SeasarBatisの基底例外クラスです。
 * <p>
 * SeasarBatisで発生する全ての例外はこのクラスを継承します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBException extends RuntimeException {

    /**
     * 例外を生成します。
     * 
     * @param message エラーメッセージ
     */
    public SBException(String message) {
        super(message);
    }

    /**
     * 元の例外を保持した例外を生成します。
     * 
     * @param message エラーメッセージ
     * @param cause 元の例外
     */
    public SBException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 元の例外を保持した例外を生成します。
     * 
     * @param cause 元の例外
     */
    public SBException(Throwable cause) {
        super(cause);
    }
}