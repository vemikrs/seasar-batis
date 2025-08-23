/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import java.io.IOException;
import java.io.Reader;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.exception.SBException;

/**
 * {@link SBJdbcManager}を生成するファクトリクラスです。
 * <p>
 * データソースの設定をXMLファイルから読み込み、{@link SBJdbcManager}のインスタンスを生成します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBJdbcManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBJdbcManagerFactory.class);

    private String configPath;

    /**
     * {@link SBJdbcManagerFactory}を構築します。
     * 
     * @param configPath MyBatis設定ファイルのパス
     */
    public SBJdbcManagerFactory(String configPath) {
        this.configPath = configPath;
    }

    /**
     * {@link SBJdbcManager}のインスタンスを生成します。
     *
     * @return {@link SBJdbcManager}のインスタンス
     */
    public SBJdbcManager create() {
        return new SBJdbcManager(createSqlSessionFactory());
    }

    /**
     * {@link SqlSessionFactory}のインスタンスを生成します。
     *
     * @return {@link SqlSessionFactory}のインスタンス
     */
    private SqlSessionFactory createSqlSessionFactory() {
        try (Reader reader = Resources.getResourceAsReader(configPath)) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            DataSource dataSource = createDataSource(factory.getConfiguration());
            factory.getConfiguration().setEnvironment(
                    new Environment("development",
                            new JdbcTransactionFactory(), dataSource));
            logger.info("MyBatis設定を読み込みました");
            return factory;
        } catch (IOException e) {
            logger.error("MyBatis設定の読み込みに失敗しました: {}", e.getMessage(), e);
            throw new SBException("MyBatis設定の読み込みに失敗しました", e);
        }
    }

    /**
     * {@link DataSource}のインスタンスを生成します。
     *
     * @param configuration MyBatisの{@link Configuration}
     * @return {@link DataSource}のインスタンス
     */
    private DataSource createDataSource(Configuration configuration) {
        final Environment env = configuration.getEnvironment();
        return env.getDataSource();
    }
}