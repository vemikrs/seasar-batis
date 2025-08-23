/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.transaction;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;

/**
 * SBTransactionManagerのテストクラス。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
class SBTransactionManagerTest {

    @Mock
    private SqlSessionFactory sqlSessionFactory;
    
    @Mock
    private SqlSession sqlSession;
    
    private SBTransactionManager txManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sqlSessionFactory.openSession(false)).thenReturn(sqlSession);
        txManager = new SBTransactionManager(sqlSessionFactory);
    }

    @Test
    void testIndependentTransactionIsolation() {
        // 独立トランザクションが他のトランザクションと分離されているかテスト
        
        // 最初に通常のトランザクションを開始
        Boolean result1 = txManager.executeWithTransaction(false, new Callable<Boolean>() {
            @Override
            public Boolean call() {
                assertTrue(txManager.isActive(), "通常のトランザクションが開始されていること");
                
                // 独立トランザクションを実行
                String result = txManager.executeWithTransaction(true, new Callable<String>() {
                    @Override
                    public String call() {
                        return "independent";
                    }
                });
                
                assertEquals("independent", result, "独立トランザクションが正常に実行されること");
                assertTrue(txManager.isActive(), "独立トランザクション後も元のトランザクションが有効であること");
                
                return true;
            }
        });
        
        assertTrue(result1, "テストが成功したこと");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
    }

    @Test
    void testIndependentTransactionWithException() {
        // 独立トランザクションで例外が発生した場合のテスト
        
        assertThrows(RuntimeException.class, () -> {
            txManager.executeWithTransaction(true, new Callable<String>() {
                @Override
                public String call() {
                    throw new RuntimeException("独立トランザクションエラー");
                }
            });
        });
        
        // 例外後もトランザクションが適切に終了していることを確認
        assertFalse(txManager.isActive(), "例外後トランザクションが終了していること");
    }
}