/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSession;
import java.util.List;
import java.util.Map;

/**
 * SQLクエリを実行するための実行クラス。
 * MyBatisのSQLSessionを使用してCRUD操作を提供します。
 * 
 * <p>このクラスは以下の機能を提供します：
 * <ul>
 *   <li>SELECT文の実行（リスト形式での結果取得）</li>
 *   <li>INSERT文の実行（影響行数の取得）</li>
 *   <li>UPDATE文の実行（影響行数の取得）</li>
 *   <li>DELETE文の実行（影響行数の取得）</li>
 * </ul>
 */
public class SBQueryExecutor {
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * QueryExecutorを初期化します。
     *
     * @param sqlSessionFactory MyBatisのSQLSessionFactory
     */
    public SBQueryExecutor(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * SELECT文を実行し、結果をリストで取得します。
     *
     * @param <T> 戻り値の要素型
     * @param sql 実行するSQL文
     * @param parameters バインドパラメータ
     * @return 検索結果のリスト
     */
    public <T> List<T> executeSelect(String sql, Map<String, Object> parameters) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList("dynamicSQL", parameters);
        }
    }

    /**
     * UPDATE文を実行します。
     * トランザクション制御（コミット/ロールバック）を自動的に行います。
     *
     * @param sql 実行するSQL文
     * @param parameters バインドパラメータ
     * @return 更新された行数
     * @throws RuntimeException SQL実行時にエラーが発生した場合
     */
    public int executeUpdate(String sql, Map<String, Object> parameters) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            int result = session.update("dynamicSQL", parameters);
            session.commit();
            return result;
        } catch (Exception e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                session.rollback();
            }
            throw e;
        }
    }

    /**
     * INSERT文を実行します。
     * トランザクション制御（コミット/ロールバック）を自動的に行います。
     *
     * @param sql 実行するSQL文
     * @param parameters バインドパラメータ
     * @return 挿入された行数
     * @throws RuntimeException SQL実行時にエラーが発生した場合
     */
    public int executeInsert(String sql, Map<String, Object> parameters) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            int result = session.insert("dynamicSQL", parameters);
            session.commit();
            return result;
        } catch (Exception e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                session.rollback();
            }
            throw e;
        }
    }

    /**
     * DELETE文を実行します。
     * トランザクション制御（コミット/ロールバック）を自動的に行います。
     *
     * @param sql 実行するSQL文
     * @param parameters バインドパラメータ
     * @return 削除された行数
     * @throws RuntimeException SQL実行時にエラーが発生した場合
     */
    public int executeDelete(String sql, Map<String, Object> parameters) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            int result = session.delete("dynamicSQL", parameters);
            session.commit();
            return result;
        } catch (Exception e) {
            try (SqlSession session = sqlSessionFactory.openSession()) {
                session.rollback();
            }
            throw e;
        }
    }
}
