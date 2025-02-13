/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.scripting;

import java.util.Map;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * SBScriptLanguageDriver は、カスタム SQL スクリプト領域を処理するための言語ドライバです。
 * <p>
 * マッパー初期化時に <script> の中身が空の場合は、実行時にパラメータ "_sql" から SQL を取得し、
 * StaticSqlSource 経由でパラメータマッピングを生成します。
 * </p>
 * 
 * @author 
 * @version 1.0.0
 */
public class SBScriptLanguageDriver extends XMLLanguageDriver {

    @Override
    public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
        try {
            // <script> 要素内の SQL テキストを取得
            String sql = script.getStringBody().trim();
            // SQL テキストが空の場合は、実行時に '_sql' パラメータから取得する DeferredSqlSource を返す
            if (sql.isEmpty()) {
                return new SBDeferredSqlSource(configuration);
            }
            return new StaticSqlSource(configuration, sql);
        } catch (Exception e) {
            throw new BuilderException("SQLスクリプトの解析に失敗しました。", e);
        }
    }

    /**
     * SBDeferredSqlSource は、実行時にパラメータ "_sql" から SQL 文を取得し、
     * StaticSqlSource を生成してパラメータマッピングを適用する SqlSource です。
     */
    private static class SBDeferredSqlSource implements SqlSource {
        private final Configuration configuration;

        /**
         * コンストラクタ。
         * 
         * @param configuration MyBatis の Configuration オブジェクト
         */
        public SBDeferredSqlSource(Configuration configuration) {
            this.configuration = configuration;
        }

        /**
         * 実行時のパラメータMapから "_sql" を取得し、StaticSqlSource を生成して BoundSql を返します。
         *
         * @param parameterObject バインドパラメータを含むオブジェクト
         * @return 生成された BoundSql オブジェクト
         * @throws BuilderException パラメータが不正な場合にスローされます
         */
        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            if (parameterObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramMap = (Map<String, Object>) parameterObject;
                Object sqlObj = paramMap.get("_sql");
                if (sqlObj == null || !(sqlObj instanceof String) || ((String) sqlObj).trim().isEmpty()) {
                    throw new BuilderException("Deferred SQL source: '_sql' パラメータが設定されていません。");
                }
                String sql = ((String) sqlObj).trim();
                // StaticSqlSource を利用して、実行時の SQL とパラメータマッピングを生成
                SqlSource staticSqlSource = new StaticSqlSource(configuration, sql);
                return staticSqlSource.getBoundSql(parameterObject);
            } else {
                throw new BuilderException("Deferred SQL source は Map 型のパラメータを必要とします。");
            }
        }
    }
}