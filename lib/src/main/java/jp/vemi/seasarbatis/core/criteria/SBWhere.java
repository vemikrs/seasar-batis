/*
 * Copyright(c) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.criteria;

import java.util.Map;

/**
 * SQL WHERE句を構築するためのインターフェース。
 * 条件式の生成と結合をサポートします。
 */
public interface SBWhere {

    /**
     * WHERE句のSQL文を構築します。
     * 
     * @return 構築されたWHERE句のSQL文
     */
    String build();

    /**
     * 等価条件（=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere eq(String column, Object value);

    /**
     * 不等価条件（<>）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere ne(String column, Object value);

    /**
     * より大きい条件（>）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere gt(String column, Object value);

    /**
     * 以上条件（>=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere ge(String column, Object value);

    /**
     * より小さい条件（<）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere lt(String column, Object value);

    /**
     * 以下条件（<=）を追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere le(String column, Object value);

    /**
     * LIKE条件を追加します。
     * 
     * @param column カラム名
     * @param value  検索パターン
     * @return このインスタンス
     */
    SBWhere like(String column, Object value);

    /**
     * IS NULL条件を追加します。
     * 
     * @param column カラム名
     * @return このインスタンス
     */
    SBWhere isNull(String column);

    /**
     * IS NOT NULL条件を追加します。
     * 
     * @param column カラム名
     * @return このインスタンス
     */
    SBWhere isNotNull(String column);

    /**
     * IN条件を追加します。
     * 
     * @param column カラム名
     * @param values 比較値の配列
     * @return このインスタンス
     */
    SBWhere in(String column, Object... values);

    /**
     * NOT IN条件を追加します。
     * 
     * @param column カラム名
     * @param values 比較値の配列
     * @return このインスタンス
     */
    SBWhere notIn(String column, Object... values);

    /**
     * BETWEEN条件を追加します。
     * 
     * @param column カラム名
     * @param value1 開始値
     * @param value2 終了値
     * @return このインスタンス
     */
    SBWhere between(String column, Object value1, Object value2);

    /**
     * NOT BETWEEN条件を追加します。
     * 
     * @param column カラム名
     * @param value1 開始値
     * @param value2 終了値
     * @return このインスタンス
     */
    SBWhere notBetween(String column, Object value1, Object value2);

    /**
     * 別のWhere条件をANDで結合します。
     * 
     * @param where 結合する条件
     * @return このインスタンス
     */
    SBWhere and(SBWhere where);

    /**
     * 別のWhere条件をORで結合します。
     * 
     * @param where 結合する条件
     * @return このインスタンス
     */
    SBWhere or(SBWhere where);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere and(String column, Object value);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @return このインスタンス
     */
    SBWhere or(String column, Object value);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @return このインスタンス
     */
    SBWhere and(String column, Object value, boolean isAdd);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @return このインスタンス
     */
    SBWhere or(String column, Object value, boolean isAdd);

    /**
     * 等価条件をANDで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @return このインスタンス
     */
    SBWhere and(String column, Object value, boolean isAdd, boolean isOr);

    /**
     * 等価条件をORで追加します。
     * 
     * @param column カラム名
     * @param value  比較値
     * @param isAdd  条件を追加するかどうか
     * @param isOr   OR条件として追加するかどうか
     * @return このインスタンス
     */
    SBWhere or(String column, Object value, boolean isAdd, boolean isOr);

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
    SBWhere and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot);

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
    SBWhere or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot);

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
    SBWhere and(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd);

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
    SBWhere or(String column, Object value, boolean isAdd, boolean isOr, boolean isNot, boolean isAnd);

    /**
     * 構築されたWHERE句のSQL文を取得します。
     * 
     * @return WHERE句のSQL文
     */
    String getWhereSql();

    /**
     * バインドパラメータを取得します。
     * 
     * @return パラメータのマップ
     */
    Map<String, Object> getParameters();

    /**
     * 条件が存在するかを確認します。
     * 
     * @return 条件が1つ以上存在する場合はtrue
     */
    boolean hasConditions();
}
