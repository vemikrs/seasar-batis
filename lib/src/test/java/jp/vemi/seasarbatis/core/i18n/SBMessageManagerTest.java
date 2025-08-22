/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SBMessageManagerのテストクラスです。
 */
public class SBMessageManagerTest {
    
    private SBMessageManager messageManager;
    
    @BeforeEach
    void setUp() {
        messageManager = SBMessageManager.getInstance();
    }
    
    @Test
    void testGetMessageInEnglish() {
        // 英語ロケールに設定
        messageManager.setLocale(Locale.ENGLISH);
        
        // 英語メッセージが取得できることを確認
        String message = messageManager.getMessage("transaction.error.execution");
        assertThat(message).isEqualTo("Transaction execution error");
    }
    
    @Test
    void testGetMessageInJapanese() {
        // 日本語ロケールに設定
        messageManager.setLocale(Locale.JAPANESE);
        
        // 日本語メッセージが取得できることを確認
        String message = messageManager.getMessage("transaction.error.execution");
        assertThat(message).isEqualTo("トランザクション実行エラー");
    }
    
    @Test
    void testGetMessageWithParameters() {
        // 英語ロケールに設定
        messageManager.setLocale(Locale.ENGLISH);
        
        // パラメータ付きメッセージが正しく置換されることを確認
        String message = messageManager.getMessage("transaction.error.unsupported.propagation", "INVALID_TYPE");
        assertThat(message).isEqualTo("Unsupported propagation type: INVALID_TYPE");
    }
    
    @Test
    void testGetMessageWithParametersInJapanese() {
        // 日本語ロケールに設定
        messageManager.setLocale(Locale.JAPANESE);
        
        // パラメータ付きメッセージが正しく置換されることを確認
        String message = messageManager.getMessage("transaction.error.unsupported.propagation", "INVALID_TYPE");
        assertThat(message).isEqualTo("未対応の伝播タイプです: INVALID_TYPE");
    }
    
    @Test
    void testGetMessageNotFound() {
        // 存在しないキーの場合
        String message = messageManager.getMessage("nonexistent.key");
        assertThat(message).isEqualTo("Message not found: nonexistent.key");
    }
    
    @Test
    void testLocaleChange() {
        // 最初に日本語で設定
        messageManager.setLocale(Locale.JAPANESE);
        String japaneseMessage = messageManager.getMessage("transaction.error.execution");
        assertThat(japaneseMessage).isEqualTo("トランザクション実行エラー");
        
        // 英語に変更
        messageManager.setLocale(Locale.ENGLISH);
        String englishMessage = messageManager.getMessage("transaction.error.execution");
        assertThat(englishMessage).isEqualTo("Transaction execution error");
    }
    
    @Test
    void testGetCurrentLocale() {
        // ロケールが正しく設定されることを確認
        messageManager.setLocale(Locale.JAPANESE);
        assertThat(messageManager.getCurrentLocale()).isEqualTo(Locale.JAPANESE);
        
        messageManager.setLocale(Locale.ENGLISH);
        assertThat(messageManager.getCurrentLocale()).isEqualTo(Locale.ENGLISH);
    }
    
    @Test
    void testSetNullLocale() {
        // nullを設定した場合はデフォルトロケールが使用されることを確認
        Locale originalDefault = Locale.getDefault();
        messageManager.setLocale(null);
        assertThat(messageManager.getCurrentLocale()).isEqualTo(originalDefault);
    }
}