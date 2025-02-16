/*
 * Copyright(c) 2025 VEMI All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.executor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.sql.CommandType;
import jp.vemi.seasarbatis.core.sql.ProcessedSql;
import jp.vemi.seasarbatis.core.sql.loader.SBSqlFileLoader;
import jp.vemi.seasarbatis.core.sql.processor.SBSqlProcessor;
import jp.vemi.seasarbatis.core.util.SBTypeConverterUtils;

/**
 * SQLクエリを実行するための実行クラスです。
 * <p>
 * 本クラスは、MyBatisのSqlSessionを使用してデータベース操作を提供します。
 * 共通のセッション処理を内部メソッドに切り出すことで、冗長なコードの分散を防いでいます。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 */
public class SBQueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SBQueryExecutor.class);
    private final SqlSessionFactory sqlSessionFactory;
    private final SBSqlProcessor sqlProcessor;

    public SBQueryExecutor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlProcessor = new SBSqlProcessor(sqlSessionFactory);
    }

    /**
     * SQLファイルから実行します。（SqlSession指定）
     * 
     * @param <T>         戻り値の型
     * @param sqlFile     SQLファイルパス
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @param session     SQLセッション
     * @return 実行結果
     */
    public <T> T executeFile(String sqlFile, Map<String, Object> parameters, CommandType commandType,
            SqlSession session) {
        try {
            String sql = SBSqlFileLoader.load(sqlFile);
            return executeSqlCommand(sql, parameters, commandType, session);
        } catch (IOException e) {
            logger.error("SQLファイル読み込みエラー: {}", e.getMessage(), e);
            throw new RuntimeException("SQLファイルの読み込みに失敗しました: " + sqlFile, e);
        }
    }

    /**
     * SQLファイルから実行します。
     * 
     * @param <T>         戻り値の型
     * @param sqlFile     SQLファイルパス
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @return 実行結果
     */
    public <T> T executeFile(String sqlFile, Map<String, Object> parameters, CommandType commandType) {
        try {
            String sql = SBSqlFileLoader.load(sqlFile);
            return executeSqlCommand(sql, parameters, commandType);
        } catch (IOException e) {
            logger.error("SQLファイル読み込みエラー: {}", e.getMessage(), e);
            throw new RuntimeException("SQLファイルの読み込みに失敗しました: " + sqlFile, e);
        }
    }

    /**
     * SQL文字列から直接実行します。（SqlSession指定）
     *
     * @param <T>         戻り値の型
     * @param sql         SQL文
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @param session     SQLセッション
     * @return 実行結果
     */
    public <T> T execute(String sql, Map<String, Object> parameters, CommandType commandType, SqlSession session) {
        return executeSqlCommand(sql, parameters, commandType, session);
    }

    /**
     * SQL文字列から直接実行します。
     * 
     * @param <T>         戻り値の型
     * @param sql         SQL文
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @return 実行結果
     */
    public <T> T execute(String sql, Map<String, Object> parameters, CommandType commandType) {
        return executeWithSession(null, commandType, session -> execute(sql, parameters, commandType, session));
    }

    /**
     * SELECT文を実行し、型安全な結果を返します。
     *
     * @param <T>        戻り値の要素型
     * @param sql        SQL文
     * @param parameters バインドパラメータ
     * @param resultType マッピング先のクラス
     * @param session    SQLセッション
     * @return マッピングされた結果のリスト
     */
    public <T> List<T> executeSelect(String sql, Map<String, Object> parameters, Class<T> resultType,
            SqlSession session) {
        ProcessedSql processedSql = sqlProcessor.process(sql, parameters);
        logger.debug("Executing SELECT SQL: {}", processedSql);

        List<Map<String, Object>> rawResults = session.selectList("jp.vemi.seasarbatis.preparedSELECT",
                Collections.singletonMap("_sql", processedSql.getSql()));
        Configuration configuration = session.getConfiguration();
        return rawResults.stream()
                .map(row -> SBTypeConverterUtils.convertRowToEntity(row, resultType, configuration))
                .collect(Collectors.toList());
    }

    /**
     * SELECT文を実行し、型安全な結果を返します。
     *
     * @param <T>        戻り値の要素型
     * @param sql        SQL文
     * @param parameters バインドパラメータ
     * @param resultType マッピング先のクラス
     * @return マッピングされた結果のリスト
     */
    public <T> List<T> executeSelect(String sql, Map<String, Object> parameters, Class<T> resultType) {
        return executeWithSession(null, CommandType.SELECT,
                session -> executeSelect(sql, parameters, resultType, session));
    }

    /**
     * 非SELECT文を実行します。
     * 
     * @param sql         SQL文
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ（INSERT/UPDATE/DELETE）
     * @return 実行結果（更新件数など）
     */
    public int executeUpdate(String sql, Map<String, Object> parameters, CommandType commandType) {
        if (CommandType.SELECT.equals(commandType)) {
            throw new IllegalArgumentException("SELECT文にはexecuteSelectを使用してください");
        }
        return executeSqlCommand(sql, parameters, commandType);
    }

    /**
     * SQL文字列を処理し、セッションをオープンしてSQLを実行します。
     * <p>
     * 共通のセッション処理をまとめ、各種SQL実行メソッドの冗長性を低減します。
     * </p>
     *
     * @param <T>         戻り値の型
     * @param sql         SQL文
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @param session     SQLセッション
     * @return 実行結果
     */
    @SuppressWarnings("unchecked")
    private <T> T executeSqlCommand(String sql, Map<String, Object> parameters, CommandType commandType,
            SqlSession session) {
        ProcessedSql processedSql = sqlProcessor.process(sql, parameters);
        logger.debug("Executing {} SQL: {}", commandType, processedSql);

        String statement = "jp.vemi.seasarbatis.prepared" + commandType;
        if (CommandType.SELECT.equals(commandType)) {
            return (T) session.selectList("jp.vemi.seasarbatis.preparedSELECT",
                    Collections.singletonMap("_sql", processedSql.getSql()));
        } else {
            return (T) executeStatement(session, statement,
                    Collections.singletonMap("_sql", processedSql.getSql()), commandType);
        }
    }

    /**
     * SQL文字列を処理し、セッションをオープンしてSQLを実行します。
     * <p>
     * 共通のセッション処理をまとめ、各種SQL実行メソッドの冗長性を低減します。
     * </p>
     *
     * @param <T>         戻り値の型
     * @param sql         SQL文
     * @param parameters  バインドパラメータ
     * @param commandType SQLコマンドタイプ
     * @return 実行結果
     */
    private <T> T executeSqlCommand(String sql, Map<String, Object> parameters, CommandType commandType) {
        return executeWithSession(null, commandType,
                session -> executeSqlCommand(sql, parameters, commandType, session));
    }

    /**
     * SQLセッションをオープンして共通の前処理・後処理を行いながらSQLを実行します。
     * <p>
     * セッションのコミット、ロールバック処理もこのメソッド内で行います。
     * </p>
     *
     * @param <T>          戻り値の型
     * @param processedSql 解析済みSQLオブジェクト
     * @param commandType  SQLコマンドタイプ
     * @param action       セッション上で実行する処理を表すラムダ
     * @return 実行結果
     */
    private <T> T executeWithSession(ProcessedSql processedSql, CommandType commandType,
            Function<SqlSession, T> action) {
        return executeWithSession(processedSql, commandType, action, null);
    }

    /**
     * SQLセッションをオープンして共通の前処理・後処理を行いながらSQLを実行します。
     * <p>
     * セッションのコミット、ロールバック処理もこのメソッド内で行います。
     * </p>
     *
     * @param <T>             戻り値の型
     * @param processedSql    解析済みSQLオブジェクト
     * @param commandType     SQLコマンドタイプ
     * @param action          セッション上で実行する処理を表すラムダ
     * @param externalSession 外部セッション
     * @return 実行結果
     */
    private <T> T executeWithSession(ProcessedSql processedSql, CommandType commandType,
            Function<SqlSession, T> action, SqlSession externalSession) {
        SqlSession session = externalSession;
        boolean external = false;

        if (externalSession == null) {
            session = sqlSessionFactory.openSession(false);
        } else {
            external = true;
        }

        try {
            T result = action.apply(session);
            /*
             * if (!CommandType.SELECT.equals(commandType)) {
             * session.commit();
             * logger.info("{} affected {} rows", commandType, result);
             * }
             */
            return result;
        } catch (Exception e) {
            logger.error("SQL実行エラー: {}", e.getMessage(), e);
            if (session != null && !external) {
                session.rollback();
                logger.warn("トランザクションをロールバックしました");
            }
            throw new RuntimeException("SQL実行中にエラーが発生しました", e);
        } finally {
            if (session != null && !external) {
                session.close();
            }
        }
    }

    /**
     * SQLセッション上で指定のステートメントを実行します。
     * 
     * @param session     SQLセッション
     * @param statement   実行するステートメントID
     * @param params      バインドパラメータ
     * @param commandType SQLコマンドタイプ（INSERT/UPDATE/DELETE）
     * @return 実行結果
     */
    private Object executeStatement(SqlSession session, String statement, Map<String, Object> params,
            CommandType commandType) {
        switch (commandType) {
            case INSERT:
                return session.insert(statement, params);
            case UPDATE:
                return session.update(statement, params);
            case DELETE:
                return session.delete(statement, params);
            default:
                throw new IllegalArgumentException("不正なSQLコマンドタイプ: " + commandType);
        }
    }
}