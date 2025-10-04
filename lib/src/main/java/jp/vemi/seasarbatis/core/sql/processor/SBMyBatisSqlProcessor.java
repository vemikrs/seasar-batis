/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.Configuration;

/**
 * MyBatisの機能を使用してSQLを処理し、パラメータの値を埋め込んだSQL文字列を返します。
 * <p>
 * このクラスは、MyBatisのSQL（例：SELECT * FROM sbtest_users WHERE id = #{id}）から、
 * バインド変数に値を代入した実行SQLに変換します。置換後は、バインド変数部分が実際の値となります。
 * 例えば、SQL「SELECT * FROM sbtest_users WHERE id = #{id}」に対し、
 * パラメータ {@code id=1} を指定すると、実行SQLは「SELECT * FROM sbtest_users WHERE id =
 * 1」となります。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBMyBatisSqlProcessor {

    /**
     * SQLを処理します。
     *
     * @param sql SQL文
     * @param configuration MyBatis設定
     * @param parameters バインドパラメータ
     * @return バインド変数に値が代入されたSQL文字列
     */
    public static String process(String sql, Configuration configuration, Map<String, Object> parameters) {
        List<ParameterMapping> parameterMappings = getParameterMappings(configuration, sql);
        String sqlWithPlaceholders = sql;

        // プレースホルダをパラメータの値に置換
        Dialect dialect = loadDialect();
        for (ParameterMapping mapping : parameterMappings) {
            Object value = parameters.get(mapping.getProperty());
            String replacement = formatParameter(value, dialect);
            // MyBatisのプレースホルダをエスケープして1回分の置換を実施
            String property = "\\#\\{" + mapping.getProperty() + "\\}";
            sqlWithPlaceholders = sqlWithPlaceholders.replaceFirst(property, Matcher.quoteReplacement(replacement));
        }
        return sqlWithPlaceholders;
    }

    /**
     * パラメータマッピングを取得します。
     *
     * @param configuration MyBatis設定
     * @param sql               SQL文
     * @return パラメータマッピングリスト
     */
    private static List<ParameterMapping> getParameterMappings(Configuration configuration, String sql) {
        DynamicSqlSource sqlSource = new DynamicSqlSource(configuration, new TextSqlNode(sql));
        // ダミーのMappedStatementを生成
        MappedStatement ms = new MappedStatement.Builder(configuration, "dummy", sqlSource, SqlCommandType.SELECT)
                .build();
        return ms.getBoundSql(null).getParameterMappings();
    }

    /**
     * パラメータ値をSQL挿入用の文字列に変換します。
     * <p>
     * 文字列の場合はシングルクォートで囲み、必要に応じてエスケープ処理を行います。
     * 日付型（java.util.Date、java.time.LocalDate、java.time.LocalDateTime）については、
     * 標準的なフォーマットで文字列に変換し、シングルクォートで囲みます。
     * </p>
     *
     * @param value   パラメータ値
     * @param dialect SQLの方言
     * @return SQLに埋め込む形式の文字列
     */
    private static String formatParameter(Object value, Dialect dialect) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            // 文字列をエスケープ
            String escapedValue = escapeSql(value.toString(), dialect);
            // シングルクォートで囲んでエスケープ
            return dialect != null ? dialect.escapeString(escapedValue) : "'" + escapedValue + "'";
        }
        // JDBC型（java.sql系）を優先して個別にフォーマット
        if (value instanceof Timestamp) {
            // TIMESTAMP => yyyy-MM-dd HH:mm:ss
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedValue = sdf.format((Timestamp) value);
            return dialect != null ? dialect.formatDate(formattedValue) : "'" + formattedValue + "'";
        }
    if (value instanceof java.sql.Date && !(value instanceof Timestamp)) {
            // java.sql.Date は java.util.Date のサブクラスのため、順序に注意
            // DATE => yyyy-MM-dd
            if (value instanceof java.sql.Date) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String formattedValue = sdf.format((java.sql.Date) value);
                return dialect != null ? dialect.formatLocalDate(formattedValue) : "'" + formattedValue + "'";
            }
        }
        if (value instanceof Time) {
            // TIME => HH:mm:ss
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String formattedValue = sdf.format((Time) value);
            return dialect != null ? dialect.formatLocalDateTime(formattedValue) : "'" + formattedValue + "'";
        }
    if (value instanceof java.util.Date) {
            // java.util.Date（汎用） => yyyy-MM-dd HH:mm:ss
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedValue = sdf.format((java.util.Date) value);
            return dialect != null ? dialect.formatDate(formattedValue) : "'" + formattedValue + "'";
        }
        if (value instanceof LocalDate) {
            // java.time.LocalDate を ISO_LOCAL_DATE 形式 (yyyy-MM-dd) でフォーマット
            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;
            String formattedValue = ((LocalDate) value).format(dtf);
            return dialect != null ? dialect.formatLocalDate(formattedValue) : "'" + formattedValue + "'";
        }
        if (value instanceof LocalDateTime) {
            // java.time.LocalDateTime を yyyy-MM-dd HH:mm:ss 形式でフォーマット
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedValue = ((LocalDateTime) value).format(dtf);
            return dialect != null ? dialect.formatLocalDateTime(formattedValue) : "'" + formattedValue + "'";
        }
        if (value instanceof Number) {
            // 数値型はそのまま文字列に変換
            return value.toString();
        }
        if (value instanceof Boolean) {
            // Boolean型はtrueまたはfalseを文字列に変換
            return value.toString();
        }
        if (value.getClass().isArray()) {
            // 配列の場合、各要素をフォーマットしてカンマ区切りで連結
            String formattedValue = Arrays.stream((Object[]) value)
                    .map(v -> formatParameter(v, dialect))
                    .collect(Collectors.joining(", "));
            return "(" + formattedValue + ")";
        }
        if (value instanceof Collection) {
            // Collectionの場合、各要素をフォーマットしてカンマ区切りで連結
            String formattedValue = ((Collection<?>) value).stream()
                    .map(v -> formatParameter(v, dialect))
                    .collect(Collectors.joining(", "));
            return "(" + formattedValue + ")";
        }
        // 上記以外の型はtoString()で変換
        return value.toString();
    }

    /**
     * SQLインジェクション対策のエスケープ処理を行います。
     *
     * @param value   エスケープする文字列
     * @param dialect SQLの方言
     * @return エスケープされた文字列
     */
    private static String escapeSql(String value, Dialect dialect) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (dialect != null) {
            return dialect.escapeString(value);
        }

        // デフォルトのエスケープ処理
        String escapedValue = value.replace("'", "''");
        escapedValue = escapedValue.replace("\\", "\\\\");
        return escapedValue;
    }

    /**
     * データベースの方言をロードします。
     *
     * @return データベースの方言
     */
    private static Dialect loadDialect() {
        ServiceLoader<Dialect> loader = ServiceLoader.load(Dialect.class);
        return loader.findFirst().orElse(null);
    }

    /**
     * SQLの方言インターフェース
     */
    public interface Dialect {
        /**
         * 文字列をエスケープします。
         *
         * @param value エスケープする文字列
         * @return エスケープされた文字列
         */
        String escapeString(String value);

        /**
         * 日付をフォーマットします。
         *
         * @param value フォーマットする日付
         * @return フォーマットされた日付
         */
        String formatDate(String value);

        /**
         * LocalDateをフォーマットします。
         *
         * @param value フォーマットするLocalDate
         * @return フォーマットされたLocalDate
         */
        String formatLocalDate(String value);

        /**
         * LocalDateTimeをフォーマットします。
         *
         * @param value フォーマットするLocalDateTime
         * @return フォーマットされたLocalDateTime
         */
        String formatLocalDateTime(String value);
    }
}