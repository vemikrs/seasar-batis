/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.spring.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import jp.vemi.seasarbatis.spring.manager.SpringJdbcManager;

public class SeasarBatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringJdbcManager springJdbcManager(SqlSessionFactory sqlSessionFactory) {
        return new SpringJdbcManager(sqlSessionFactory);
    }
}
