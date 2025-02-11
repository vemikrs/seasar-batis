package jp.vemi.seasarbatis.jdbc.selector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.vemi.seasarbatis.criteria.OrderDirection;
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
    private final List<String> orderByList = new ArrayList<>();

    /**
     * Fromインスタンスを作成します。
     * 
     * @param jdbcManager JDBCマネージャー
     * @param entityClass エンティティのクラス
     */
    public From(SBJdbcManager jdbcManager, Class<T> entityClass) {
        this.jdbcManager = jdbcManager;
        this.entityClass = entityClass;
    }

    /**
     * Where条件を設定します。
     * 
     * @param where 検索条件
     * @return このFromインスタンス
     */
    public From<T> where(Where where) {
        this.where = where;
        return this;
    }

    /**
     * ORDER BY句を設定します。
     * 
     * @param orderBy ソート条件（例: "name, age DESC"）
     * @return このFromインスタンス
     */
    public From<T> orderBy(String orderBy) {
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            orderByList.add(orderBy);
        }
        return this;
    }

    /**
     * ORDER BY句を追加します。
     * 
     * @param column    カラム名
     * @param direction ソート方向
     * @return このインスタンス
     */
    public From<T> orderBy(String column, OrderDirection direction) {
        if (column != null && !column.trim().isEmpty()) {
            orderByList.add(column + " " + direction.toSql());
        }
        return this;
    }

    /**
     * クエリを実行し、単一の結果を返します。
     * 
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
     * 
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

        if (!orderByList.isEmpty()) {
            sql.append(" ORDER BY ")
                    .append(String.join(", ", orderByList));
        }

        return jdbcManager.findBySql(sql.toString(), params);
    }
}