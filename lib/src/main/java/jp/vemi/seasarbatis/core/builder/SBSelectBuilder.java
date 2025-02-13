/*
 * Copyright(c) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jp.vemi.seasarbatis.core.criteria.OrderDirection;
import jp.vemi.seasarbatis.core.criteria.SBWhere;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * SELECT文を構築するビルダークラス。
 * Fluent interfaceパターンでSELECT文を組み立てます。
 * 
 * @param <E> エンティティの型
 */
public class SBSelectBuilder<E> implements
        SBWhereCapable<SBSelectBuilder<E>>,
        SBOrderByCapable<SBSelectBuilder<E>> {

    private final SBJdbcManager jdbcManager;
    private final Class<E> entityClass;
    private SBWhere where;
    private final List<String> orderByList = new ArrayList<>();
    private final Map<String, Object> parameters = new HashMap<>();

    /**
     * コンストラクタ
     * 
     * @param jdbcManager JDBCマネージャー
     * @param entityClass エンティティのクラス
     */
    public SBSelectBuilder(SBJdbcManager jdbcManager, Class<E> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    @Override
    public String build() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ")
                .append(jdbcManager.getTableName(entityClass));

        if (where != null) {
            sql.append(" WHERE ").append(where.build());
            parameters.putAll(where.getParameters());
        }

        if (!orderByList.isEmpty()) {
            sql.append(" ORDER BY ")
                    .append(String.join(", ", orderByList));
        }

        return sql.toString();
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public SBSelectBuilder<E> where(Consumer<SBWhere> consumer) {
        SBWhere newWhere = new SimpleWhere();
        consumer.accept(newWhere);
        return where(newWhere);
    }

    @Override
    public SBSelectBuilder<E> where(SBWhere where) {
        this.where = where;
        return this;
    }

    @Override
    public SBSelectBuilder<E> orderBy(String column) {
        return orderBy(column, OrderDirection.ASC);
    }

    @Override
    public SBSelectBuilder<E> orderBy(String column, OrderDirection direction) {
        orderByList.add(column + " " + direction.name());
        return this;
    }

    /**
     * クエリを実行し、結果のリストを返します。
     * 
     * @return エンティティのリスト
     */
    public List<E> getResultList() {
        return jdbcManager.selectBySql(build(), getParameters());
    }

    /**
     * クエリを実行し、単一の結果を返します。
     * 
     * @return エンティティ。結果が存在しない場合はnull
     * @throws IllegalStateException 複数の結果が存在する場合
     */
    public E getSingleResult() {
        List<E> results = getResultList();
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("複数の結果が見つかりました");
        }
        return results.get(0);
    }
}