/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.exception.SBTransactionException;

/**
 * トランザクション操作の基本機能を提供するクラスです。
 * <p>
 * MyBatisのSqlSessionを使用した低レベルなトランザクション操作を提供します。
 * セーブポイントの作成や解放、ロールバックなどの機能も提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBTransactionOperation {
    private static final Logger logger = LoggerFactory.getLogger(SBTransactionOperation.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final ThreadLocal<SqlSession> currentSession = new ThreadLocal<>();
    private final ConcurrentMap<String, Savepoint> savepoints = new ConcurrentHashMap<>();

    private boolean isActive = false;

    /**
     * トランザクション操作を管理するクラスを構築します。
     *
     * @param sqlSessionFactory SQLセッションファクトリ
     */
    public SBTransactionOperation(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 新しいトランザクションを開始します。
     *
     * @throws SBTransactionException トランザクションが既に開始されている場合
     */
    public void begin() {
        if (isActive) {
            throw new SBTransactionException("transaction.error.already.started");
        }
        SqlSession session = sqlSessionFactory.openSession(false);
        currentSession.set(session);
        this.isActive = true;
    }

    /**
     * 新しいトランザクションを開始します。
     *
     * @param session SqlSession
     * @throws SBTransactionException トランザクションが既に開始されている場合
     */
    public void begin(SqlSession session) {
        if (isActive) {
            throw new SBTransactionException("transaction.error.already.started");
        }
        currentSession.set(session);
        this.isActive = true;
    }

    /**
     * トランザクションをコミットします。
     */
    public void commit() {
        if (!isActive) {
            throw new SBTransactionException("transaction.error.not.started");
        }
        currentSession.get().commit();
    }

    /**
     * トランザクションをロールバックします。
     */
    public void rollback() {
        if (!isActive) {
            throw new SBTransactionException("transaction.error.not.started");
        }
        currentSession.get().rollback();
    }

    /**
     * トランザクションを終了します。
     */
    public void end() {
        if (!isActive) {
            return;
        }
        try {
            currentSession.get().close();
        } finally {
            isActive = false;
            currentSession.remove();
        }
    }

    /**
     * 現在のSqlSessionを取得します。
     *
     * @return SqlSession
     */
    public SqlSession getCurrentSession() {
        if (!isActive) {
            throw new SBTransactionException("transaction.error.not.started");
        }
        return currentSession.get();
    }

    /**
     * トランザクションがアクティブかどうかを返します。
     *
     * @return トランザクションがアクティブな場合はtrue
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 新しいセーブポイントを作成します。
     * 
     * @return 作成されたセーブポイントのID
     * @throws SBTransactionException トランザクションが開始されていない場合
     */
    public String createSavepoint() {
        if (!isActive()) {
            throw new SBTransactionException("transaction.error.not.started");
        }

        String savepointId = UUID.randomUUID().toString();
        Connection connection = currentSession.get().getConnection();
        Savepoint savepoint;
        try {
            savepoint = connection.setSavepoint();
        } catch (SQLException e) {
            throw new SBTransactionException("transaction.error.savepoint.creation", e);
        }
        savepoints.put(savepointId, savepoint);
        logger.debug("セーブポイントを作成しました: {}", savepointId);
        return savepointId;
    }

    /**
     * 指定されたセーブポイントを解放します。
     * 
     * @param savepointId セーブポイントのID
     * @throws SBTransactionException セーブポイントが見つからない場合
     */
    public void releaseSavepoint(String savepointId) {
        Savepoint savepoint = savepoints.remove(savepointId);
        if (savepoint == null) {
            throw new SBTransactionException("transaction.error.savepoint.not.found", savepointId);
        }

        try {
            currentSession.get().getConnection().releaseSavepoint(savepoint);
            logger.debug("セーブポイントを解放しました: {}", savepointId);
        } catch (SQLException e) {
            throw new SBTransactionException("transaction.error.savepoint.release", e, savepointId);
        }
    }

    /**
     * 指定されたセーブポイントまでロールバックします。
     * 
     * @param savepointId セーブポイントのID
     * @throws SBTransactionException セーブポイントが見つからない場合
     */
    public void rollbackToSavepoint(String savepointId) {
        Savepoint savepoint = savepoints.get(savepointId);
        if (savepoint == null) {
            throw new SBTransactionException("transaction.error.savepoint.not.found", savepointId);
        }

        try {
            currentSession.get().getConnection().rollback(savepoint);
            logger.debug("セーブポイントまでロールバックしました: {}", savepointId);
        } catch (SQLException e) {
            throw new SBTransactionException("transaction.error.savepoint.rollback", e, savepointId);
        }
    }
}