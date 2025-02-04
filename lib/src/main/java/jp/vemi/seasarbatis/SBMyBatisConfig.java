/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.io.Resources;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;

/**
 * MyBatisの設定を管理するクラス。
 * SQLSessionFactoryの初期化と提供を行います。
 * 
 * <p>このクラスは以下の機能を提供します：
 * <ul>
 *   <li>MyBatis設定ファイルの読み込み</li>
 *   <li>DataSourceの設定</li>
 *   <li>SQLSessionFactoryの初期化と管理</li>
 * </ul>
 */
public class SBMyBatisConfig {

    private SqlSessionFactory sqlSessionFactory;

    /**
     * MyBatis設定を初期化します。
     *
     * @param configFilePath MyBatis設定ファイルのパス
     * @param dataSource 使用するデータソース
     * @throws IOException 設定ファイルの読み込みに失敗した場合
     */
    public SBMyBatisConfig(String configFilePath, DataSource dataSource) throws IOException {
        Reader reader = Resources.getResourceAsReader(configFilePath);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        sqlSessionFactory.getConfiguration().setEnvironment(
                new org.apache.ibatis.mapping.Environment("development",
                        new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), dataSource));
    }

    /**
     * 初期化済みのSQLSessionFactoryインスタンスを取得します。
     *
     * @return 設定済みのSQLSessionFactoryインスタンス
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}