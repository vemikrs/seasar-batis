/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.examples;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import jp.vemi.seasarbatis.jdbc.SBJdbcManager;
import jp.vemi.seasarbatis.jdbc.SBJdbcManagerFactory;
import jp.vemi.seasarbatis.test.entity.TestSbUser;

/**
 * SBJdbcManagerのバッチ処理機能のデモンストレーションクラスです。
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 */
public class BatchProcessingExample {

    public static void main(String[] args) {
        try {
            // SBJdbcManagerの初期化
            SBJdbcManagerFactory factory = new SBJdbcManagerFactory("mybatis-config.xml");
            SBJdbcManager jdbcManager = factory.create();

            // バッチ登録のデモ
            demonstrateBatchInsert(jdbcManager);

            // バッチ更新のデモ
            demonstrateBatchUpdate(jdbcManager);

            // バッチ削除のデモ
            demonstrateBatchDelete(jdbcManager);

            // バッチ登録または更新のデモ
            demonstrateBatchInsertOrUpdate(jdbcManager);

        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * バッチ登録のデモンストレーション
     */
    private static void demonstrateBatchInsert(SBJdbcManager jdbcManager) {
        System.out.println("=== バッチ登録のデモ ===");

        List<TestSbUser> users = Arrays.asList(
                TestSbUser.builder()
                        .id(1001L)
                        .name("バッチユーザー1")
                        .sequenceNo(1)
                        .amount(1000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(1002L)
                        .name("バッチユーザー2")
                        .sequenceNo(2)
                        .amount(2000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(1003L)
                        .name("バッチユーザー3")
                        .sequenceNo(3)
                        .amount(3000.0)
                        .isActive(false)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        try {
            List<TestSbUser> results = jdbcManager.batchInsert(users);
            System.out.println("バッチ登録完了: " + results.size() + " 件");
            results.forEach(user -> System.out.println("  登録済み: " + user.getName() + " (ID: " + user.getId() + ")"));
        } catch (Exception e) {
            System.err.println("バッチ登録エラー: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * バッチ更新のデモンストレーション
     */
    private static void demonstrateBatchUpdate(SBJdbcManager jdbcManager) {
        System.out.println("=== バッチ更新のデモ ===");

        List<TestSbUser> updateUsers = Arrays.asList(
                TestSbUser.builder()
                        .id(1001L)
                        .name("更新済みバッチユーザー1")
                        .sequenceNo(1)
                        .amount(1500.0)
                        .isActive(false)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()
                        .id(1002L)
                        .name("更新済みバッチユーザー2")
                        .sequenceNo(2)
                        .amount(2500.0)
                        .isActive(false)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        try {
            List<Integer> results = jdbcManager.batchUpdate(updateUsers);
            System.out.println("バッチ更新完了: " + results.size() + " 件");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("  更新件数: " + results.get(i) + " (ユーザーID: " + updateUsers.get(i).getId() + ")");
            }
        } catch (Exception e) {
            System.err.println("バッチ更新エラー: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * バッチ削除のデモンストレーション
     */
    private static void demonstrateBatchDelete(SBJdbcManager jdbcManager) {
        System.out.println("=== バッチ削除のデモ ===");

        List<TestSbUser> deleteUsers = Arrays.asList(
                TestSbUser.builder().id(1001L).build(),
                TestSbUser.builder().id(1002L).build()
        );

        try {
            List<Integer> results = jdbcManager.batchDelete(deleteUsers);
            System.out.println("バッチ削除完了: " + results.size() + " 件");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("  削除件数: " + results.get(i) + " (ユーザーID: " + deleteUsers.get(i).getId() + ")");
            }
        } catch (Exception e) {
            System.err.println("バッチ削除エラー: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * バッチ登録または更新のデモンストレーション
     */
    private static void demonstrateBatchInsertOrUpdate(SBJdbcManager jdbcManager) {
        System.out.println("=== バッチ登録または更新のデモ ===");

        List<TestSbUser> mixedUsers = Arrays.asList(
                TestSbUser.builder()  // 更新対象（既存ID）
                        .id(1003L)
                        .name("更新されたユーザー3")
                        .sequenceNo(3)
                        .amount(3500.0)
                        .isActive(true)
                        .updatedAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()  // 新規登録対象
                        .id(1004L)
                        .name("新規ユーザー4")
                        .sequenceNo(4)
                        .amount(4000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build(),
                TestSbUser.builder()  // 新規登録対象
                        .id(1005L)
                        .name("新規ユーザー5")
                        .sequenceNo(5)
                        .amount(5000.0)
                        .isActive(true)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build()
        );

        try {
            List<TestSbUser> results = jdbcManager.batchInsertOrUpdate(mixedUsers);
            System.out.println("バッチ登録または更新完了: " + results.size() + " 件");
            results.forEach(user -> System.out.println("  処理済み: " + user.getName() + " (ID: " + user.getId() + ")"));
        } catch (Exception e) {
            System.err.println("バッチ登録または更新エラー: " + e.getMessage());
        }

        System.out.println();
    }
}