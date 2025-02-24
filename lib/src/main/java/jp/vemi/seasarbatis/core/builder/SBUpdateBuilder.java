/*
 * Copyright(c) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.builder;
import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.getTableName;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import jp.vemi.seasarbatis.core.criteria.SBWhere;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * UPDATE文を構築するビルダークラス。
 * Fluent interfaceパターンでUPDATE文を組み立てます。
 * 
 * @param <E> エンティティの型
 */
public class SBUpdateBuilder<E> implements SBWhereCapable<SBUpdateBuilder<E>> {

    private final SBJdbcManager jdbcManager;
    private final Class<E> entityClass;
    private final Map<String, Object> setValues = new LinkedHashMap<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private SBWhere where;

    /**
     * コンストラクタ
     * 
     * @param jdbcManager JDBCマネージャー
     * @param entityClass エンティティのクラス
     */
    public SBUpdateBuilder(SBJdbcManager jdbcManager, Class<E> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    /**
     * 更新するカラムと値を設定します。
     * 
     * @param column カラム名
     * @param value  設定値
     * @return このビルダーインスタンス
     */
    public SBUpdateBuilder<E> set(String column, Object value) {
        setValues.put(column, value);
        return this;
    }

    @Override
    public String build() {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ")
                .append(getTableName(entityClass))
                .append(" SET ");

        // SET句の構築
        setValues.forEach((column, value) -> {
            sql.append(column)
                    .append(" = /*")
                    .append(column)
                    .append("*/?, ");
            parameters.put(column, value);
        });

        // 最後のカンマを削除
        sql.setLength(sql.length() - 2);

        // WHERE句の追加
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
    public SBUpdateBuilder<E> where(Consumer<SBWhere> consumer) {
        SBWhere newWhere = new SimpleWhere();
        consumer.accept(newWhere);
        return where(newWhere);
    }

    @Override
    public SBUpdateBuilder<E> where(SBWhere where) {
        this.where = where;
        return this;
    }

    /**
     * UPDATE文を実行します。
     * 
     * @return 更新された行数
     */
    public int execute() {
        return jdbcManager.update(build(), getParameters());
    }
}