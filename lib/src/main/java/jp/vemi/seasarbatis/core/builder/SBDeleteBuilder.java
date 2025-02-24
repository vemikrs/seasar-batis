/*
 * Copyright(c) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.builder;

import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.getTableName;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import jp.vemi.seasarbatis.core.criteria.SBWhere;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * DELETE文を構築するビルダークラス。 Fluent interfaceパターンでDELETE文を組み立てます。
 * 
 * @param <E> エンティティの型
 */
public class SBDeleteBuilder<E> implements SBWhereCapable<SBDeleteBuilder<E>> {

    private final SBJdbcManager jdbcManager;
    private final Class<E> entityClass;
    private final Map<String, Object> parameters = new HashMap<>();
    private SBWhere where;

    /**
     * コンストラクタ
     * 
     * @param jdbcManager JDBCマネージャー
     * @param entityClass エンティティのクラス
     */
    public SBDeleteBuilder(SBJdbcManager jdbcManager, Class<E> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    @Override
    public String build() {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(getTableName(entityClass));

        if (where != null) {
            sql.append(" WHERE ").append(where.build());
            parameters.putAll(where.getParameters());
        }

        return sql.toString();
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public SBDeleteBuilder<E> where(Consumer<SBWhere> consumer) {
        SimpleWhere newWhere = new SimpleWhere();
        consumer.accept(newWhere);
        return where(newWhere);
    }

    @Override
    public SBDeleteBuilder<E> where(SBWhere where) {
        this.where = where;
        return this;
    }

    /**
     * DELETE文を実行します。
     * 
     * @return 削除された行数
     */
    public int execute() {
        return jdbcManager.delete(build(), getParameters());
    }
}
