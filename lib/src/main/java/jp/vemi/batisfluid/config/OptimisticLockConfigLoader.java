/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.batisfluid.config.OptimisticLockConfig.EntityLockConfig;
import jp.vemi.batisfluid.config.OptimisticLockConfig.LockType;

/**
 * 楽観的排他制御設定ファイルの読み込みを行うクラス。
 * <p>
 * プロパティファイルから楽観的排他制御の設定を読み込み、
 * OptimisticLockConfigオブジェクトを構築します。
 * 旧ファイル名（seasarbatis-optimistic-lock.properties）と
 * 新ファイル名（batisfluid-optimistic-lock.properties）の両方をサポートします。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
public class OptimisticLockConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockConfigLoader.class);
    
    /**
     * デフォルトの設定ファイル名（新）。
     */
    public static final String DEFAULT_CONFIG_FILE = "batisfluid-optimistic-lock.properties";
    
    /**
     * 旧バージョン互換の設定ファイル名。
     *
     * @deprecated v0.0.2以降はbatisfluid-optimistic-lock.propertiesを使用してください。
     */
    @Deprecated(since = "0.0.2")
    public static final String LEGACY_CONFIG_FILE = "seasarbatis-optimistic-lock.properties";
    
    /**
     * 設定のプレフィックス（新）。
     */
    private static final String CONFIG_PREFIX = "batisfluid.optimistic-lock.";
    
    /**
     * 旧バージョン互換の設定プレフィックス。
     */
    private static final String LEGACY_CONFIG_PREFIX = "seasarbatis.optimistic-lock.";
    
    /**
     * デフォルトの設定ファイルから楽観的排他制御設定を読み込みます。
     * <p>
     * 新ファイル名（batisfluid-optimistic-lock.properties）を優先的に読み込み、
     * 存在しない場合は旧ファイル名（seasarbatis-optimistic-lock.properties）を試行します。
     * </p>
     * 
     * @return 楽観的排他制御設定
     */
    public static OptimisticLockConfig loadDefault() {
        // 新ファイル名を優先
        InputStream is = OptimisticLockConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
            return load(DEFAULT_CONFIG_FILE, CONFIG_PREFIX);
        }
        
        // 旧ファイル名をフォールバック
        is = OptimisticLockConfigLoader.class.getClassLoader().getResourceAsStream(LEGACY_CONFIG_FILE);
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                // ignore
            }
            logger.warn("旧設定ファイル名 '{}' が使用されています。v0.0.3以降のサポートのため '{}' への移行を推奨します。",
                       LEGACY_CONFIG_FILE, DEFAULT_CONFIG_FILE);
            return load(LEGACY_CONFIG_FILE, LEGACY_CONFIG_PREFIX);
        }
        
        logger.info("楽観的排他制御設定ファイルが見つかりません。デフォルト設定を使用します。");
        return new OptimisticLockConfig();
    }
    
    /**
     * 指定された設定ファイルから楽観的排他制御設定を読み込みます。
     * 
     * @param configFilePath 設定ファイルのパス
     * @return 楽観的排他制御設定
     */
    public static OptimisticLockConfig load(String configFilePath) {
        // ファイル名から適切なプレフィックスを判断
        String prefix = configFilePath.contains("seasarbatis") ? LEGACY_CONFIG_PREFIX : CONFIG_PREFIX;
        return load(configFilePath, prefix);
    }
    
    /**
     * 指定された設定ファイルとプレフィックスから楽観的排他制御設定を読み込みます。
     * 
     * @param configFilePath 設定ファイルのパス
     * @param prefix 設定キーのプレフィックス
     * @return 楽観的排他制御設定
     */
    private static OptimisticLockConfig load(String configFilePath, String prefix) {
        OptimisticLockConfig config = new OptimisticLockConfig();
        
        try (InputStream is = OptimisticLockConfigLoader.class.getClassLoader().getResourceAsStream(configFilePath)) {
            if (is == null) {
                logger.info("楽観的排他制御設定ファイルが見つかりません: {}. デフォルト設定を使用します。", configFilePath);
                return config;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            // グローバル設定の読み込み
            loadGlobalConfig(config, props, prefix);
            
            // エンティティ固有設定の読み込み
            loadEntityConfigs(config, props, prefix);
            
            logger.info("楽観的排他制御設定ファイルを読み込みました: {}", configFilePath);
            
        } catch (IOException e) {
            logger.error("楽観的排他制御設定ファイルの読み込みに失敗しました: {}", configFilePath, e);
        }
        
        return config;
    }
    
    /**
     * グローバル設定を読み込みます。
     * 
     * @param config 楽観的排他制御設定
     * @param props プロパティ
     * @param prefix 設定キーのプレフィックス
     */
    private static void loadGlobalConfig(OptimisticLockConfig config, Properties props, String prefix) {
        // 楽観的排他制御の有効性
        String enabledStr = props.getProperty(prefix + "enabled", "true");
        config.setEnabled(Boolean.parseBoolean(enabledStr));
        
        // デフォルトの楽観的排他制御タイプ
        String defaultTypeStr = props.getProperty(prefix + "default-type", "NONE");
        try {
            LockType defaultType = LockType.valueOf(defaultTypeStr.toUpperCase());
            config.setDefaultLockType(defaultType);
        } catch (IllegalArgumentException e) {
            logger.warn("無効なデフォルト楽観的排他制御タイプです: {}. NONEを使用します。", defaultTypeStr);
            config.setDefaultLockType(LockType.NONE);
        }
    }
    
    /**
     * エンティティ固有設定を読み込みます。
     * 
     * @param config 楽観的排他制御設定
     * @param props プロパティ
     * @param prefix 設定キーのプレフィックス
     */
    private static void loadEntityConfigs(OptimisticLockConfig config, Properties props, String prefix) {
        String entityPrefix = prefix + "entity.";
        
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(entityPrefix)) {
                String remaining = key.substring(entityPrefix.length());
                String[] parts = remaining.split("\\.", 2);
                
                if (parts.length == 2) {
                    String className = parts[0];
                    String property = parts[1];
                    
                    try {
                        Class<?> entityClass = Class.forName(className);
                        
                        if ("type".equals(property)) {
                            String typeStr = props.getProperty(key);
                            LockType lockType = LockType.valueOf(typeStr.toUpperCase());
                            
                            String columnProperty = entityPrefix + className + ".column";
                            String columnName = props.getProperty(columnProperty);
                            
                            EntityLockConfig entityConfig = new EntityLockConfig(lockType, columnName);
                            config.addEntityConfig(entityClass, entityConfig);
                            
                            logger.debug("エンティティ楽観的排他制御設定を追加: {} -> {}, column: {}", 
                                       className, lockType, columnName);
                        }
                        
                    } catch (ClassNotFoundException e) {
                        logger.warn("エンティティクラスが見つかりません: {}", className);
                    } catch (IllegalArgumentException e) {
                        logger.warn("無効な楽観的排他制御タイプです: {} for {}", props.getProperty(key), className);
                    }
                }
            }
        }
    }
}
