/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid;

import org.apache.ibatis.session.SqlSessionFactory;

import jp.vemi.batisfluid.core.JdbcFlow;
import jp.vemi.batisfluid.core.SqlRunner;
import jp.vemi.batisfluid.config.OptimisticLockConfig;

/**
 * BatisFluidのエントリーポイントクラス。
 * <p>
 * MyBatisの{@link SqlSessionFactory}をラップし、fluent APIとexternalized SQLを提供します。
 * Seasar2のS2TIGERプロジェクトにインスパイアされた、型安全でモダンなデータアクセス層を構築できます。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
public class BatisFluid {
    
    private final SqlSessionFactory sqlSessionFactory;
    private final OptimisticLockConfig optimisticLockConfig;
    
    /**
     * プライベートコンストラクタ。
     * {@link #of(SqlSessionFactory)}を使用してインスタンスを生成してください。
     *
     * @param sqlSessionFactory SqlSessionFactory
     */
    private BatisFluid(SqlSessionFactory sqlSessionFactory) {
        this(sqlSessionFactory, new OptimisticLockConfig());
    }
    
    /**
     * プライベートコンストラクタ。
     * {@link #of(SqlSessionFactory, OptimisticLockConfig)}を使用してインスタンスを生成してください。
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @param optimisticLockConfig 楽観的排他制御設定
     */
    private BatisFluid(SqlSessionFactory sqlSessionFactory, OptimisticLockConfig optimisticLockConfig) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.optimisticLockConfig = optimisticLockConfig;
    }
    
    /**
     * BatisFluidインスタンスを生成します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @return BatisFluidインスタンス
     */
    public static BatisFluid of(SqlSessionFactory sqlSessionFactory) {
        return new BatisFluid(sqlSessionFactory);
    }
    
    /**
     * BatisFluidインスタンスを生成します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     * @param optimisticLockConfig 楽観的排他制御設定
     * @return BatisFluidインスタンス
     */
    public static BatisFluid of(SqlSessionFactory sqlSessionFactory, OptimisticLockConfig optimisticLockConfig) {
        return new BatisFluid(sqlSessionFactory, optimisticLockConfig);
    }
    
    /**
     * fluent APIを提供するJdbcFlowインスタンスを取得します。
     *
     * @return JdbcFlowインスタンス
     */
    public JdbcFlow jdbcFlow() {
        return new JdbcFlow(sqlSessionFactory, optimisticLockConfig);
    }
    
    /**
     * externalized SQLを実行するSqlRunnerインスタンスを取得します。
     *
     * @return SqlRunnerインスタンス
     */
    public SqlRunner sqlRunner() {
        return new SqlRunner(sqlSessionFactory);
    }
    
    /**
     * SqlSessionFactoryを取得します。
     *
     * @return SqlSessionFactory
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
    
    /**
     * 楽観的排他制御設定を取得します。
     *
     * @return 楽観的排他制御設定
     */
    public OptimisticLockConfig getOptimisticLockConfig() {
        return optimisticLockConfig;
    }
}
