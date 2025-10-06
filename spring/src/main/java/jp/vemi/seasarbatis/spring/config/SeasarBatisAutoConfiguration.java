/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.spring.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import jp.vemi.seasarbatis.spring.manager.SpringJdbcManager;

/**
 * SeasarBatisのSpring Boot自動設定クラス。
 * <p>
 * <strong>非推奨：</strong> このクラスはv0.0.2で非推奨となりました。
 * 代わりに{@code jp.vemi.batisfluid.spring.config.BatisFluidAutoConfiguration}を使用してください。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @deprecated v0.0.2以降は{@code jp.vemi.batisfluid.spring.config.BatisFluidAutoConfiguration}を使用してください。
 *             このクラスはv0.0.3以降で削除される予定です。
 */
@Deprecated(since = "0.0.2")
@Configuration(proxyBeanMethods = false)
public class SeasarBatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringJdbcManager springJdbcManager(SqlSessionFactory sqlSessionFactory) {
        return new SpringJdbcManager(sqlSessionFactory);
    }
}
