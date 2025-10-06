/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.batisfluid.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jp.vemi.batisfluid.config.OptimisticLockConfig.LockType;

/**
 * OptimisticLockConfigLoaderのテストクラス。
 *
 * @author H.Kurosawa
 * @version 0.0.2
 */
class OptimisticLockConfigLoaderTest {
    
    @Test
    void testLoadDefault() {
        // デフォルト設定の読み込みをテスト
        // ファイルが存在しない場合でもエラーにならないことを確認
        OptimisticLockConfig config = OptimisticLockConfigLoader.loadDefault();
        assertNotNull(config, "設定がnullであってはならない");
        assertTrue(config.isEnabled(), "デフォルトでは有効であるべき");
        assertEquals(LockType.NONE, config.getDefaultLockType(), "デフォルトのロックタイプはNONEであるべき");
    }
    
    @Test
    void testLoadNonExistentFile() {
        // 存在しないファイルを読み込んでもエラーにならないことを確認
        OptimisticLockConfig config = OptimisticLockConfigLoader.load("non-existent-file.properties");
        assertNotNull(config, "設定がnullであってはならない");
        assertTrue(config.isEnabled(), "デフォルトでは有効であるべき");
    }
    
    @Test
    void testConfigBuilder() {
        // OptimisticLockConfigのビルダーパターンをテスト
        OptimisticLockConfig config = new OptimisticLockConfig()
            .setDefaultLockType(LockType.VERSION)
            .setEnabled(true);
        
        assertEquals(LockType.VERSION, config.getDefaultLockType(), "設定したロックタイプが反映されるべき");
        assertTrue(config.isEnabled(), "設定した有効フラグが反映されるべき");
    }
    
    @Test
    void testDisableOptimisticLock() {
        // 楽観的排他制御を無効にできることをテスト
        OptimisticLockConfig config = new OptimisticLockConfig()
            .setEnabled(false);
        
        assertFalse(config.isEnabled(), "無効化した場合はfalseであるべき");
    }
    
    @Test
    void testEntityConfigAddition() {
        // エンティティ固有の設定を追加できることをテスト
        OptimisticLockConfig config = new OptimisticLockConfig();
        OptimisticLockConfig.EntityLockConfig entityConfig = 
            new OptimisticLockConfig.EntityLockConfig(LockType.VERSION, "version");
        
        config.addEntityConfig(TestEntity.class, entityConfig);
        
        assertTrue(config.getEntityConfig(TestEntity.class).isPresent(), 
                  "追加したエンティティ設定が取得できるべき");
        assertEquals(LockType.VERSION, 
                    config.getEntityConfig(TestEntity.class).get().getLockType(),
                    "設定したロックタイプが反映されるべき");
        assertEquals("version", 
                    config.getEntityConfig(TestEntity.class).get().getColumnName(),
                    "設定したカラム名が反映されるべき");
    }
    
    @Test
    void testEntityConfigNotFound() {
        // 設定されていないエンティティの設定を取得した場合
        OptimisticLockConfig config = new OptimisticLockConfig();
        assertFalse(config.getEntityConfig(TestEntity.class).isPresent(), 
                   "設定されていないエンティティの場合はEmptyであるべき");
    }
    
    // テスト用のダミーエンティティクラス
    private static class TestEntity {
        private Long id;
        private Long version;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Long getVersion() {
            return version;
        }
        
        public void setVersion(Long version) {
            this.version = version;
        }
    }
}
