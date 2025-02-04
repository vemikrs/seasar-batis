/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import java.io.IOException;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * MyBatisのマッパーインターフェースを管理するジェネリッククラス。
 * 指定されたマッパーインターフェースのインスタンスを生成し、
 * SQLセッションの自動クローズを行います。
 *
 * @param <T> マッパーインターフェースの型
 */
public class SBMyBatisMapper<T> {
    private final SqlSessionFactory sqlSessionFactory;
    private final Class<T> mapperClass;

    /**
     * マッパークラスを初期化します。
     *
     * @param sqlSessionFactory MyBatisのSQLセッションファクトリー
     * @param mapperClass       使用するマッパーインターフェースのClass
     */
    public SBMyBatisMapper(SqlSessionFactory sqlSessionFactory, Class<T> mapperClass) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.mapperClass = mapperClass;
    }

    /**
     * マッパーインターフェースのインスタンスを取得します。
     * セッションは自動的にクローズされます。
     *
     * @return マッパーインターフェースのインスタンス
     */
    public T getMapper() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(mapperClass);
        }
    }

    public String executeQueryFromFile(String sqlFilePath, Map<String, Object> parameters) throws IOException {
        String sql = SBQueryBuilder.loadSQLFromFile(sqlFilePath);
        sql = SBQueryBuilder.processSQL(sql, sqlSessionFactory, parameters);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectOne("dynamicSQL", parameters);
        }
    }
}