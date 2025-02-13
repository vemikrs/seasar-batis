/*
 * Copyright(c) 2025 VEMI, All rights reserved.
 */
package jp.vemi.seasarbatis.core.builder;

import java.util.Map;

/**
 * SQLビルダーの基底インターフェース
 */
interface SBSqlBuilder<T extends SBSqlBuilder<T>> {
    /**
     * SQLをビルドします。
     * @return SQL
     */
    String build();

    /**
     * パラメータを取得します。
     * @return パラメータ
     */
    Map<String, Object> getParameters();
}
