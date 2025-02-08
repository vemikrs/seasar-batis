/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.criteria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbstractWhere<T extends AbstractWhere<T>> implements Where {

    protected final List<String> conditions = new ArrayList<>();
    protected final Map<String, Object> parameters = new LinkedHashMap<>();
    private int parameterIndex = 0;

    @SuppressWarnings("unchecked")
    protected T addCondition(String condition, Object... params) {
        conditions.add(condition);
        for (Object param : params) {
            parameters.put("param" + parameterIndex++, param);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T eq(String column, Object value) {
        if (value != null) {
            addCondition(column + " = /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T ne(String column, Object value) {
        if (value != null) {
            addCondition(column + " <> /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T gt(String column, Object value) {
        if (value != null) {
            addCondition(column + " > /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T ge(String column, Object value) {
        if (value != null) {
            addCondition(column + " >= /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T lt(String column, Object value) {
        if (value != null) {
            addCondition(column + " < /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T le(String column, Object value) {
        if (value != null) {
            addCondition(column + " <= /*param" + parameterIndex + "*/0", value);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T like(String column, Object value) {
        if (value != null) {
            addCondition(column + " LIKE /*param" + parameterIndex + "*/''", "%" + value + "%");
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T isNull(String column) {
        addCondition(column + " IS NULL");
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T isNotNull(String column) {
        addCondition(column + " IS NOT NULL");
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T in(String column, Object... values) {
        if (values != null && values.length > 0) {
            StringBuilder sb = new StringBuilder(column).append(" IN (");
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("/*param").append(parameterIndex + i).append("*/0");
            }
            sb.append(")");
            addCondition(sb.toString(), values);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T notIn(String column, Object... values) {
        if (values != null && values.length > 0) {
            StringBuilder sb = new StringBuilder(column).append(" NOT IN (");
            for (int i = 0; i < values.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append("/*param").append(parameterIndex + i).append("*/0");
            }
            sb.append(")");
            addCondition(sb.toString(), values);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T between(String column, Object value1, Object value2) {
        if (value1 != null && value2 != null) {
            addCondition(
                    column + " BETWEEN /*param" + parameterIndex + "*/0 AND /*param" + (parameterIndex + 1) + "*/0",
                    value1, value2);
        }
        return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T notBetween(String column, Object value1, Object value2) {
        if (value1 != null && value2 != null) {
            addCondition(
                    column + " NOT BETWEEN /*param" + parameterIndex + "*/0 AND /*param" + (parameterIndex + 1) + "*/0",
                    value1, value2);
        }
        return (T) this;
    }

    @Override
    public String getWhereSql() {
        StringBuilder sql = new StringBuilder();
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return sql.toString();
    }

    @Override
    public Map<String, Object> getParameters() {
        return new LinkedHashMap<>(parameters);
    }

    @Override
    public Where and(Where where) {
        if (where != null && where.hasConditions()) {
            String whereSql = where.getWhereSql().replace("WHERE", "");
            addCondition("(" + whereSql + ")");
            parameters.putAll(where.getParameters());
        }
        return this;
    }

    @Override
    public Where or(Where where) {
        if (where != null && where.hasConditions()) {
            conditions.add("OR");
            String whereSql = where.getWhereSql().replace("WHERE", "");
            addCondition("(" + whereSql + ")");
            parameters.putAll(where.getParameters());
        }
        return this;
    }

    @Override
    public Where and(String column, Object value) {
        return and(column, value, true);
    }

    @Override
    public Where or(String column, Object value) {
        return or(column, value, true);
    }

    @Override
    public Where and(String column, Object value, boolean isAdd) {
        return and(column, value, isAdd, false);
    }

    @Override
    public Where or(String column, Object value, boolean isAdd) {
        return or(column, value, isAdd, false);
    }

    @Override
    public Where and(String column, Object value, boolean isAdd, boolean isOr) {
        return and(column, value, isAdd, isOr, false);
    }

    @Override
    public Where or(String column, Object value, boolean isAdd, boolean isOr) {
        return or(column, value, isAdd, isOr, false);
    }

    @Override
    public Where and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot) {
        return and(column, value, isAdd, isOr, isNot, true);
    }

    @Override
    public Where or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot) {
        return or(column, value, isAdd, isOr, isNot, true);
    }

    @Override
    public Where and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd) {
        if (!isAdd || value == null) {
            return this;
        }

        StringBuilder condition = new StringBuilder();
        if (!conditions.isEmpty() && isAnd) {
            condition.append("AND ");
        }
        if (isNot) {
            condition.append("NOT ");
        }
        condition.append(column).append(" = /*param").append(parameterIndex).append("*/0");

        if (isOr) {
            conditions.add("OR");
        }
        addCondition(condition.toString(), value);
        return this;
    }

    @Override
    public Where or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd) {
        if (!isAdd || value == null) {
            return this;
        }

        StringBuilder condition = new StringBuilder();
        if (!conditions.isEmpty() && !isAnd) {
            condition.append("OR ");
        }
        if (isNot) {
            condition.append("NOT ");
        }
        condition.append(column).append(" = /*param").append(parameterIndex).append("*/0");

        if (isOr) {
            conditions.add("OR");
        }
        addCondition(condition.toString(), value);
        return this;
    }

    @Override
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
}
