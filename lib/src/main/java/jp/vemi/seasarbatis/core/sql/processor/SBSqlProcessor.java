/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.io.IOException;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.core.sql.ProcessedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;

/**
 * SQLを解析するクラスです。
 */
public class SBSqlProcessor {
    private final SqlSessionFactory sqlSessionFactory;

    public SBSqlProcessor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * SQLを解析します。
     * 
     * @param sql
     * @param parameters
     * @return
     */
    public ProcessedSql process(String sql, Map<String, Object> parameters) {
        // 1. SBS2SqlParserでIF条件などの基本的な解析
        ParsedSql parsedSql = SBSqlParser.parse(sql, parameters);

        // 2. MyBatisの機能でバインド変数を処理
        String processedSql = SBMyBatisSqlProcessor.process(
                parsedSql.getSql(),
                sqlSessionFactory,
                parameters);

        // 3. 解析結果と実行用パラメータを保持したオブジェクトを返す
        return ProcessedSql.builder()
                .sql(processedSql)
                .build();
    }

    /**
     * SQLファイルを読み込み、SQLを解析します。
     * 
     * @param filePath
     * @param parameters
     * @return
     * @throws IOException
     */
    public ProcessedSql processFile(String filePath, Map<String, Object> parameters) throws IOException {
        String sql = SBSqlFileLoader.load(filePath);
        return process(sql, parameters);
    }

}