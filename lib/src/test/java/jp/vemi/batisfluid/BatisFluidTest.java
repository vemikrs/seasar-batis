/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jp.vemi.batisfluid.config.OptimisticLockConfig;
import jp.vemi.batisfluid.core.JdbcFlow;
import jp.vemi.batisfluid.core.SqlRunner;

/**
 * BatisFluidのテストクラス。
 *
 * @author H.Kurosawa
 * @version 0.0.2
 */
class BatisFluidTest {
    
    private static SqlSessionFactory sqlSessionFactory;
    
    @BeforeAll
    static void setup() throws Exception {
        sqlSessionFactory = new SqlSessionFactoryBuilder()
            .build(Resources.getResourceAsReader("mybatis-config.xml"));
    }
    
    @Test
    void testBatisFluidCreation() {
        // BatisFluidのインスタンス生成をテスト
        BatisFluid fluid = BatisFluid.of(sqlSessionFactory);
        assertNotNull(fluid, "BatisFluidインスタンスがnullであってはならない");
        assertNotNull(fluid.getSqlSessionFactory(), "SqlSessionFactoryがnullであってはならない");
    }
    
    @Test
    void testBatisFluidWithConfig() {
        // OptimisticLockConfigを指定してBatisFluidを生成
        OptimisticLockConfig config = new OptimisticLockConfig();
        config.setDefaultLockType(OptimisticLockConfig.LockType.VERSION);
        
        BatisFluid fluid = BatisFluid.of(sqlSessionFactory, config);
        assertNotNull(fluid, "BatisFluidインスタンスがnullであってはならない");
        assertNotNull(fluid.getOptimisticLockConfig(), "OptimisticLockConfigがnullであってはならない");
    }
    
    @Test
    void testJdbcFlowCreation() {
        // JdbcFlowの取得をテスト
        BatisFluid fluid = BatisFluid.of(sqlSessionFactory);
        JdbcFlow flow = fluid.jdbcFlow();
        assertNotNull(flow, "JdbcFlowがnullであってはならない");
        assertNotNull(flow.getDelegate(), "JdbcFlowの内部delegateがnullであってはならない");
    }
    
    @Test
    void testSqlRunnerCreation() {
        // SqlRunnerの取得をテスト
        BatisFluid fluid = BatisFluid.of(sqlSessionFactory);
        SqlRunner runner = fluid.sqlRunner();
        assertNotNull(runner, "SqlRunnerがnullであってはならない");
    }
    
    @Test
    void testMultipleJdbcFlowInstances() {
        // 複数のJdbcFlowインスタンスを取得できることをテスト
        BatisFluid fluid = BatisFluid.of(sqlSessionFactory);
        JdbcFlow flow1 = fluid.jdbcFlow();
        JdbcFlow flow2 = fluid.jdbcFlow();
        
        assertNotNull(flow1, "1つ目のJdbcFlowがnullであってはならない");
        assertNotNull(flow2, "2つ目のJdbcFlowがnullであってはならない");
        assertNotSame(flow1, flow2, "複数呼び出しで異なるインスタンスが生成されるべき");
    }
}
