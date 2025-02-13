/*
 * Copyright(c) 2025 VEMI, All rights reserved.
 */
package jp.vemi.seasarbatis.core.builder;

import jp.vemi.seasarbatis.core.criteria.OrderDirection;

/**
 * ORDER BY句を持つSQLビルダーの基底インターフェース
 */
interface SBOrderByCapable<T extends SBOrderByCapable<T>> extends SBSqlBuilder<T> {
    T orderBy(String column);
    T orderBy(String column, OrderDirection direction);
}