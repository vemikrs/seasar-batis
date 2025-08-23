/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.session;

import java.io.IOException;
import java.io.Reader;
import javax.sql.DataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.exception.SBSQLException;

/**
 * MyBatisのSqlSessionFactoryを生成するファクトリクラスです。
 * <p>
 * MyBatisの設定ファイルを読み込み、SqlSessionFactoryを構築します。
 * データソースの設定や、MyBatisの基本設定を行います。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBSqlSessionFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBSqlSessionFactory.class);

    /**
     * SqlSessionFactoryを生成します。
     * 
     * @param dataSource データソース
     * @return 生成されたSqlSessionFactory
     * @throws SBSQLException MyBatis設定の読み込みに失敗した場合
     */
    public static SqlSessionFactory create(DataSource dataSource) {
        try (Reader reader = Resources.getResourceAsReader("mybatis-config.xml")) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            logger.info("MyBatis設定を読み込みました");
            return factory;
        } catch (IOException e) {
            logger.error("MyBatis設定の読み込みに失敗しました: {}", e.getMessage(), e);
            throw new SBSQLException("MyBatis設定ファイル(mybatis-config.xml)の読み込みに失敗しました", e);
        }
    }
}