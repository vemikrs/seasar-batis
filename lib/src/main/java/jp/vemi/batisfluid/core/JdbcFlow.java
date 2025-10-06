/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.core;

import org.apache.ibatis.session.SqlSessionFactory;

import jp.vemi.batisfluid.config.OptimisticLockConfig;
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
        // 設定をコピー（後で実装）
        this.delegate = new SBJdbcManager(sqlSessionFactory, oldConfig);
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
}
