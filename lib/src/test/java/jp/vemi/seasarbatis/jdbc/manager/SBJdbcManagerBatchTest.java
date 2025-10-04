/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.seasarbatis.exception.SBIllegalStateException;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;
import jp.vemi.seasarbatis.jdbc.SBJdbcManagerFactory;
import jp.vemi.seasarbatis.test.entity.TestSbUser;

/**
 * SBJdbcManagerのバッチ処理機能のテストクラスです。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SBJdbcManagerBatchTest {

    private SBJdbcManager jdbcManager;

    @BeforeEach
    void setUp() throws Exception {
        // ローカル開発ではH2を使用し、CI環境ではMySQLを使用
        String configFile = System.getProperty("test.database", "h2").equals("mysql") 
            ? "mybatis-test-config.xml" 
            : "mybatis-h2-test-config.xml";
        
        SBJdbcManagerFactory factory = new SBJdbcManagerFactory(configFile);
        jdbcManager = factory.create();
        
        initializeDatabase();
    }

    private void initializeDatabase() throws Exception {
        // スキーマ作成と初期データの投入
        executeSqlScript("/ddl/01_create_test_schema.sql");
        executeSqlScript("/ddl/02_insert_initial_data.sql");
    }

    private void executeSqlScript(String resourcePath) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("SQLファイルが見つかりません: " + resourcePath);
            }
            String sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            // セミコロンで分割して、空行以外を実行
            String[] commands = sql.split(";");
            try (SqlSession session = jdbcManager.getSqlSessionFactory().openSession(true);
                    Connection conn = session.getConnection();
                    Statement stmt = conn.createStatement()) {
                // タイムアウトを設定
                stmt.setQueryTimeout(5);
                for (String cmd : commands) {
                    if (!cmd.trim().isEmpty()) {
                        try {
                            stmt.executeUpdate(cmd);
                        } catch (Exception e) {
                            // DDLエラーを許容（既に存在する場合など）
                            System.out.println("[WARN] SQL実行エラー（許容）: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Test
    void testBatchInsert() {
        // テストデータ準備
        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(101L)
                        .name("バッチユーザー1")
                        .sequenceNo(1)
                        .amount(1000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(102L)
                        .name("バッチユーザー2")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(103L)
                        .name("バッチユーザー3")
                        .sequenceNo(3)
                        .amount(3000.0)
                        .isActive(false)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        // バッチ登録実行
        List<TestSbUser> results = jdbcManager.batchInsert(users);

        // 検証
        assertNotNull(results);
        assertEquals(3, results.size());

        for (int i = 0; i < users.size(); i++) {
            TestSbUser expected = users.get(i);
            TestSbUser actual = results.get(i);
            assertEquals(expected.getId(), actual.getId());
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getSequenceNo(), actual.getSequenceNo());
            assertEquals(expected.getAmount(), actual.getAmount());
            assertEquals(expected.getIsActive(), actual.getIsActive());
        }
    }

    @Test
    void testBatchInsertEmpty() {
        // 空のリストでのテスト
        List<TestSbUser> emptyList = new ArrayList<>();
        
        // 例外がスローされることを確認
        assertThrows(SBIllegalStateException.class, () -> {
            jdbcManager.batchInsert(emptyList);
        });
    }

    @Test
    void testBatchInsertNull() {
        // nullでのテスト
        assertThrows(SBIllegalStateException.class, () -> {
            jdbcManager.batchInsert(null);
        });
    }

    @Test
    void testBatchUpdate() {
        // テストデータ準備（事前に登録）
        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(201L)
                        .name("更新前ユーザー1")
                        .sequenceNo(1)
                        .amount(1000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(202L)
                        .name("更新前ユーザー2")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );
        
        // 事前登録
        jdbcManager.batchInsert(users);

        // 更新データ準備
        List<TestSbUser> updateUsers = Arrays.asList(
                TestSbUser.builder()
                        .id(201L)
                        .name("更新後ユーザー1")
                        .sequenceNo(1)
                        .amount(1500.0)
                        .isActive(false)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(202L)
                        .name("更新後ユーザー2")
                        .sequenceNo(2)
                        .amount(2500.0)
                        .isActive(false)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        // バッチ更新実行
        List<Integer> results = jdbcManager.batchUpdate(updateUsers);

        // 検証
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(count -> count > 0), "すべての更新で1件以上が影響されている必要があります");

        // 更新内容の確認
        TestSbUser updated1 = jdbcManager.findByPk(TestSbUser.builder().id(201L).build()).getSingleResult();
        assertEquals("更新後ユーザー1", updated1.getName());
        assertEquals(1500.0, updated1.getAmount());
        assertFalse(updated1.getIsActive());

        TestSbUser updated2 = jdbcManager.findByPk(TestSbUser.builder().id(202L).build()).getSingleResult();
        assertEquals("更新後ユーザー2", updated2.getName());
        assertEquals(2500.0, updated2.getAmount());
        assertFalse(updated2.getIsActive());
    }

    @Test
    void testBatchUpdateEmpty() {
        // 空のリストでのテスト
        List<TestSbUser> emptyList = new ArrayList<>();
        
        // 例外がスローされることを確認
        assertThrows(SBIllegalStateException.class, () -> {
            jdbcManager.batchUpdate(emptyList);
        });
    }

    @Test
    void testBatchDelete() {
        // テストデータ準備（事前に登録）
        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(301L)
                        .name("削除対象ユーザー1")
                        .sequenceNo(1)
                        .amount(1000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(302L)
                        .name("削除対象ユーザー2")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );
        
        // 事前登録
        jdbcManager.batchInsert(users);

        // バッチ削除実行
        List<Integer> results = jdbcManager.batchDelete(users);

        // 検証
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(count -> count > 0), "すべての削除で1件以上が影響されている必要があります");

        // 削除確認（例外を抑制して検索）
        TestSbUser deleted1 = jdbcManager.findByPkNoException(TestSbUser.builder().id(301L).build()).getSingleResult();
        TestSbUser deleted2 = jdbcManager.findByPkNoException(TestSbUser.builder().id(302L).build()).getSingleResult();
        
        // 削除されているため、nullまたは見つからないことを確認
        assertTrue(deleted1 == null || deleted2 == null, "削除されたレコードは取得できない必要があります");
    }

    @Test
    void testBatchDeleteEmpty() {
        // 空のリストでのテスト
        List<TestSbUser> emptyList = new ArrayList<>();
        
        // 例外がスローされることを確認
        assertThrows(SBIllegalStateException.class, () -> {
            jdbcManager.batchDelete(emptyList);
        });
    }

    @Test
    void testBatchInsertOrUpdate() {
        // 既存データ準備（更新対象）
        TestSbUser existingUser = TestSbUser.builder()
                .id(401L)
                .name("既存ユーザー")
                .sequenceNo(1)
                .amount(1000.0)
                .isActive(true)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        jdbcManager.insert(existingUser);

        // テストデータ準備（新規と更新の混在）
        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(401L)  // 既存データ（更新対象）
                        .name("更新されたユーザー")
                        .sequenceNo(1)
                        .amount(1500.0)
                        .isActive(false)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(402L)  // 新規データ（登録対象）
                        .name("新規ユーザー")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(403L)  // 新規データ（登録対象）
                        .name("新規ユーザー2")
                        .sequenceNo(3)
                        .amount(3000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        // バッチ登録または更新実行
        List<TestSbUser> results = jdbcManager.batchInsertOrUpdate(users);

        // 検証
        assertNotNull(results);
        assertEquals(3, results.size());

        // 更新されたユーザーの確認
        TestSbUser updated = jdbcManager.findByPk(TestSbUser.builder().id(401L).build()).getSingleResult();
        assertEquals("更新されたユーザー", updated.getName());
        assertEquals(1500.0, updated.getAmount());
        assertFalse(updated.getIsActive());

        // 新規登録されたユーザーの確認
        TestSbUser inserted1 = jdbcManager.findByPk(TestSbUser.builder().id(402L).build()).getSingleResult();
        assertEquals("新規ユーザー", inserted1.getName());
        assertEquals(2000.0, inserted1.getAmount());
        assertTrue(inserted1.getIsActive());

        TestSbUser inserted2 = jdbcManager.findByPk(TestSbUser.builder().id(403L).build()).getSingleResult();
        assertEquals("新規ユーザー2", inserted2.getName());
        assertEquals(3000.0, inserted2.getAmount());
        assertTrue(inserted2.getIsActive());
    }

    @Test
    void testBatchInsertOrUpdateEmpty() {
        // 空のリストでのテスト
        List<TestSbUser> emptyList = new ArrayList<>();
        
        // 例外がスローされることを確認
        assertThrows(SBIllegalStateException.class, () -> {
            jdbcManager.batchInsertOrUpdate(emptyList);
        });
    }

    @Test
    void testBatchOperationsWithIndependentTransaction() {
        // 独立したトランザクションでのバッチ処理テスト
        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(501L)
                        .name("独立トランザクションユーザー1")
                        .sequenceNo(1)
                        .amount(1000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(502L)
                        .name("独立トランザクションユーザー2")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        // 独立したトランザクションでバッチ登録実行
        List<TestSbUser> results = jdbcManager.batchInsert(users, true);

        // 検証
        assertNotNull(results);
        assertEquals(2, results.size());

        // 登録確認
        TestSbUser inserted1 = jdbcManager.findByPk(TestSbUser.builder().id(501L).build()).getSingleResult();
        TestSbUser inserted2 = jdbcManager.findByPk(TestSbUser.builder().id(502L).build()).getSingleResult();
        
        assertNotNull(inserted1);
        assertNotNull(inserted2);
        assertEquals("独立トランザクションユーザー1", inserted1.getName());
        assertEquals("独立トランザクションユーザー2", inserted2.getName());
    }
}