/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.io.IOException;
import java.util.Map;

import org.apache.ibatis.session.Configuration;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.core.sql.ProcessedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;

/**
 * SQLを解析・処理するプロセッサークラスです。
 * <p>
 * MyBatisの機能を利用してSQLの解析と変数のバインド処理を行います。
 * IF条件の評価やバインド変数の置換などの基本的なSQL処理機能を提供します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBSqlProcessor {
    private final Configuration configuration;

    /**
     * SBSqlProcessorを構築します。
     *
     * @param configuration MyBatisの設定オブジェクト
     */
    public SBSqlProcessor(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * SQLを解析し、実行可能な形式に処理します。
     * 
     * @param sql        SQL文字列
     * @param parameters バインドパラメータ
     * @return 処理済みSQL情報
     */
    public ProcessedSql process(String sql, Map<String, Object> parameters) {
        ParsedSql parsedSql = SBSqlParser.parse(sql, parameters);

        String processedSql = SBMyBatisSqlProcessor.process(
                parsedSql.getSql(),
                configuration,
                parameters);

        return ProcessedSql.builder()
                .sql(processedSql)
                .build();
    }

    /**
     * SQLファイルを読み込み、SQLを解析します。
     * 
     * @param filePath   SQLファイルのパス
     * @param parameters バインドパラメータ
     * @return 処理済みSQL情報
     * @throws IOException SQLファイルの読み込みに失敗した場合
     */
    public ProcessedSql processFile(String filePath, Map<String, Object> parameters) throws IOException {
        String sql = SBSqlFileLoader.load(filePath);
        return process(sql, parameters);
    }
}