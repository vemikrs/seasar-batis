/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.core.criteria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 複合的なWHERE句を構築するためのクラス。
 * 複数のWhere条件をAND/ORで結合できます。
 */
public class ComplexWhere extends AbstractWhere<ComplexWhere> {

    private final List<SBWhere> wheres = new ArrayList<>();
    private final List<String> operators = new ArrayList<>();
    private String nextOperator = "AND";

    /**
     * WHEREの条件を追加します。
     * 
     * @param where 追加する条件
     * @return このインスタンス
     */
    public ComplexWhere add(SBWhere where) {
        if (where != null && where.hasConditions()) {
            wheres.add(where);
            if (!operators.isEmpty()) {
                operators.add(nextOperator);
            }
        }
        return this;
    }

    /**
     * 次の条件をANDで結合するように設定します。
     * 
     * @return このインスタンス
     */
    public ComplexWhere and() {
        this.nextOperator = "AND";
        return this;
    }

    /**
     * 次の条件をORで結合するように設定します。
     * 
     * @return このインスタンス
     */
    public ComplexWhere or() {
        this.nextOperator = "OR";
        return this;
    }

    @Override
    public String getWhereSql() {
        if (wheres.isEmpty()) {
            return super.getWhereSql();
        }

        StringBuilder sql = new StringBuilder();
        if (!conditions.isEmpty()) {
            sql.append(super.getWhereSql().replace("WHERE", ""));
            sql.append(" ");
            sql.append(nextOperator);
            sql.append(" ");
        } else if (!wheres.isEmpty()) {
            sql.append(" WHERE ");
        }

        for (int i = 0; i < wheres.size(); i++) {
            if (i > 0) {
                sql.append(" ").append(operators.get(i - 1)).append(" ");
            }
            String whereSql = wheres.get(i).getWhereSql();
            sql.append("(").append(whereSql.replace("WHERE", "")).append(")");
        }

        return sql.toString();
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> allParams = new LinkedHashMap<>(super.getParameters());
        for (SBWhere where : wheres) {
            allParams.putAll(where.getParameters());
        }
        return allParams;
    }

    @Override
    public boolean hasConditions() {
        if (super.hasConditions()) {
            return true;
        }
        return wheres.stream().anyMatch(SBWhere::hasConditions);
    }
}