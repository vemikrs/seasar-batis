/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * SeasarBatisの国際化メッセージを管理するクラスです。
 * <p>
 * リソースバンドルを使用してロケールに応じたメッセージを提供します。
 * デフォルトのロケールはシステムのロケールが使用され、
 * 対応していないロケールの場合は英語メッセージが返されます。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0-beta1
 * @since 2025/01/01
 */
public class SBMessageManager {
    
    private static final String BUNDLE_NAME = "jp.vemi.seasarbatis.messages";
    private static final SBMessageManager INSTANCE = new SBMessageManager();
    
    private Locale currentLocale;
    private ResourceBundle bundle;
    
    /**
     * SBMessageManagerのシングルトンインスタンスを取得します。
     * 
     * @return SBMessageManagerのインスタンス
     */
    public static SBMessageManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * SBMessageManagerを構築します。
     * デフォルトロケールで初期化されます。
     */
    private SBMessageManager() {
        this.currentLocale = Locale.getDefault();
        loadBundle();
    }
    
    /**
     * 現在のロケールを設定します。
     * 
     * @param locale 設定するロケール
     */
    public void setLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (!this.currentLocale.equals(locale)) {
            this.currentLocale = locale;
            loadBundle();
        }
    }
    
    /**
     * 現在のロケールを取得します。
     * 
     * @return 現在のロケール
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * 指定されたキーに対応するメッセージを取得します。
     * 
     * @param key メッセージキー
     * @return ローカライズされたメッセージ
     */
    public String getMessage(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "Message not found: " + key;
        }
    }
    
    /**
     * 指定されたキーに対応するメッセージを取得し、パラメータで置換します。
     * 
     * @param key メッセージキー
     * @param args 置換パラメータ
     * @return ローカライズされ、パラメータが置換されたメッセージ
     */
    public String getMessage(String key, Object... args) {
        try {
            String message = bundle.getString(key);
            return MessageFormat.format(message, args);
        } catch (MissingResourceException e) {
            return "Message not found: " + key;
        }
    }
    
    /**
     * リソースバンドルを読み込みます。
     */
    private void loadBundle() {
        try {
            // デフォルトロケール(システムロケール)へのフォールバックを抑制し、
            // 指定ロケールが見つからない場合はベース(英語)にフォールバックさせる
            ResourceBundle.Control noDefaultFallback = ResourceBundle.Control
                    .getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale, noDefaultFallback);
        } catch (MissingResourceException e) {
            // フォールバックとして英語のバンドルを使用
            bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH, ResourceBundle.Control
                    .getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        }
    }
}