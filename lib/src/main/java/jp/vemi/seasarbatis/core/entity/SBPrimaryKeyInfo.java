/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.entity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import jp.vemi.seasarbatis.core.meta.SBColumnMeta;
import jp.vemi.seasarbatis.exception.SBEntityException;

/**
 * エンティティの主キー情報を保持するクラスです。
 * <p>
 * 主キーのフィールド情報とカラム名の対応関係を管理し、
 * 主キー値の取得や検証などの機能を提供します。
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/08/23
 */
public class SBPrimaryKeyInfo {
    
    private final List<Field> fields;
    private final List<String> columnNames;

    /**
     * 主キー情報を構築します。
     *
     * @param fields 主キーフィールドのリスト
     * @param columnNames 主キーカラム名のリスト
     */
    public SBPrimaryKeyInfo(List<Field> fields, List<String> columnNames) {
        this.fields = fields;
        this.columnNames = columnNames;
    }

    /**
     * エンティティから主キーの値を取得します。
     *
     * @param <T> エンティティの型
     * @param entity エンティティ
     * @return 主キーの値のマップ（カラム名, 値）
     * @throws SBEntityException 主キー値の取得に失敗した場合
     */
    public <T> Map<String, Object> getPrimaryKeyValues(T entity) {
        Map<String, Object> pkValues = new HashMap<>();
        fields.forEach(field -> {
            try {
                field.setAccessible(true);
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                pkValues.put(columnMeta.name(), field.get(entity));
            } catch (Exception e) {
                throw new SBEntityException("主キーの値の取得に失敗しました", e);
            }
        });
        return pkValues;
    }

    /**
     * 主キーフィールドのリストを取得します。
     *
     * @return 主キーフィールドのリスト
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * 主キーカラム名のリストを取得します。
     *
     * @return 主キーカラム名のリスト
     */
    public List<String> getColumnNames() {
        return columnNames;
    }
}