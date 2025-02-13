/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql;

/**
 * SQLコマンドタイプを表す列挙型です。
 */
public enum CommandType {
    /** SELECT文 */
    SELECT,
    /** INSERT文 */
    INSERT,
    /** UPDATE文 */
    UPDATE,
    /** DELETE文 */
    DELETE;

    /**
     * マッパーステートメントのIDを取得します。
     * 
     * @return マッパーステートメントのID
     */
    public String getStatementId() {
        return "jp.vemi.seasarbatis.prepared" + name();
    }
}