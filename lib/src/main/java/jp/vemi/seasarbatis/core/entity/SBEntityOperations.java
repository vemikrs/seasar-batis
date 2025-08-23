/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.entity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.meta.SBColumnMeta;
import jp.vemi.seasarbatis.core.meta.SBTableMeta;
import jp.vemi.seasarbatis.exception.SBEntityException;
import jp.vemi.seasarbatis.exception.SBException;
import jp.vemi.seasarbatis.exception.SBIllegalStateException;

/**
 * エンティティ操作に関する共通機能を提供するユーティリティクラスです。
 * <p>
 * テーブル名の解決や主キー情報の取得など、エンティティに関連する 操作の共通実装を提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBEntityOperations {
    private static final Logger logger = LoggerFactory.getLogger(SBEntityOperations.class);

    /**
     * エンティティクラスからテーブル名を取得します。
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティクラス
     * @return テーブル名
     */
    public static <T> String getTableName(Class<T> entityClass) {
        SBTableMeta tableMeta = entityClass.getAnnotation(SBTableMeta.class);
        if (tableMeta != null) {
            String schema = tableMeta.schema();
            String tableName = tableMeta.name();
            return schema.isEmpty() ? tableName : schema + "." + tableName;
        }
        logger.warn("@SBTableMetaが見つかりません: {}", entityClass.getName());
        return entityClass.getSimpleName().toLowerCase();
    }

    /**
     * エンティティからパラメータマップを取得します。
     *
     * @param <T> エンティティの型
     * @param entity エンティティ
     * @return パラメータマップ
     */
    public static <T> Map<String, Object> getEntityParams(T entity) {

        Map<String, Object> params = new HashMap<>();
        Arrays.stream(entity.getClass().getDeclaredFields()).forEach(field -> {
            try {
                field.setAccessible(true);
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                if (columnMeta == null) {
                    throw new SBException("カラムメタ情報が不明です: " + field.getName());
                }

                String columnName = columnMeta.name();
                Object value = null;

                // boolean型のフィールドの場合、isXxx形式のgetterメソッドを試す
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    try {
                        String getterName = "is" + Character.toUpperCase(field.getName().charAt(0))
                                + field.getName().substring(1);
                        java.lang.reflect.Method getter = entity.getClass().getMethod(getterName);
                        value = getter.invoke(entity);
                    } catch (NoSuchMethodException e) {
                        // isXxx形式のgetterメソッドがない場合は、そのままfield.get()を試す
                        value = field.get(entity);
                    }
                } else {
                    value = field.get(entity);
                }
                params.put(columnName, value);
            } catch (Exception e) {
                throw new SBException("パラメータの取得に失敗しました", e);
            }
        });
        return params;
    }

    /**
     * エンティティクラスから主キー情報を取得します。
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティクラス
     * @return 主キー情報
     */
    public static <T> SBPrimaryKeyInfo getPrimaryKeyInfo(Class<T> entityClass) {
        List<java.lang.reflect.Field> pkFields = Arrays.stream(entityClass.getDeclaredFields()).filter(field -> {
            SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
            return columnMeta != null && columnMeta.primaryKey();
        }).collect(Collectors.toList());

        if (pkFields.isEmpty()) {
            throw new SBIllegalStateException("主キーが見つかりません: " + entityClass.getName());
        }

        List<String> pkColumnNames = pkFields.stream().map(field -> field.getAnnotation(SBColumnMeta.class).name())
                .collect(Collectors.toList());

        return new SBPrimaryKeyInfo(pkFields, pkColumnNames);
    }

    /**
     * エンティティから主キーの値を取得します。
     *
     * @param <T> エンティティの型
     * @param entity エンティティ
     * @return 主キーの値
     */
    public static <T> Map<String, Object> getPrimaryKeyValues(T entity) {
        SBPrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entity.getClass());
        Map<String, Object> pkValues = new HashMap<>();
        pkInfo.getFields().forEach(field -> {
            try {
                field.setAccessible(true);
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                if (columnMeta == null) {
                    throw new SBEntityException("カラムメタ情報が不明です: " + field.getName());
                }
                pkValues.put(columnMeta.name(), field.get(entity));
            } catch (Exception e) {
                throw new SBEntityException("主キーの値の取得に失敗しました", e);
            }
        });
        return pkValues;
    }
}