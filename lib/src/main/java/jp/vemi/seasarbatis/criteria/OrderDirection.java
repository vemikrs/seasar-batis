package jp.vemi.seasarbatis.criteria;

/**
 * ORDER BY句のソート方向を表す列挙型。
 */
public enum OrderDirection {
    /** 昇順 */
    ASC("ASC"),
    /** 降順 */
    DESC("DESC");

    private final String sql;

    OrderDirection(String sql) {
        this.sql = sql;
    }

    /**
     * SQL文で使用する文字列表現を取得します。
     * 
     * @return SQL文で使用する文字列
     */
    public String toSql() {
        return sql;
    }
}