package jp.vemi.seasarbatis.jdbc.selector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.vemi.seasarbatis.criteria.SimpleWhere;
import jp.vemi.seasarbatis.criteria.Where;
import jp.vemi.seasarbatis.jdbc.manager.SBJdbcManager;

/**
 * Select操作のためのクエリビルダークラス。
 * Seasar2のJdbcManagerライクなインターフェースを提供します。
 * 
 * @param <T> エンティティの型
 */
public class From<T> {
    private final SBJdbcManager jdbcManager;
    private final Class<T> entityClass;
    private Where where;
    private String orderBy;

    /**
     * Fromインスタンスを作成します。
     * @param jdbcManager JDBCマネージャー
     * @param entityClass エンティティのクラス
     */
    public From(SBJdbcManager jdbcManager, Class<T> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    /**
     * Where条件を設定します。
     * @param where 検索条件
     * @return このFromインスタンス
     */
    public From<T> where(Where where) {
        this.where = where;
        return this;
    }

    /**
     * 新しいWhere条件を作成します。
     * @return SimpleWhereインスタンス
     */
    public SimpleWhere where() {
        SimpleWhere where = jdbcManager.where();
        this.where = where;
        return where;
    }

    /**
     * ORDER BY句を設定します。
     * @param orderBy ソート条件（例: "name ASC, age DESC"）
     * @return このFromインスタンス
     */
    public From<T> orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /**
     * クエリを実行し、単一の結果を返します。
     * @return 検索結果のエンティティ。結果が見つからない場合はnull
     * @throws IllegalStateException 複数の結果が見つかった場合
     */
    public T getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("複数の結果が見つかりました");
        }
        return results.get(0);
    }

    /**
     * クエリを実行し、結果のリストを返します。
     * @return 検索結果のリスト
     */
    public List<T> getResultList() {
        StringBuilder sql = new StringBuilder("SELECT * FROM ")
            .append(jdbcManager.getTableName(entityClass));

        Map<String, Object> params = new HashMap<>();
        if (where != null && where.hasConditions()) {
            sql.append(where.getWhereSql());
            params.putAll(where.getParameters());
        }

        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
        }

        return jdbcManager.findBySql(sql.toString(), params);
    }
}