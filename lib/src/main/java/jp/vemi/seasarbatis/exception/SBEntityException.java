/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

/**
 * エンティティ操作に関する例外クラスです。
 * <p>
 * エンティティのメタデータ取得や値の設定時に発生する例外を表します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBEntityException extends SBException {
    public SBEntityException(String message) {
        super(message);
    }

    public SBEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
