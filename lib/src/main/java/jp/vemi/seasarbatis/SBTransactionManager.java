/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * MyBatisのトランザクション管理を行うクラス。
 * セッションの開始、コミット、ロールバックを自動的に処理します。
 */
public class SBTransactionManager {
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * トランザクションマネージャーを初期化します。
     *
     * @param sqlSessionFactory SqlSessionFactoryインスタンス
     */
    public SBTransactionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * トランザクション内でコールバック処理を実行します。
     * 処理が正常に完了した場合は自動的にコミットし、
     * 例外が発生した場合は自動的にロールバックします。
     *
     * @param callback 実行するトランザクション処理
     * @throws Exception 処理中に発生した例外
     */
    public void executeInTransaction(TransactionCallback callback) throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            try {
                callback.doInTransaction(session);
                session.commit();
            } catch (Exception e) {
                session.rollback();
                throw e;
            }
        }
    }

    /**
     * トランザクション内で実行される処理を定義するインターフェース。
     */
    public interface TransactionCallback {
        /**
         * トランザクション内で実行される処理を実装します。
         *
         * @param session 現在のSQLセッション
         * @throws Exception 処理中に発生した例外
         */
        void doInTransaction(SqlSession session) throws Exception;
    }
}