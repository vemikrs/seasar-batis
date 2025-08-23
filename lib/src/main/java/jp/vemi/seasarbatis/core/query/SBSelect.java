/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.query;

import static jp.vemi.seasarbatis.core.entity.SBEntityOperations.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.entity.SBPrimaryKeyInfo;
import jp.vemi.seasarbatis.core.sql.CommandType;
import jp.vemi.seasarbatis.core.sql.executor.SBQueryExecutor;
import jp.vemi.seasarbatis.core.transaction.SBTransactionOperation;
import jp.vemi.seasarbatis.exception.SBException;
import jp.vemi.seasarbatis.exception.SBNoResultException;
import jp.vemi.seasarbatis.exception.SBNonUniqueResultException;

/**
 * 型安全な検索クエリを提供するクラスです。
 * <p>
 * Fluent APIによる型安全な検索クエリの構築と実行を提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBSelect<T> {
    private static final Logger logger = LoggerFactory.getLogger(SBSelect.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final SBQueryExecutor queryExecutor;
    private final SBTransactionOperation txOperation;

    private Class<T> entityClass;
    private String sql;
    private String sqlFile;
    private Map<String, Object> params = new HashMap<>();
    private Map<String, Object> primaryKeys;
    private boolean suppressException;

    /**
     * コンストラクタ
     *
     * @param sqlSessionFactory SQLセッションファクトリー
     * @param queryExecutor クエリ実行オブジェクト
     * @param txOperation トランザクション操作オブジェクト
     */
    public SBSelect(SqlSessionFactory sqlSessionFactory, SBQueryExecutor queryExecutor,
            SBTransactionOperation txOperation) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.queryExecutor = queryExecutor;
        this.txOperation = txOperation;
    }

    /**
     * 検索対象のエンティティクラスを設定します。
     *
     * @param entityClass エンティティクラス
     * @return SBSelectインスタンス
     */
    public SBSelect<T> from(Class<T> entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    /**
     * 実行するSQL文を設定します。
     *
     * @param sql SQL文
     * @return SBSelectインスタンス
     */
    public SBSelect<T> withSql(String sql) {
        this.sql = sql;
        return this;
    }

    /**
     * 実行するSQLファイルパスを設定します。
     *
     * @param sqlFile SQLファイルパス
     * @return SBSelectインスタンス
     */
    public SBSelect<T> withSqlFile(String sqlFile) {
        this.sqlFile = sqlFile;
        return this;
    }

    /**
     * パラメータを設定します。
     *
     * @param params パラメータ
     * @return SBSelectインスタンス
     */
    public SBSelect<T> withParams(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

    /**
     * 主キーによる検索条件を設定します。
     *
     * @param primaryKeys 主キー
     * @return SBSelectインスタンス
     */
    public SBSelect<T> byPrimaryKey(Map<String, Object> primaryKeys) {
        this.primaryKeys = primaryKeys;
        return this;
    }

    /**
     * 例外を抑制するかどうかを設定します。
     *
     * @return SBSelectインスタンス
     */
    public SBSelect<T> suppressException() {
        this.suppressException = true;
        return this;
    }

    /**
     * 検索結果を1件返します。
     *
     * @return 検索結果
     * @throws SBNoResultException 検索結果が0件の場合
     * @throws SBNonUniqueResultException 検索結果が複数件存在する場合
     */
    public T getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            if (!suppressException) {
                throw new SBNoResultException("検索結果が0件でした");
            }
            return null;
        }
        if (results.size() > 1) {
            throw new SBNonUniqueResultException("検索結果が複数件存在します: " + results.size() + "件");
        }
        return results.get(0);
    }

    /**
     * 検索結果をリストで返します。
     *
     * @return 検索結果
     */
    public List<T> getResultList() {
        try {
            if (!txOperation.isActive()) {
                txOperation.begin(sqlSessionFactory.openSession(false));
            }

            if (sql != null) {
                return queryExecutor.executeSelect(sql, params, entityClass);
            } else if (sqlFile != null) {
                return queryExecutor.executeFile(sqlFile, params, CommandType.SELECT);
            } else if (primaryKeys != null) {
                // 主キーによる検索のロジック
                SBPrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entityClass);
                String tableName = getTableName(entityClass);
                StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");

                for (int i = 0; i < primaryKeys.size(); i++) {
                    if (i > 0) {
                        sqlBuilder.append(" AND ");
                    }
                    String propertyName = pkInfo.getColumnNames().get(i);
                    sqlBuilder.append(propertyName).append(" = /*pk").append(i).append("*/").append(i);
                    params.put("pk" + i, primaryKeys.get(propertyName));
                }

                return queryExecutor.executeSelect(sqlBuilder.toString(), params, entityClass);
            } else {
                // 全件検索
                String tableName = getTableName(entityClass);
                return queryExecutor.executeSelect("SELECT * FROM " + tableName, params, entityClass);
            }
        } catch (Exception e) {
            if (suppressException) {
                logger.warn("検索実行中の例外を抑制します。: {}", e.getMessage());
                return Collections.emptyList();
            }
            throw new SBException("検索実行中にエラーが発生しました", e);
        }
    }
}