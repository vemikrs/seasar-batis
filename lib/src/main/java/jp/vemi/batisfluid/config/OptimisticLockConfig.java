/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 楽観的排他制御の設定を管理するクラス。
 * <p>
 * エンティティごとのバージョンカラムや最終更新日時カラムの設定を管理し、
 * 自動的な楽観的排他制御の動作を制御します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 0.0.2
 * @since 0.0.2
 */
public class OptimisticLockConfig {
    
    /**
     * 楽観的排他制御の種類を表す列挙型。
     */
    public enum LockType {
        /** バージョン番号による制御 */
        VERSION,
        /** 最終更新日時による制御 */
        LAST_MODIFIED,
        /** 楽観的排他制御を使用しない */
        NONE
    }
    
    /**
     * エンティティごとの楽観的排他制御設定。
     */
    public static class EntityLockConfig {
        private final LockType lockType;
        private final String columnName;
        
        /**
         * エンティティの楽観的排他制御設定を構築します。
         *
         * @param lockType 楽観的排他制御の種類
         * @param columnName カラム名
         */
        public EntityLockConfig(LockType lockType, String columnName) {
            this.lockType = lockType;
            this.columnName = columnName;
        }
        
        /**
         * 楽観的排他制御の種類を取得します。
         *
         * @return 楽観的排他制御の種類
         */
        public LockType getLockType() {
            return lockType;
        }
        
        /**
         * カラム名を取得します。
         *
         * @return カラム名
         */
        public String getColumnName() {
            return columnName;
        }
    }
    
    /**
     * エンティティクラスごとの楽観的排他制御設定を保持するマップ。
     */
    private final Map<Class<?>, EntityLockConfig> entityConfigs = new HashMap<>();
    
    /**
     * デフォルトの楽観的排他制御の種類。
     */
    private LockType defaultLockType = LockType.NONE;
    
    /**
     * 楽観的排他制御が有効かどうかを示すフラグ。
     */
    private boolean enabled = true;
    
    /**
     * 指定されたエンティティクラスの楽観的排他制御設定を取得します。
     * 
     * @param entityClass エンティティクラス
     * @return 楽観的排他制御設定、設定がない場合はEmpty
     */
    public Optional<EntityLockConfig> getEntityConfig(Class<?> entityClass) {
        return Optional.ofNullable(entityConfigs.get(entityClass));
    }
    
    /**
     * エンティティクラスの楽観的排他制御設定を追加します。
     * 
     * @param entityClass エンティティクラス
     * @param config 楽観的排他制御設定
     * @return このインスタンス（メソッドチェーン用）
     */
    public OptimisticLockConfig addEntityConfig(Class<?> entityClass, EntityLockConfig config) {
        entityConfigs.put(entityClass, config);
        return this;
    }
    
    /**
     * デフォルトの楽観的排他制御の種類を取得します。
     * 
     * @return デフォルトの楽観的排他制御の種類
     */
    public LockType getDefaultLockType() {
        return defaultLockType;
    }
    
    /**
     * デフォルトの楽観的排他制御の種類を設定します。
     * 
     * @param defaultLockType デフォルトの楽観的排他制御の種類
     * @return このインスタンス（メソッドチェーン用）
     */
    public OptimisticLockConfig setDefaultLockType(LockType defaultLockType) {
        this.defaultLockType = defaultLockType;
        return this;
    }
    
    /**
     * 楽観的排他制御が有効かどうかを取得します。
     * 
     * @return 有効な場合true、無効な場合false
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 楽観的排他制御を有効または無効にします。
     * 
     * @param enabled 有効にする場合true、無効にする場合false
     * @return このインスタンス（メソッドチェーン用）
     */
    public OptimisticLockConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
