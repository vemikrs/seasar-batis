/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.util.Map;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * MyBatisの機能を使用してSQLを処理します
 */
public class SBMyBatisSqlProcessor {
    /**
     * MyBatisの機能を使用してSQLを処理します
     */
    public static String process(String sql, SqlSessionFactory sqlSessionFactory, Map<String, Object> parameters) {
        SqlSource sqlSource = new DynamicSqlSource(
                sqlSessionFactory.getConfiguration(),
                new TextSqlNode(sql));
        MappedStatement ms = new MappedStatement.Builder(
                sqlSessionFactory.getConfiguration(),
                "dynamicSQL",
                sqlSource,
                SqlCommandType.SELECT).build();

        BoundSql boundSql = ms.getBoundSql(parameters);
        return boundSql.getSql();
    }

}
