/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * SBLocaleConfigのテストクラスです。
 */
public class SBLocaleConfigTest {
    
    private SBLocaleConfig localeConfig;
    
    @BeforeEach
    void setUp() {
        localeConfig = SBLocaleConfig.getInstance();
    }
    
    @Test
    void testSetJapanese() {
        localeConfig.setJapanese();
        assertThat(localeConfig.getCurrentLocale()).isEqualTo(Locale.JAPANESE);
    }
    
    @Test
    void testSetEnglish() {
        localeConfig.setEnglish();
        assertThat(localeConfig.getCurrentLocale()).isEqualTo(Locale.ENGLISH);
    }
    
    @Test
    void testSetDefault() {
        Locale systemDefault = Locale.getDefault();
        localeConfig.setDefault();
        assertThat(localeConfig.getCurrentLocale()).isEqualTo(systemDefault);
    }
    
    @Test
    void testSetCustomLocale() {
        Locale customLocale = Locale.FRENCH;
        localeConfig.setLocale(customLocale);
        assertThat(localeConfig.getCurrentLocale()).isEqualTo(customLocale);
    }
    
    @Test
    void testSingletonInstance() {
        SBLocaleConfig instance1 = SBLocaleConfig.getInstance();
        SBLocaleConfig instance2 = SBLocaleConfig.getInstance();
        assertThat(instance1).isSameAs(instance2);
    }
}