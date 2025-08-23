/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * 型変換時の例外クラスです。
 * <p>
 * エンティティへのマッピング時や値の型変換時に発生するエラーを表します。
 * 主に数値や日付などの型変換失敗時にスローされます。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBTypeConversionException extends SBException {
    
    public SBTypeConversionException(String message) {
        super(message);
    }

    public SBTypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}