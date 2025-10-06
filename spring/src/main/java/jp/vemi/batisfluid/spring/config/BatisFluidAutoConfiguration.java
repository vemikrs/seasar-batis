/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.spring.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jp.vemi.batisfluid.BatisFluid;
import jp.vemi.batisfluid.core.JdbcFlow;
import jp.vemi.batisfluid.spring.core.SpringJdbcFlow;

/**
 * BatisFluidのSpring Boot自動設定クラス。
 * <p>
 * Spring Bootアプリケーションで{@link BatisFluid}を自動的に構成します。
 * {@link SqlSessionFactory}が利用可能な場合、{@link SpringJdbcFlow}のBeanを自動的に生成します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SqlSessionFactory.class, JdbcFlow.class})
public class BatisFluidAutoConfiguration {
    
    /**
     * SpringJdbcFlowのBeanを生成します。
     * <p>
     * アプリケーション内に{@link SpringJdbcFlow}のBeanが存在しない場合のみ、
     * 自動的に生成されます。
     * </p>
     *
     * @param sqlSessionFactory MyBatisのSqlSessionFactory
     * @return SpringJdbcFlowのインスタンス
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringJdbcFlow springJdbcFlow(SqlSessionFactory sqlSessionFactory) {
        return new SpringJdbcFlow(sqlSessionFactory);
    }
    
    /**
     * BatisFluidのBeanを生成します。
     * <p>
     * アプリケーション内に{@link BatisFluid}のBeanが存在しない場合のみ、
     * 自動的に生成されます。
     * </p>
     *
     * @param sqlSessionFactory MyBatisのSqlSessionFactory
     * @return BatisFluidのインスタンス
     */
    @Bean
    @ConditionalOnMissingBean
    public BatisFluid batisFluid(SqlSessionFactory sqlSessionFactory) {
        return BatisFluid.of(sqlSessionFactory);
    }
}
