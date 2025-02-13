/*
 * Copyright(c) 2025 VEMI, All rights reserved.
 */
package jp.vemi.seasarbatis.core.builder;

import java.util.function.Consumer;

import jp.vemi.seasarbatis.core.criteria.SBWhere;

/**
 * WHERE句を持つSQLビルダーの基底インターフェース
 */
interface SBWhereCapable<T extends SBWhereCapable<T>> extends SBSqlBuilder<T> {
    T where(SBWhere where);
    T where(Consumer<SBWhere> whereConsumer);
}