/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import java.io.IOException;
import java.io.Reader;
import javax.sql.DataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBSqlSessionFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBSqlSessionFactory.class);

    public static SqlSessionFactory create(DataSource dataSource) {
        try (Reader reader = Resources.getResourceAsReader("mybatis-config.xml")) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            logger.info("MyBatis設定を読み込みました");
            return factory;
        } catch (IOException e) {
            logger.error("MyBatis設定の読み込みに失敗しました: {}", e.getMessage(), e);
            throw new RuntimeException("MyBatis設定の読み込みに失敗しました", e);
        }
    }
}