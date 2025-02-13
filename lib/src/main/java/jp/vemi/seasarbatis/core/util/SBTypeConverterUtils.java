/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.util;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MyBatis型ハンドラを使用したエンティティ変換ユーティリティクラスです。<br>
 * SQL実行結果のMapからエンティティへの変換処理を提供します。<br>
 * 変換に失敗した場合、例外を投げるかどうかをオプションで指定可能です。<br>
 * 
 * @author
 * @version 1.3.0
 * @since 2025/01/01
 */
public class SBTypeConverterUtils {
    private static final Logger logger = LoggerFactory.getLogger(SBTypeConverterUtils.class);

    /**
     * 指定されたMap形式の行データを対象のエンティティに変換します。<br>
     * 変換に失敗した場合は、例外を投げずに値をそのまま設定します。
     *
     * @param <T>           エンティティの型
     * @param row           SQL実行結果の1行分のデータ（カラム名と値のマップ）
     * @param entityClass   エンティティのクラス
     * @param configuration MyBatisのConfigurationオブジェクト
     * @return 変換後のエンティティ
     */
    public static <T> T convertRowToEntity(Map<String, Object> row, Class<T> entityClass, Configuration configuration) {
        return convertRowToEntity(row, entityClass, configuration, false);
    }

    /**
     * 指定されたMap形式の行データを対象のエンティティに変換します。<br>
     * throwOnError が true の場合、変換に失敗した際に例外を投げます。
     *
     * @param <T>           エンティティの型
     * @param row           SQL実行結果の1行分のデータ（カラム名と値のマップ）
     * @param entityClass   エンティティのクラス
     * @param configuration MyBatisのConfigurationオブジェクト
     * @param throwOnError  変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後のエンティティ
     */
    public static <T> T convertRowToEntity(Map<String, Object> row, Class<T> entityClass, Configuration configuration,
            boolean throwOnError) {
        T entity = configuration.getObjectFactory().create(entityClass);
        MetaObject metaObject = configuration.newMetaObject(entity);
        row.forEach((key, value) -> {
            if (metaObject.hasSetter(key)) {
                Class<?> setterType = metaObject.getSetterType(key);
                if (value != null && !setterType.isAssignableFrom(value.getClass())) {
                    value = convertValue(value, setterType, throwOnError);
                }
                metaObject.setValue(key, value);
            }
        });
        return entity;
    }

    /**
     * 値を指定されたターゲット型に変換します。<br>
     * 変換に失敗した場合は例外を投げず、元の値を返します。
     *
     * @param value      変換対象の値
     * @param targetType 変換先の型
     * @return 変換後の値
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        return convertValue(value, targetType, false);
    }

    /**
     * 値を指定されたターゲット型に変換します。<br>
     * throwOnError が true の場合、変換に失敗した際に例外を投げます。
     *
     * @param value        変換対象の値
     * @param targetType   変換先の型
     * @param throwOnError 変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後の値
     */
    public static Object convertValue(Object value, Class<?> targetType, boolean throwOnError) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        // 数値系の変換
        if (Number.class.isAssignableFrom(value.getClass()) || value instanceof BigDecimal) {
            return convertNumber((Number) value, targetType, throwOnError);
        }
        // Boolean変換
        if (targetType == Boolean.class || targetType == boolean.class) {
            return convertToBoolean(value);
        }
        // 文字列変換
        if (targetType == String.class) {
            return value.toString();
        }
        // 日付/時刻変換
        if (targetType == Timestamp.class) {
            return convertToTimestamp(value, throwOnError);
        }
        if (targetType == Date.class) {
            return convertToDate(value, throwOnError);
        }
        if (targetType == Time.class) {
            return convertToTime(value, throwOnError);
        }
        // それ以外の場合、toStringで生成
        return value.toString();
    }

    /**
     * 数値の型変換を行います。<br>
     * throwOnError が true の場合、変換不可時に例外を投げます。
     *
     * @param number       変換対象の数値
     * @param targetType   変換先の数値型
     * @param throwOnError 変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後の数値
     */
    private static Object convertNumber(Number number, Class<?> targetType, boolean throwOnError) {
        try {
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
        } catch (Exception e) {
            if (throwOnError) {
                throw new IllegalArgumentException("数値への変換に失敗しました: " + number, e);
            }
            logger.warn("数値への変換に失敗しました: {}", number);
        }
        return number;
    }

    /**
     * Boolean型への変換を行います。
     *
     * @param value 変換対象の値
     * @return 変換後の Boolean 値
     */
    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
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

    /**
     * Timestamp型への変換を行います。<br>
     * throwOnError が true の場合、変換不可時に例外を投げます。
     *
     * @param value        変換対象の値
     * @param throwOnError 変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後の Timestamp 値
     */
    private static Timestamp convertToTimestamp(Object value, boolean throwOnError) {
        try {
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
        } catch (Exception e) {
            if (throwOnError) {
                throw new IllegalArgumentException("Timestamp への変換に失敗しました: " + value, e);
            }
            logger.warn("Timestamp への変換に失敗しました: {}", value);
        }
        if (throwOnError) {
            throw new IllegalArgumentException("Timestamp への変換不可: " + value);
        }
        // 返却値は Timestamp 型でなければならないため、変換不可の場合は常に null を返す
        return null;
    }

    /**
     * java.sql.Date型への変換を行います。<br>
     * throwOnError が true の場合、変換不可時に例外を投げます。
     *
     * @param value        変換対象の値
     * @param throwOnError 変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後の Date 値
     */
    private static Date convertToDate(Object value, boolean throwOnError) {
        try {
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
        } catch (Exception e) {
            if (throwOnError) {
                throw new IllegalArgumentException("Date への変換に失敗しました: " + value, e);
            }
            logger.warn("Date への変換に失敗しました: {}", value);
        }
        if (throwOnError) {
            throw new IllegalArgumentException("Date への変換不可: " + value);
        }
        return null;
    }

    /**
     * Time型への変換を行います。<br>
     * throwOnError が true の場合、変換不可時に例外を投げます。
     *
     * @param value        変換対象の値
     * @param throwOnError 変換失敗時に例外を投げる場合は true、そうでなければ false
     * @return 変換後の Time 値
     */
    private static Time convertToTime(Object value, boolean throwOnError) {
        try {
            if (value instanceof Time) {
                return (Time) value;
            }
            if (value instanceof java.util.Date) {
                return new Time(((java.util.Date) value).getTime());
            }
            if (value instanceof Number) {
                return new Time(((Number) value).longValue());
            }
        } catch (Exception e) {
            if (throwOnError) {
                throw new IllegalArgumentException("Time への変換に失敗しました: " + value, e);
            }
            logger.warn("Time への変換に失敗しました: {}", value);
        }
        if (throwOnError) {
            throw new IllegalArgumentException("Time への変換不可: " + value);
        }
        return null;
    }
}