/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

/**
 * その他の例外クラスの国際化対応テストクラスです。
 */
public class SBExceptionI18nTest {
    
    @BeforeEach
    void setUp() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
    }
    
    @Test
    @Tag("smoke")
    void testSBNoResultExceptionDefaultMessage() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        SBNoResultException exception = new SBNoResultException();
        assertThat(exception.getMessage()).isEqualTo("No result found");
    }
    
    @Test
    void testSBNoResultExceptionDefaultMessageInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        SBNoResultException exception = new SBNoResultException();
        assertThat(exception.getMessage()).isEqualTo("結果が見つかりません");
    }
    
    @Test
    void testSBNonUniqueResultExceptionDefaultMessage() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        SBNonUniqueResultException exception = new SBNonUniqueResultException();
        assertThat(exception.getMessage()).isEqualTo("Non-unique result found");
    }
    
    @Test
    void testSBNonUniqueResultExceptionDefaultMessageInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        SBNonUniqueResultException exception = new SBNonUniqueResultException();
        assertThat(exception.getMessage()).isEqualTo("一意でない結果です");
    }
    
    @Test
    void testSBEntityExceptionWithMessage() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        SBEntityException exception = new SBEntityException("entity.error.metadata");
        assertThat(exception.getMessage()).isEqualTo("Failed to retrieve entity metadata");
    }
    
    @Test
    void testSBEntityExceptionWithMessageInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        SBEntityException exception = new SBEntityException("entity.error.metadata");
        assertThat(exception.getMessage()).isEqualTo("エンティティのメタデータ取得に失敗しました");
    }
    
    @Test
    void testSBSQLExceptionWithMessage() {
        SBMessageManager.getInstance().setLocale(Locale.ENGLISH);
        SBSQLException exception = new SBSQLException("sql.error.execution");
        assertThat(exception.getMessage()).isEqualTo("SQL execution error");
    }
    
    @Test
    void testSBSQLExceptionWithMessageInJapanese() {
        SBMessageManager.getInstance().setLocale(Locale.JAPANESE);
        SBSQLException exception = new SBSQLException("sql.error.execution");
        assertThat(exception.getMessage()).isEqualTo("SQL実行エラー");
    }
}