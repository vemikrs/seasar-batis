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
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.Configuration;

import jp.vemi.seasarbatis.core.sql.dialect.SBDialect;
import jp.vemi.seasarbatis.core.sql.dialect.PostgresDialect;

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
 * @version 0.1.0
 * @since 2025/01/01
 */
public class SBMyBatisSqlProcessor {

    private final SBDialect dialect;

    /**
     * デフォルトコンストラクタ。PostgresDialectを使用します。
     */
    public SBMyBatisSqlProcessor() {
        this(new PostgresDialect());
    }

    /**
     * Dialectを指定するコンストラクタ。
     *
     * @param dialect 使用するDialect
     */
    public SBMyBatisSqlProcessor(SBDialect dialect) {
        this.dialect = dialect != null ? dialect : new PostgresDialect();
    }

    /**
     * SQLを処理します。
     *
     * @param sql SQL文
     * @param configuration MyBatis設定
     * @param parameters バインドパラメータ
     * @return バインド変数に値が代入されたSQL文字列
     */
    public String process(String sql, Configuration configuration, Map<String, Object> parameters) {
        List<ParameterMapping> parameterMappings = getParameterMappings(configuration, sql);
        String sqlWithPlaceholders = sql;

        // プレースホルダをパラメータの値に置換
        for (ParameterMapping mapping : parameterMappings) {
            Object value = parameters.get(mapping.getProperty());
            String replacement = formatParameter(value);
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
    private List<ParameterMapping> getParameterMappings(Configuration configuration, String sql) {
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
     * @return SQLに埋め込む形式の文字列
     */
    private String formatParameter(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return dialect.formatString(value.toString());
        }
        // JDBC型（java.sql系）を優先して個別にフォーマット
        if (value instanceof Timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedValue = sdf.format((Timestamp) value);
            return dialect.formatTimestamp(formattedValue);
        }
        if (value instanceof java.sql.Date && !(value instanceof Timestamp)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String formattedValue = sdf.format((java.sql.Date) value);
            return dialect.formatDate(formattedValue + " 00:00:00");
        }
        if (value instanceof Time) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String formattedValue = sdf.format((Time) value);
            return dialect.formatTimestamp("1970-01-01 " + formattedValue);
        }
        if (value instanceof java.util.Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedValue = sdf.format((java.util.Date) value);
            return dialect.formatDate(formattedValue);
        }
        if (value instanceof LocalDate) {
            DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE;
            String formattedValue = ((LocalDate) value).format(dtf);
            return dialect.formatDate(formattedValue + " 00:00:00");
        }
        if (value instanceof LocalDateTime) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedValue = ((LocalDateTime) value).format(dtf);
            return dialect.formatTimestamp(formattedValue);
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value.getClass().isArray()) {
            String formattedValue = Arrays.stream((Object[]) value)
                    .map(this::formatParameter)
                    .collect(Collectors.joining(", "));
            return dialect.formatArray(formattedValue);
        }
        if (value instanceof Collection) {
            String formattedValue = ((Collection<?>) value).stream()
                    .map(this::formatParameter)
                    .collect(Collectors.joining(", "));
            return dialect.formatArray(formattedValue);
        }
        return value.toString();
    }
}