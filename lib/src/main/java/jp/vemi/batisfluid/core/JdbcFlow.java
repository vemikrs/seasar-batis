/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.core;

import org.apache.ibatis.session.SqlSessionFactory;

import jp.vemi.batisfluid.config.OptimisticLockConfig;
import jp.vemi.seasarbatis.core.builder.SBSelectBuilder;
import jp.vemi.seasarbatis.core.query.SBSelect;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * fluent APIを提供するJDBC操作クラス。
 * <p>
 * Seasar2のJdbcManagerにインスパイアされた、型安全なデータアクセスAPIを提供します。
 * 現バージョンでは、内部的に{@link SBJdbcManager}に委譲することで互換性を維持しています。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
public class JdbcFlow {
    
    private final SBJdbcManager delegate;
    
    /**
     * JdbcFlowを構築します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     */
    public JdbcFlow(SqlSessionFactory sqlSessionFactory) {
        this.delegate = new SBJdbcManager(sqlSessionFactory);
    }
    
    /**
     * JdbcFlowを構築します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @param optimisticLockConfig 楽観的排他制御設定
     */
    public JdbcFlow(SqlSessionFactory sqlSessionFactory, OptimisticLockConfig optimisticLockConfig) {
        // OptimisticLockConfigは新クラスなので、旧クラスに変換が必要
        jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig oldConfig = 
            new jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig();
        
        // 基本設定をコピー
        oldConfig.setEnabled(optimisticLockConfig.isEnabled());
        oldConfig.setDefaultLockType(convertLockType(optimisticLockConfig.getDefaultLockType()));
        
        // エンティティ固有の設定をコピー（現時点では未実装）
        // TODO: エンティティごとの設定を取得できるAPIが必要
        
        this.delegate = new SBJdbcManager(sqlSessionFactory, oldConfig);
    }
    
    /**
     * LockTypeを旧型に変換します。
     *
     * @param newLockType 新しいLockType
     * @return 旧LockType
     */
    private static jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType convertLockType(
            OptimisticLockConfig.LockType newLockType) {
        switch (newLockType) {
            case VERSION:
                return jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType.VERSION;
            case LAST_MODIFIED:
                return jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType.LAST_MODIFIED;
            case NONE:
            default:
                return jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType.NONE;
        }
    }
    
    /**
     * 内部的に使用するSBJdbcManagerを取得します。
     * <p>
     * v0.0.2での互換性のために提供されています。
     * </p>
     *
     * @return SBJdbcManager
     */
    public SBJdbcManager getDelegate() {
        return delegate;
    }
    
    // 以下、主要なメソッドをデリゲート
    // TODO: 将来的には直接実装に置き換える
    
    /**
     * SELECT操作を開始します。
     * <p>
     * 型安全なクエリビルダーを返します。
     * </p>
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティクラス
     * @return SELECT操作ビルダー
     */
    public <T> SBSelectBuilder<T> from(Class<T> entityClass) {
        return delegate.from(entityClass);
    }
    
    /**
     * エンティティをINSERTします。
     *
     * @param <T> エンティティの型
     * @param entity 挿入するエンティティ
     * @return 挿入後のエンティティ（自動生成キーが設定される）
     */
    public <T> T insert(T entity) {
        return delegate.insert(entity);
    }
    
    /**
     * エンティティをINSERTします。
     *
     * @param <T> エンティティの型
     * @param entity 挿入するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 挿入後のエンティティ（自動生成キーが設定される）
     */
    public <T> T insert(T entity, boolean isIndependentTransaction) {
        return delegate.insert(entity, isIndependentTransaction);
    }
    
    /**
     * エンティティをUPDATEします。
     *
     * @param <T> エンティティの型
     * @param entity 更新するエンティティ
     * @return 更新後のエンティティ
     */
    public <T> T update(T entity) {
        return delegate.update(entity);
    }
    
    /**
     * エンティティをUPDATEします。
     *
     * @param <T> エンティティの型
     * @param entity 更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 更新後のエンティティ
     */
    public <T> T update(T entity, boolean isIndependentTransaction) {
        return delegate.update(entity, isIndependentTransaction);
    }
    
    /**
     * エンティティをDELETEします。
     *
     * @param <T> エンティティの型
     * @param entity 削除するエンティティ
     * @return 削除された件数
     */
    public <T> int delete(T entity) {
        return delegate.delete(entity);
    }
    
    /**
     * エンティティをDELETEします。
     *
     * @param <T> エンティティの型
     * @param entity 削除するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 削除された件数
     */
    public <T> int delete(T entity, boolean isIndependentTransaction) {
        return delegate.delete(entity, isIndependentTransaction);
    }
    
    /**
     * エンティティを主キーで検索します。
     *
     * @param <T> エンティティの型
     * @param entity 検索条件となるエンティティ（主キーが設定されている必要がある）
     * @return SELECT操作ビルダー
     */
    public <T> SBSelect<T> findByPk(T entity) {
        return delegate.findByPk(entity);
    }
    
    /**
     * エンティティをINSERT or UPDATEします。
     * <p>
     * 主キーが存在する場合はUPDATE、存在しない場合はINSERTを実行します。
     * </p>
     *
     * @param <T> エンティティの型
     * @param entity 挿入または更新するエンティティ
     * @return 挿入または更新後のエンティティ
     */
    public <T> T insertOrUpdate(T entity) {
        return delegate.insertOrUpdate(entity);
    }
    
    /**
     * エンティティをINSERT or UPDATEします。
     * <p>
     * 主キーが存在する場合はUPDATE、存在しない場合はINSERTを実行します。
     * </p>
     *
     * @param <T> エンティティの型
     * @param entity 挿入または更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 挿入または更新後のエンティティ
     */
    public <T> T insertOrUpdate(T entity, boolean isIndependentTransaction) {
        return delegate.insertOrUpdate(entity, isIndependentTransaction);
    }
}
