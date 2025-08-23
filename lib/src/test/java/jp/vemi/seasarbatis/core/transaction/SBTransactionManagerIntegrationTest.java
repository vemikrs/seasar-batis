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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SBTransactionManagerのさらなるテストクラス。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
class SBTransactionManagerIntegrationTest {

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
        // 独立したセッションを返すようにモック設定
        when(sqlSessionFactory.openSession(false)).thenReturn(sqlSession1, sqlSession2);
        txManager = new SBTransactionManager(sqlSessionFactory);
    }

    @Test
    void testNestedIndependentTransactions() {
        // ネストされた独立トランザクションのテスト
        final AtomicBoolean innerTransactionExecuted = new AtomicBoolean(false);
        final AtomicBoolean outerTransactionActive = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() {
                outerTransactionActive.set(txManager.isActive());
                
                // 内側で独立トランザクションを実行
                String innerResult = txManager.executeWithTransaction(true, new Callable<String>() {
                    @Override
                    public String call() {
                        innerTransactionExecuted.set(true);
                        return "inner";
                    }
                });
                
                // 外側のトランザクションが依然として有効であることを確認
                assertTrue(txManager.isActive(), "外側のトランザクションが有効であること");
                assertEquals("inner", innerResult, "内側のトランザクションが正常に実行されること");
                
                return "outer";
            }
        });
        
        assertTrue(outerTransactionActive.get(), "外側のトランザクションが開始されていたこと");
        assertTrue(innerTransactionExecuted.get(), "内側のトランザクションが実行されたこと");
        assertEquals("outer", result, "外側のトランザクションの結果が正常に返されること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
    }

    @Test
    void testIndependentTransactionRollbackIsolation() {
        // 独立トランザクションのロールバックが外側のトランザクションに影響しないことのテスト
        final AtomicBoolean outerTransactionStillActive = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() {
                assertTrue(txManager.isActive(), "外側のトランザクションが有効であること");
                
                // 内側で独立トランザクションを実行し、例外を発生させる
                try {
                    txManager.executeWithTransaction(true, new Callable<String>() {
                        @Override
                        public String call() {
                            throw new RuntimeException("内側のトランザクションエラー");
                        }
                    });
                    fail("例外がスローされるべき");
                } catch (RuntimeException e) {
                    // 期待される例外
                    assertEquals("内側のトランザクションエラー", e.getCause().getMessage());
                }
                
                // 外側のトランザクションが依然として有効であることを確認
                outerTransactionStillActive.set(txManager.isActive());
                
                return "outer success";
            }
        });
        
        assertTrue(outerTransactionStillActive.get(), "内側のトランザクションエラー後も外側のトランザクションが有効であること");
        assertEquals("outer success", result, "外側のトランザクションが正常に完了すること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
    }

    @Test
    void testMultipleIndependentTransactions() {
        // 複数の独立トランザクションの実行テスト
        final AtomicBoolean tx1Executed = new AtomicBoolean(false);
        final AtomicBoolean tx2Executed = new AtomicBoolean(false);
        
        String result = txManager.executeWithTransaction(false, new Callable<String>() {
            @Override
            public String call() {
                // 最初の独立トランザクション
                String result1 = txManager.executeWithTransaction(true, new Callable<String>() {
                    @Override
                    public String call() {
                        tx1Executed.set(true);
                        return "tx1";
                    }
                });
                
                // 2番目の独立トランザクション
                String result2 = txManager.executeWithTransaction(true, new Callable<String>() {
                    @Override
                    public String call() {
                        tx2Executed.set(true);
                        return "tx2";
                    }
                });
                
                assertEquals("tx1", result1, "最初の独立トランザクションが正常に実行されること");
                assertEquals("tx2", result2, "2番目の独立トランザクションが正常に実行されること");
                
                return "main";
            }
        });
        
        assertTrue(tx1Executed.get(), "最初の独立トランザクションが実行されたこと");
        assertTrue(tx2Executed.get(), "2番目の独立トランザクションが実行されたこと");
        assertEquals("main", result, "メイントランザクションが正常に完了すること");
        assertFalse(txManager.isActive(), "全てのトランザクションが終了していること");
    }
}