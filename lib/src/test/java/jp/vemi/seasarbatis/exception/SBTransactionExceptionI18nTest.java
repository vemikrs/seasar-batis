/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * SBTransactionExceptionの国際化対応テストクラスです。
 */
public class SBTransactionExceptionI18nTest {
    
    @BeforeEach
    void setUp() {
        // テスト開始時にロケールをリセット
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
    }
    
    @Test
    void testExceptionMessageInEnglish() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        
        SBTransactionException exception = new SBTransactionException("transaction.error.execution");
        assertThat(exception.getMessage()).isEqualTo("Transaction execution error");
    }
    
    @Test
    void testExceptionMessageInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        
        SBTransactionException exception = new SBTransactionException("transaction.error.execution");
        assertThat(exception.getMessage()).isEqualTo("トランザクション実行エラー");
    }
    
    @Test
    void testExceptionMessageWithParameters() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        
        SBTransactionException exception = new SBTransactionException("transaction.error.unsupported.propagation", "INVALID_TYPE");
        assertThat(exception.getMessage()).isEqualTo("Unsupported propagation type: INVALID_TYPE");
    }
    
    @Test
    void testExceptionMessageWithParametersInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        
        SBTransactionException exception = new SBTransactionException("transaction.error.unsupported.propagation", "INVALID_TYPE");
        assertThat(exception.getMessage()).isEqualTo("未対応の伝播タイプです: INVALID_TYPE");
    }
    
    @Test
    void testExceptionWithCause() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        
        RuntimeException cause = new RuntimeException("Original cause");
        SBTransactionException exception = new SBTransactionException("transaction.error.execution", cause);
        
        assertThat(exception.getMessage()).isEqualTo("Transaction execution error");
        assertThat(exception.getCause()).isSameAs(cause);
    }
    
    @Test
    void testExceptionWithCauseAndParameters() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        
        RuntimeException cause = new RuntimeException("Original cause");
        SBTransactionException exception = new SBTransactionException("transaction.error.savepoint.not.found", cause, "sp1");
        
        assertThat(exception.getMessage()).isEqualTo("セーブポイントが見つかりません: sp1");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}