/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLクエリを実行するための実行クラス。
 * MyBatisのSQLSessionを使用してデータベース操作を提供します。
 */
public class SBQueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SBQueryExecutor.class);
    private final SqlSessionFactory sqlSessionFactory;

    public SBQueryExecutor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * SQLファイルから実行
     */
    public <T> T executeFile(String sqlFile, Map<String, Object> parameters, String commandType) {
        try {
            String sql = SBQueryBuilder.loadSQLFromFile(sqlFile);
            return executeInternal(sql, parameters, commandType);
        } catch (IOException e) {
            logger.error("SQLファイル読み込みエラー: {}", e.getMessage(), e);
            throw new RuntimeException("SQLファイルの読み込みに失敗しました: " + sqlFile, e);
        }
    }

    /**
     * SQL文字列から直接実行
     */
    public <T> T execute(String sql, Map<String, Object> parameters, String commandType) {
        return executeInternal(sql, parameters, commandType);
    }

    @SuppressWarnings("unchecked")
    private <T> T executeInternal(String sql, Map<String, Object> parameters, String commandType) {
        String processedSql = SBQueryBuilder.processSQL(sql, parameters);
        logger.debug("Executing {} SQL: {}", commandType, processedSql);

        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            // PreparedStatementを使用するようにマッピング
            Map<String, Object> params = new HashMap<>(parameters);
            params.put("_sql", processedSql);
            
            Object result = null;
            String statement = "prepared" + commandType;  // preparedSELECT など

            switch (commandType) {
                case "SELECT":
                    result = session.selectList(statement, params);
                    break;
                case "INSERT":
                    result = session.insert(statement, params);
                    break;
                case "UPDATE":
                    result = session.update(statement, params);
                    break;
                case "DELETE":
                    result = session.delete(statement, params);
                    break;
                default:
                    throw new IllegalArgumentException("不正なSQLコマンドタイプ: " + commandType);
            }

            if (!"SELECT".equals(commandType)) {
                session.commit();
                logger.info("{} affected {} rows", commandType, result);
            }

            return (T) result;
        } catch (Exception e) {
            logger.error("SQL実行エラー: {}", e.getMessage(), e);
            try (SqlSession session = sqlSessionFactory.openSession()) {
                session.rollback();
                logger.warn("トランザクションをロールバックしました");
            }
            throw new RuntimeException("SQL実行中にエラーが発生しました", e);
        }
    }

    /**
     * SQLを実行し、結果を取得します。
     * @param <T> 戻り値の型
     * @param sql SQL文
     * @param parameters バインドパラメータ
     * @return  実行結果
     */
    public <T> List<T> select(String sql, Map<String, Object> parameters) {
        return execute(sql, parameters, "SELECT");
    }

    /**
     * SQLを実行し、結果を取得します。
     * @param sql SQL文
     * @param parameters バインドパラメータ
     * @return 実行結果
     */
    public int update(String sql, Map<String, Object> parameters) {
        return execute(sql, parameters, "UPDATE");
    }
}
