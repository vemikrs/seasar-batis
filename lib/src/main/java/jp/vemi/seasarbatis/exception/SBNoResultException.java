/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * 検索結果が0件の場合にスローされる例外です。
 * <p>
 * 1件以上の結果が期待される検索で結果が0件だった場合にスローされます。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBNoResultException extends SBException {
    public SBNoResultException(String message) {
        super(message);
    }

    public SBNoResultException(String message, Throwable cause) {
        super(message, cause);
    }
}