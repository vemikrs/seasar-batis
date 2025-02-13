package jp.vemi.seasarbatis.core.criteria;

import java.util.Map;

/**
 * SQL WHERE句を構築するためのインターフェース。
 * 条件式の生成と結合をサポートします。
 */
public interface Where {

    /**
     * 等価条件（=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where eq(String column, Object value);

    /**
     * 不等価条件（<>）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where ne(String column, Object value);

    /**
     * より大きい条件（>）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where gt(String column, Object value);

    /**
     * 以上条件（>=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where ge(String column, Object value);

    /**
     * より小さい条件（<）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where lt(String column, Object value);

    /**
     * 以下条件（<=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where le(String column, Object value);

    /**
     * LIKE条件を追加します。
     * 
     * @param column カラム名
     * @param value  検索パターン
     * @return このインスタンス
     */
    public Where like(String column, Object value);

    /**
     * IS NULL条件を追加します。
     * 
     * @param column カラム名
     * @return このインスタンス
     */
    public Where isNull(String column);

    /**
     * IS NOT NULL条件を追加します。
     * 
     * @param column カラム名
     * @return このインスタンス
     */
    public Where isNotNull(String column);

    /**
     * IN条件を追加します。
     * 
     * @param column カラム名
     * @param values 比較値の配列
     * @return このインスタンス
     */
    public Where in(String column, Object... values);

    /**
     * NOT IN条件を追加します。
     * 
     * @param column カラム名
     * @param values 比較値の配列
     * @return このインスタンス
     */
    public Where notIn(String column, Object... values);

    /**
     * BETWEEN条件を追加します。
     * 
     * @param column カラム名
     * @param value1 開始値
     * @param value2 終了値
     * @return このインスタンス
     */
    public Where between(String column, Object value1, Object value2);

    /**
     * NOT BETWEEN条件を追加します。
     * 
     * @param column カラム名
     * @param value1 開始値
     * @param value2 終了値
     * @return このインスタンス
     */
    public Where notBetween(String column, Object value1, Object value2);

    /**
     * 別のWhere条件をANDで結合します。
     * 
     * @param where 結合する条件
     * @return このインスタンス
     */
    public Where and(Where where);

    /**
     * 別のWhere条件をORで結合します。
     * 
     * @param where 結合する条件
     * @return このインスタンス
     */
    public Where or(Where where);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where and(String column, Object value);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    public Where or(String column, Object value);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @return このインスタンス
     */
    public Where and(String column, Object value, boolean isAdd);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @return このインスタンス
     */
    public Where or(String column, Object value, boolean isAdd);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @return このインスタンス
     */
    public Where and(String column, Object value, boolean isAdd, boolean isOr);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @return このインスタンス
     */
    public Where or(String column, Object value, boolean isAdd, boolean isOr);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @param isNot  NOT条件として追加するかどうか
     * @return このインスタンス
     */
    public Where and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @param isNot  NOT条件として追加するかどうか
     * @return このインスタンス
     */
    public Where or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @param isNot  NOT条件として追加するかどうか
     * @param isAnd  AND条件として追加するかどうか
     * @return このインスタンス
     */
    public Where and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @param isNot  NOT条件として追加するかどうか
     * @param isAnd  AND条件として追加するかどうか
     * @return このインスタンス
     */
    public Where or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd);

    /**
     * 構築されたWHERE句のSQL文を取得します。
     * 
     * @return WHERE句のSQL文
     */
    public String getWhereSql();

    /**
     * バインドパラメータを取得します。
     * 
     * @return パラメータのマップ
     */
    public Map<String, Object> getParameters();

    /**
     * 条件が存在するかを確認します。
     * 
     * @return 条件が1つ以上存在する場合はtrue
     */
    public boolean hasConditions();
}
