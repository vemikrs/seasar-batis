/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.transaction;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * トランザクション操作のコンテキストを管理するクラスです。
 * <p>
 * ThreadLocalを使用して、現在のスレッドのトランザクション操作を追跡します。
 * 独立トランザクションが実行される際に、一時的に異なるトランザクション操作を
 * 使用できるようにします。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBTransactionContext {
    
    private static final ThreadLocal<SBTransactionOperation> currentOperation = new ThreadLocal<>();
    
    /**
     * 現在のトランザクション操作を設定します。
     * 
     * @param operation トランザクション操作
     */
    public static void setCurrentOperation(SBTransactionOperation operation) {
        currentOperation.set(operation);
    }
    
    /**
     * 現在のトランザクション操作を取得します。
     * 
     * @return 現在のトランザクション操作、設定されていない場合はnull
     */
    public static SBTransactionOperation getCurrentOperation() {
        return currentOperation.get();
    }
    
    /**
     * 現在のトランザクション操作をクリアします。
     */
    public static void clearCurrentOperation() {
        currentOperation.remove();
    }
    
    /**
     * 指定されたトランザクション操作を一時的に設定して処理を実行します。
     * 
     * @param <T> 戻り値の型
     * @param operation 使用するトランザクション操作
     * @param action 実行する処理
     * @return 処理の結果
     */
    public static <T> T withOperation(SBTransactionOperation operation, java.util.concurrent.Callable<T> action) {
        SBTransactionOperation previousOperation = getCurrentOperation();
        try {
            setCurrentOperation(operation);
            return action.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(SBMessageManager.getInstance().getMessage("transaction.error.processing"), e);
        } finally {
            if (previousOperation != null) {
                setCurrentOperation(previousOperation);
            } else {
                clearCurrentOperation();
            }
        }
    }
}