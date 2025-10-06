/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.core;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;

import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * externalized SQLを実行するクラス。
 * <p>
 * .sqlファイルから読み込んだSQLや、直接指定したSQLを実行します。
 * Seasar2のS2DaoやS2JDBCの精神を受け継ぎ、SQLの外部化を第一級の機能として提供します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
public class SqlRunner {
    
    private final SBJdbcManager delegate;
    
    /**
     * SqlRunnerを構築します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     */
    public SqlRunner(SqlSessionFactory sqlSessionFactory) {
        this.delegate = new SBJdbcManager(sqlSessionFactory);
    }
    
    /**
     * SELECT文を実行します。
     *
     * @param <T> 結果の型
     * @param sql SQL文
     * @param params パラメータ
     * @param resultType 結果の型
     * @return 検索結果のリスト
     */
    public <T> List<T> select(String sql, Map<String, Object> params, Class<T> resultType) {
        return delegate.selectBySql(sql, params, resultType).getResultList();
    }
    
    /**
     * SQLファイルからSELECT文を実行します。
     *
     * @param <T> 結果の型
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @param resultType 結果の型
     * @return 検索結果のリスト
     */
    public <T> List<T> selectBySqlFile(String sqlFile, Map<String, Object> params, Class<T> resultType) {
        return delegate.selectBySqlFile(sqlFile, params, resultType).getResultList();
    }
    
    /**
     * INSERT文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 挿入された行数
     */
    public int insert(String sql, Map<String, Object> params) {
        return delegate.insert(sql, params);
    }
    
    /**
     * SQLファイルからINSERT文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 挿入された行数
     */
    public int insertBySqlFile(String sqlFile, Map<String, Object> params) {
        return delegate.insertBySqlFile(sqlFile, params);
    }
    
    /**
     * UPDATE文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 更新された行数
     */
    public int update(String sql, Map<String, Object> params) {
        return delegate.update(sql, params);
    }
    
    /**
     * SQLファイルからUPDATE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 更新された行数
     */
    public int updateBySqlFile(String sqlFile, Map<String, Object> params) {
        return delegate.updateBySqlFile(sqlFile, params);
    }
    
    /**
     * DELETE文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 削除された行数
     */
    public int delete(String sql, Map<String, Object> params) {
        return delegate.delete(sql, params);
    }
    
    /**
     * SQLファイルからDELETE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 削除された行数
     */
    public int deleteBySqlFile(String sqlFile, Map<String, Object> params) {
        return delegate.deleteBySqlFile(sqlFile, params);
    }
}
