package jp.vemi.seasarbatis.core.transaction;

import java.util.concurrent.Callable;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.Environment;

import jp.vemi.seasarbatis.exception.SBTransactionException;

/**
 * トランザクション管理の高レベルAPIを提供するクラスです。
 * <p>
 * トランザクションの境界制御や伝播制御を行い、アプリケーションに
 * より使いやすいトランザクション管理機能を提供します。
 * SBTransactionOperationを内部で使用し、より高度な制御を実現します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBTransactionManager {
    private final SqlSessionFactory sqlSessionFactory;
    private final SBTransactionOperation txOperation;

    // トランザクション伝播タイプの定義
    public enum PropagationType {
        REQUIRED,    // 既存のトランザクションを使用、なければ新規作成
        REQUIRES_NEW,// 常に新規トランザクションを作成
        NESTED      // ネストされたトランザクションを作成
    }

    public SBTransactionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        // DataSource をスレッドローカル対応のラッパに差し替え（子セッションのcommit/close抑制のため）
        try {
            Configuration conf = this.sqlSessionFactory.getConfiguration();
            Environment env = conf.getEnvironment();
            if (env != null && !(env.getDataSource() instanceof SBThreadLocalDataSource)) {
                Environment wrapped = new Environment(env.getId(), env.getTransactionFactory(),
                        new SBThreadLocalDataSource(env.getDataSource()));
                conf.setEnvironment(wrapped);
            }
        } catch (Exception ignore) {
            // 失敗しても致命的ではない（テスト環境等での安全策）
        }
        this.txOperation = new SBTransactionOperation(sqlSessionFactory);
    }

    /**
     * 指定された伝播タイプでトランザクションを実行します。
     *
     * @param <T> 戻り値の型
     * @param propagationType トランザクション伝播タイプ
     * @param operation 実行する操作
     * @return 操作の実行結果
     */
    public <T> T execute(PropagationType propagationType, Callable<T> operation) {
        switch (propagationType) {
            case REQUIRED:
                return executeRequired(operation);
            case REQUIRES_NEW:
                return executeRequiresNew(operation);
            case NESTED:
                return executeNested(operation);
            default:
                throw new SBTransactionException("transaction.error.unsupported.propagation", propagationType);
        }
    }

    public <T> T executeWithTransaction(boolean isIndependentTransaction, Callable<T> operation) {
        if (isIndependentTransaction) {
            // 独立トランザクションの場合は新しいTransactionOperationインスタンスを作成
            SBTransactionOperation independentTxOperation = new SBTransactionOperation(sqlSessionFactory);
            SqlSession session = sqlSessionFactory.openSession(false);
            // 親の BOUND を参照せず新規接続を確保・バインド
            independentTxOperation.beginIndependent(session);
            try {
                // 独立トランザクションのコンテキストを設定して実行
                T result = SBTransactionContext.withOperation(independentTxOperation, operation);
                independentTxOperation.commit();
                return result;
            } catch (Exception e) {
                independentTxOperation.rollback();
                throw new SBTransactionException("transaction.error.execution", e);
            } finally {
                independentTxOperation.end();
            }
        }

        // 現在のトランザクション操作（独立TX等）を優先し、未設定ならデフォルト操作を使用
        SBTransactionOperation op = SBTransactionContext.getCurrentOperation();
        if (op == null) {
            op = txOperation;
        }

        boolean isNewTransaction = !op.isActive();
        if (isNewTransaction) {
            op.begin(sqlSessionFactory.openSession(false));
        }

        try {
            // コンテキストを安全に設定して実行
            T result = SBTransactionContext.withOperation(op, operation);
            if (isNewTransaction) {
                op.commit();
            }
            return result;
        } catch (Exception e) {
            if (isNewTransaction) {
                op.rollback();
            }
            throw new SBTransactionException("transaction.error.execution", e);
        } finally {
            if (isNewTransaction) {
                op.end();
            }
        }
    }

    /**
     * トランザクションが存在する場合はそれを使用し、
     * 存在しない場合は新規トランザクションを作成します。
     *
     * @param <T> 戻り値の型
     * @param operation 実行する操作
     * @return 操作の実行結果
     */
    private <T> T executeRequired(Callable<T> operation) {
        return executeWithTransaction(false, operation);
    }

    /**
     * 常に新規トランザクションを作成して実行します。
     *
     * @param <T> 戻り値の型
     * @param operation 実行する操作
     * @return 操作の実行結果
     */
    private <T> T executeRequiresNew(Callable<T> operation) {
        return executeWithTransaction(true, operation);
    }

    /**
     * ネストされたトランザクションを作成して実行します。
     * 現在のトランザクションがある場合はそのスコープ内で
     * セーブポイントを作成します。
     *
     * @param <T> 戻り値の型
     * @param operation 実行する操作
     * @return 操作の実行結果
     */
    private <T> T executeNested(Callable<T> operation) {
        if (!txOperation.isActive()) {
            return executeRequired(operation);
        }

        String savepoint = txOperation.createSavepoint();
        try {
            T result = operation.call();
            txOperation.releaseSavepoint(savepoint);
            return result;
        } catch (Exception e) {
            txOperation.rollbackToSavepoint(savepoint);
            throw new SBTransactionException("transaction.error.nested.execution", e);
        }
    }

    /**
     * トランザクションが活性状態かどうかを返します。
     *
     * @return トランザクションが活性状態の場合true
     */
    public boolean isActive() {
        SBTransactionOperation current = SBTransactionContext.getCurrentOperation();
        SBTransactionOperation op = (current != null) ? current : txOperation;
        return op.isActive();
    }

    /**
     * 現在のトランザクションをコミットします。
     */
    public void commit() {
        SBTransactionOperation current = SBTransactionContext.getCurrentOperation();
        SBTransactionOperation op = (current != null) ? current : txOperation;
        op.commit();
    }

    /**
     * 現在のトランザクションをロールバックします。
     */
    public void rollback() {
        SBTransactionOperation current = SBTransactionContext.getCurrentOperation();
        SBTransactionOperation op = (current != null) ? current : txOperation;
        op.rollback();
    }

    /**
     * 内部で使用されるトランザクション操作を取得します。
     * 
     * @return トランザクション操作
     */
    public SBTransactionOperation getTransactionOperation() {
        SBTransactionOperation current = SBTransactionContext.getCurrentOperation();
        return current != null ? current : txOperation;
    }
}