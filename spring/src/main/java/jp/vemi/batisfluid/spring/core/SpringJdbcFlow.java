/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.spring.core;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.batisfluid.core.JdbcFlow;

/**
 * SpringのトランザクションマネージャーとBatisFluidを統合するクラス。
 * <p>
 * {@link JdbcFlow}を継承し、Springの{@link Transactional}アノテーションを使用して
 * トランザクション管理を行います。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
@Component
public class SpringJdbcFlow {
    
    private final JdbcFlow jdbcFlow;
    
    /**
     * SpringJdbcFlowを構築します。
     *
     * @param sqlSessionFactory SqlSessionFactory
     */
    @Autowired
    public SpringJdbcFlow(SqlSessionFactory sqlSessionFactory) {
        this.jdbcFlow = new JdbcFlow(sqlSessionFactory);
    }
    
    /**
     * 内部のJdbcFlowインスタンスを取得します。
     *
     * @return JdbcFlow
     */
    @Transactional(readOnly = true)
    public JdbcFlow getJdbcFlow() {
        return jdbcFlow;
    }
    
    /**
     * 読み取り専用トランザクションでJdbcFlowを取得します。
     *
     * @return JdbcFlow
     */
    @Transactional(readOnly = true)
    public JdbcFlow forRead() {
        return jdbcFlow;
    }
    
    /**
     * 書き込み可能トランザクションでJdbcFlowを取得します。
     *
     * @return JdbcFlow
     */
    @Transactional
    public JdbcFlow forWrite() {
        return jdbcFlow;
    }
}
