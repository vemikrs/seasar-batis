package jp.vemi.seasarbatis.jdbc.updater;

import java.util.HashMap;
import java.util.Map;

import jp.vemi.seasarbatis.criteria.Where;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * UPDATE文を構築するためのビルダークラス。
 * @param <T> エンティティの型
 */
public class UpdateBuilder<T> {
    private final SBJdbcManager jdbcManager;
    private final Class<T> entityClass;
    private final Map<String, Object> values = new HashMap<>();
    private Where where;

    public UpdateBuilder(SBJdbcManager jdbcManager, Class<T> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    /**
     * 更新するカラムと値を設定します。
     * @param column カラム名
     * @param value 更新値
     * @return このビルダー
     */
    public UpdateBuilder<T> set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    /**
     * Where条件を設定します。
     * @param where 更新条件
     * @return このビルダー
     */
    public UpdateBuilder<T> where(Where where) {
        this.where = where;
        return this;
    }

    /**
     * 更新を実行します。
     * @return 更新された行数
     */
    public int execute() {
        StringBuilder sql = new StringBuilder("UPDATE ")
            .append(jdbcManager.getTableName(entityClass))
            .append(" SET ");

        Map<String, Object> params = new HashMap<>();

        // SET句の構築
        values.forEach((column, value) -> {
            sql.append(column)
               .append(" = /*")
               .append(column)
               .append("*/0, ");
            params.put(column, value);
        });
        sql.setLength(sql.length() - 2);  // 末尾のカンマとスペースを削除

        // WHERE句の追加
        if (where != null && where.hasConditions()) {
            sql.append(where.getWhereSql());
            params.putAll(where.getParameters());
        }

        return jdbcManager.update(sql.toString(), params);
    }
}