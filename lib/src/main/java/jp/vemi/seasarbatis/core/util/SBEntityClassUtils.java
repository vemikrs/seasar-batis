/*
 * Copyright(c) 2025 VEMI All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import jp.vemi.seasarbatis.core.meta.SBColumnMeta;

/**
 * @deprecated このクラスは廃止予定です。代わりに
 *             {@link jp.vemi.seasarbatis.core.util.SBTypeConverterUtils}
 *             を使用してください。
 */
public class SBEntityClassUtils {

    public static <T> T mapToEntity(Class<T> entityClass, Map<String, Object> row) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                if (columnMeta != null) {
                    String columnName = columnMeta.name();
                    if (row.containsKey(columnName)) {
                        Object value = row.get(columnName);
                        Object convertedValue = convertValue(value, field.getType());
                        field.setAccessible(true);
                        field.set(entity, convertedValue);
                    }
                }
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("エンティティへのマッピングに失敗しました", e);
        }
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        // 数値系の変換
        if (Number.class.isAssignableFrom(value.getClass()) || value instanceof BigDecimal) {
            return convertNumber((Number) value, targetType);
        }
        // Boolean変換
        if ((targetType == Boolean.class || targetType == boolean.class)) {
            return convertToBoolean(value);
        }
        // 文字列変換
        if (targetType == String.class) {
            return value.toString();
        }
        // 日付/時刻変換
        if (targetType == Timestamp.class) {
            return convertToTimestamp(value);
        }
        if (targetType == Date.class) {
            return convertToDate(value);
        }
        if (targetType == Time.class) {
            return convertToTime(value);
        }
        // その他はtoStringで試す
        return value.toString();
    }

    private static Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == Long.class || targetType == long.class) {
            return number.longValue();
        }
        if (targetType == Integer.class || targetType == int.class) {
            return number.intValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return number.doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return number.floatValue();
        }
        if (targetType == Short.class || targetType == short.class) {
            return number.shortValue();
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return number.byteValue();
        }
        return number;
    }

    private static Object convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("y");
        }
        return false;
    }

    private static Timestamp convertToTimestamp(Object value) {
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            return new Timestamp(((Number) value).longValue());
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        throw new IllegalArgumentException("Timestampへ変換不可: " + value);
    }

    private static Date convertToDate(Object value) {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof java.util.Date) {
            return new Date(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof LocalDateTime) {
            return new Date(Timestamp.valueOf((LocalDateTime) value).getTime());
        }
        throw new IllegalArgumentException("Dateへ変換不可: " + value);
    }

    private static Time convertToTime(Object value) {
        if (value instanceof Time) {
            return (Time) value;
        }
        if (value instanceof java.util.Date) {
            return new Time(((java.util.Date) value).getTime());
        }
        if (value instanceof Number) {
            return new Time(((Number) value).longValue());
        }
        throw new IllegalArgumentException("Timeへ変換不可: " + value);
    }
}
