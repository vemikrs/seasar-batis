/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * 検索結果が複数件の場合にスローされる例外です。
 * <p>
 * 1件のみの結果が期待される検索で複数件の結果が返された場合にスローされます。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBNonUniqueResultException extends SBException {
    public SBNonUniqueResultException(String message) {
        super(message);
    }

    public SBNonUniqueResultException(String message, Throwable cause) {
        super(message, cause);
    }
}