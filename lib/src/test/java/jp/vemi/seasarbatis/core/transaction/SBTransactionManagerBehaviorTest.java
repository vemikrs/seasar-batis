/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.transaction;

import java.util.concurrent.Callable;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jp.vemi.seasarbatis.exception.SBTransactionException;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SBTransactionManagerの実際のトランザクション動作テスト。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
class SBTransactionManagerBehaviorTest {

    @Mock
    private SqlSessionFactory sqlSessionFactory;
    
    @Mock
    private SqlSession sqlSession1;
    
    @Mock 
    private SqlSession sqlSession2;
    
    private SBTransactionManager txManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sqlSessionFactory.openSession(false)).thenReturn(sqlSession1, sqlSession2);
        txManager = new SBTransactionManager(sqlSessionFactory);
    }

    @Test
    void testIndependentTransactionCreatesNewSession() {
        // 独立トランザクションが新しいセッションを作成することを確認
        
        txManager.executeWithTransaction(true, new Callable<String>() {
            @Override
            public String call() {
                return "test";
            }
        });
        
        // sqlSessionFactory.openSession(false) が呼ばれたことを確認
        verify(sqlSessionFactory, times(1)).openSession(false);
        
        // セッションのメソッドが呼ばれたことを確認 
    verify(sqlSession1, times(1)).commit(true);
        verify(sqlSession1, times(1)).close();
    }

    @Test
    void testIndependentTransactionExceptionHandling() {
        // 独立トランザクションで例外が発生した場合の動作確認
        
        assertThrows(SBTransactionException.class, () -> {
            txManager.executeWithTransaction(true, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    throw new RuntimeException("テストエラー");
                }
            });
        });
        
        // ロールバックが呼ばれたことを確認
    verify(sqlSession1, times(1)).rollback(true);
        verify(sqlSession1, times(1)).close();
    }

    @Test
    void testNormalTransactionBehavior() {
        // 通常のトランザクション動作確認
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() {
                return "normal";
            }
        });
        
        assertEquals("normal", result);
        
        // 新しいセッションが作成されたことを確認
        verify(sqlSessionFactory, times(1)).openSession(false);
    verify(sqlSession1, times(1)).commit(true);
        verify(sqlSession1, times(1)).close();
    }
}