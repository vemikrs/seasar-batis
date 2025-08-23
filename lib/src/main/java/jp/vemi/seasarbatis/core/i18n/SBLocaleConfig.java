/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.i18n;

import java.util.Locale;

/**
 * SeasarBatisの国際化設定を管理するクラスです。
 * <p>
 * ロケールの設定や国際化に関する設定を一元管理します。
 * アプリケーション全体で共通のロケール設定を提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/08/23
 */
public class SBLocaleConfig {
    
    private static final SBLocaleConfig INSTANCE = new SBLocaleConfig();
    
    /**
     * SBLocaleConfigのシングルトンインスタンスを取得します。
     * 
     * @return SBLocaleConfigのインスタンス
     */
    public static SBLocaleConfig getInstance() {
        return INSTANCE;
    }
    
    /**
     * SBLocaleConfigを構築します。
     */
    private SBLocaleConfig() {
    }
    
    /**
     * アプリケーション全体のロケールを設定します。
     * 
     * @param locale 設定するロケール
     */
    public void setLocale(Locale locale) {
        SBMessageManager.getInstance().setLocale(locale);
    }
    
    /**
     * 現在のロケールを取得します。
     * 
     * @return 現在のロケール
     */
    public Locale getCurrentLocale() {
        return SBMessageManager.getInstance().getCurrentLocale();
    }
    
    /**
     * ロケールを日本語に設定します。
     */
    public void setJapanese() {
        setLocale(Locale.JAPANESE);
    }
    
    /**
     * ロケールを英語に設定します。
     */
    public void setEnglish() {
        setLocale(Locale.ENGLISH);
    }
    
    /**
     * システムのデフォルトロケールに設定します。
     */
    public void setDefault() {
        setLocale(Locale.getDefault());
    }
}