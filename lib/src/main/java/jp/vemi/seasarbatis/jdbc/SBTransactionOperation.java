/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * トランザクション操作を管理するクラス。
 * <p>
 * トランザクションの開始、コミット、ロールバック、終了などの操作を提供します。
 * </p>
 *
 * @author VEMI
 * @version 1.0.0
 * @since 2025/02/24
 */
public class SBTransactionOperation {
    private SqlSession session;
    private boolean isActive = false;
    private final SqlSessionFactory sqlSessionFactory;

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
     * @throws IllegalStateException トランザクションが既に開始されている場合
     */
    public void begin() {
        if (isActive) {
            throw new IllegalStateException("トランザクションが既に開始されています");
        }
        this.session = sqlSessionFactory.openSession(false);
        this.isActive = true;
    }

    /**
     * 新しいトランザクションを開始します。
     *
     * @param session SqlSession
     * @throws IllegalStateException トランザクションが既に開始されている場合
     */
    public void begin(SqlSession session) {
        if (isActive) {
            throw new IllegalStateException("トランザクションが既に開始されています");
        }
        this.session = session;
        this.isActive = true;
    }

    /**
     * トランザクションをコミットします。
     */
    public void commit() {
        if (!isActive) {
            throw new IllegalStateException("トランザクションが開始されていません");
        }
        session.commit();
    }

    /**
     * トランザクションをロールバックします。
     */
    public void rollback() {
        if (!isActive) {
            throw new IllegalStateException("トランザクションが開始されていません");
        }
        session.rollback();
    }

    /**
     * トランザクションを終了します。
     */
    public void end() {
        if (!isActive) {
            return;
        }
        try {
            session.close();
        } finally {
            isActive = false;
            session = null;
        }
    }

    /**
     * 現在のSqlSessionを取得します。
     *
     * @return SqlSession
     */
    public SqlSession getCurrentSession() {
        if (!isActive) {
            throw new IllegalStateException("トランザクションが開始されていません");
        }
        return session;
    }

    /**
     * トランザクションがアクティブかどうかを返します。
     *
     * @return トランザクションがアクティブな場合はtrue
     */
    public boolean isActive() {
        return isActive;
    }
}