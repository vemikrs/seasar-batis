/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.example;

import java.util.Locale;

import jp.vemi.seasarbatis.core.i18n.SBLocaleConfig;
import jp.vemi.seasarbatis.core.i18n.SBMessageManager;
import jp.vemi.seasarbatis.exception.SBTransactionException;

/**
 * SeasarBatisのi18n機能の使用例を示すサンプルクラスです。
 * <p>
 * 国際化メッセージの取得方法やロケール設定の変更方法を示します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/08/23
 */
public class SBI18nExample {
    
    /**
     * i18n機能の使用例を実行します。
     * 
     * @param args コマンドライン引数
     */
    public static void main(String[] args) {
        
        // 1. メッセージマネージャーの取得
        SBMessageManager messageManager = SBMessageManager.getInstance();
        
        // 2. 英語でのメッセージ取得
        System.out.println("=== English Messages ===");
        messageManager.setLocale(Locale.ENGLISH);
        System.out.println("Transaction error: " + messageManager.getMessage("transaction.error.execution"));
        System.out.println("Savepoint not found: " + messageManager.getMessage("transaction.error.savepoint.not.found", "SP001"));
        
        // 3. 日本語でのメッセージ取得
        System.out.println("\n=== Japanese Messages ===");
        messageManager.setLocale(Locale.JAPANESE);
        System.out.println("トランザクションエラー: " + messageManager.getMessage("transaction.error.execution"));
        System.out.println("セーブポイントエラー: " + messageManager.getMessage("transaction.error.savepoint.not.found", "SP001"));
        
        // 4. ロケール設定クラスの使用
        System.out.println("\n=== Using SBLocaleConfig ===");
        SBLocaleConfig localeConfig = SBLocaleConfig.getInstance();
        
        localeConfig.setEnglish();
        System.out.println("Current locale: " + localeConfig.getCurrentLocale());
        System.out.println("Message: " + messageManager.getMessage("error.no.result"));
        
        localeConfig.setJapanese();
        System.out.println("Current locale: " + localeConfig.getCurrentLocale());
        System.out.println("メッセージ: " + messageManager.getMessage("error.no.result"));
        
        // 5. 例外での国際化メッセージ使用例
        System.out.println("\n=== Exception Examples ===");
        try {
            localeConfig.setEnglish();
            throw new SBTransactionException("transaction.error.unsupported.propagation", "INVALID_TYPE");
        } catch (SBTransactionException e) {
            System.out.println("English Exception: " + e.getMessage());
        }
        
        try {
            localeConfig.setJapanese();
            throw new SBTransactionException("transaction.error.unsupported.propagation", "INVALID_TYPE");
        } catch (SBTransactionException e) {
            System.out.println("Japanese Exception: " + e.getMessage());
        }
    }
}