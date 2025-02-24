/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.sql.ParsedSql;

/**
 * SQLの解析とバインドパラメータの解決を行うクラスです。
 * 
 * <p>
 * S2JDBC形式のSQLコメントを解析し、MyBatisで実行可能なSQLに変換します。 この変換では以下の処理を行います：
 * <ul>
 * <li>IF条件コメントの評価による動的SQL生成</li>
 * <li>バインド変数コメントの解決</li>
 * <li>BEGIN/ENDブロックの処理</li>
 * </ul>
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBSqlParser {
    private static final Logger logger = LoggerFactory.getLogger(SBSqlParser.class);

    /**
     * SQLを解析し、実行可能な形式に変換します。
     * 
     * <p>
     * 以下の順序で処理を行います：
     * <ol>
     * <li>IF条件コメントを評価し、条件に応じてSQL文を構築</li>
     * <li>バインド変数コメントをMyBatisの#{param}形式に変換</li>
     * </ol>
     * コメントの後に続く値は、型推論のためのダミー値として扱われ、 実際のSQLからは除去されます。
     * </p>
     *
     * @param sql SQLクエリ文字列
     * @param parameters バインドパラメータ
     * @return 変換されたSQLとパラメータ情報を含むParsedSqlオブジェクト
     */
    public static ParsedSql parse(String sql, Map<String, Object> parameters) {
        // IF条件の評価
        String processedSql = processIfConditions(sql, parameters);
        // バインド変数の解決（MyBatis形式への置換）
        return processBindVariables(processedSql, parameters);
    }

    /**
     * バインド変数コメントを解決します。
     * 
     * @param sql SQLクエリ文字列
     * @param parameters バインドパラメータ
     * @return 変換されたSQLとパラメータ情報を含むParsedSqlオブジェクト
     */
    protected static ParsedSql processBindVariables(String sql, Map<String, Object> parameters) {
        if (!sql.contains("/*")) {
            return ParsedSql.builder().sql(sql).build();
        }

        StringBuilder processedSql = new StringBuilder();
        List<String> paramNames = new ArrayList<>();

        int position = 0;

        while (true) {
            int commentStart = sql.indexOf("/*", position);
            if (commentStart < 0) {
                if (position < sql.length()) {
                    processedSql.append(sql.substring(position));
                }
                break;
            }

            String beforeComment = sql.substring(position, commentStart);

            if (!beforeComment.trim().isEmpty()) {
                processedSql.append(beforeComment);
            }

            int commentEnd = sql.indexOf("*/", commentStart + 2);
            if (commentEnd < 0) {
                throw new IllegalArgumentException("コメントが閉じられていません: " + sql);
            }

            String paramName = sql.substring(commentStart + 2, commentEnd).trim();
            paramNames.add(paramName);

            if (parameters.containsKey(paramName)) {
                processedSql.append("#{").append(paramName).append("} ");
            }

            position = skipTypeDummyValue(sql, commentEnd + 2);
        }

        String finalSql = processedSql.toString().replaceAll("\\s{2,}", " ").trim();

        return ParsedSql.builder().sql(finalSql).parameterNames(paramNames).build();
    }

    /**
     * 型推論用のダミー値をスキップします。
     * 
     * <p>
     * S2JDBCの仕様に従い、以下の処理を行います：
     * <ul>
     * <li>シングルクォートで囲まれた値は型推論用のダミー値として扱い、スキップします</li>
     * <li>数値やnullなどの直値もダミー値として扱い、スキップします</li>
     * <li>ダミー値が存在しない場合はエラーとしません</li>
     * </ul>
     * </p>
     *
     * @param sql SQLクエリ文字列
     * @param start 検索開始位置
     * @return スキップ後の位置
     */
    private static int skipTypeDummyValue(String sql, int start) {
        int i = start;
        if (i >= sql.length()) {
            return i;
        }

        char c = sql.charAt(i);

        // シングルクォートで囲まれた値のスキップ
        if (c == '\'') {
            i++;
            while (i < sql.length()) {
                if (sql.charAt(i) == '\'') {
                    return i + 1;
                }
                i++;
            }
        } else {
            // 空白、コンマ、括弧、セミコロン、シングルクォート以外の文字をスキップ
            while (i < sql.length()) {
                c = sql.charAt(i);
                if (Character.isWhitespace(c) || c == ',' || c == ')' || c == '(' || c == ';' || c == '\'') {
                    break;
                }
                i++;
            }
        }
        return i;
    }

    /**
     * IF条件を評価します。
     * <p>
     * ネストされた条件とBEGIN/ENDブロックをサポートします。
     * </p>
     *
     * @param sql SQLクエリ文字列
     * @param parameters バインドパラメータ
     * @return IF条件の評価結果を反映したSQL文字列
     */
    protected static String processIfConditions(String sql, Map<String, Object> parameters) {
        StringBuilder result = new StringBuilder();
        String[] lines = sql.split("\n");
        Stack<Boolean> ifStack = new Stack<>();
        Stack<Boolean> beginStack = new Stack<>();
        beginStack.push(true);

        String pendingLine = null;
        boolean isInIf = false;

        for (String line : lines) {
            line = line.trim();
            logger.trace("Processing line: {}", line);

            if (line.contains("/*BEGIN*/")) {
                beginStack.push(true);
                continue;
            }

            if (line.contains("/*END*/")) {
                if (isInIf) {
                    if (!ifStack.isEmpty() && ifStack.peek() && pendingLine != null) {
                        result.append(pendingLine).append("\n");
                    }
                    ifStack.pop();
                    isInIf = false;
                    pendingLine = null;
                } else if (!beginStack.isEmpty()) {
                    beginStack.pop();
                }
                continue;
            }

            if (line.contains("/*IF")) {
                String condition = extractCondition(line);
                boolean conditionResult = evaluateCondition(condition, parameters);
                logger.trace("Condition: {} => {}", condition, conditionResult);
                ifStack.push(conditionResult);
                isInIf = true;
                continue;
            }

            if (isInIf) {
                pendingLine = line;
            } else if (!ifStack.contains(false)) {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 条件式を抽出します。
     *
     * @param line 条件式を含む行
     * @return 抽出された条件式
     */
    protected static String extractCondition(String line) {
        int start = line.indexOf("/*IF") + 4;
        int end = line.indexOf("*/", start);
        return line.substring(start, end).trim();
    }

    /**
     * 条件式を評価します。
     * <p>
     * AND/OR演算子をサポートします。
     * </p>
     *
     * @param condition 条件式文字列
     * @param parameters バインドパラメータ
     * @return 条件式の評価結果
     */
    protected static boolean evaluateCondition(String condition, Map<String, Object> parameters) {
        if (condition.contains(" AND ")) {
            String[] conditions = condition.split(" AND ");
            return Arrays.stream(conditions).allMatch(c -> evaluateSingleCondition(c.trim(), parameters));
        }
        if (condition.contains(" OR ")) {
            String[] conditions = condition.split(" OR ");
            return Arrays.stream(conditions).anyMatch(c -> evaluateSingleCondition(c.trim(), parameters));
        }
        return evaluateSingleCondition(condition, parameters);
    }

    /**
     * SQL文を整形します。
     * <p>
     * 不要なコメントの削除、連続する空白の除去などを行います。
     * </p>
     *
     * @param sql 整形対象のSQL文
     * @return 整形されたSQL文
     */
    protected static String cleanupSql(String sql) {
        return sql.replaceAll("/\\*BEGIN\\*/", "") // BEGINコメントの削除
                .replaceAll("/\\*END\\*/", "") // ENDコメントの削除
                .replaceAll("\\s+", " ") // 連続する空白の削除
                .trim();
    }

    private static boolean evaluateSingleCondition(String condition, Map<String, Object> parameters) {
        String[] parts = condition.split("\\s+");
        if (parts.length < 2)
            return false;

        String paramName = parts[0];
        String operator = parts.length > 2 ? parts[1] : parts[1].equals("null") ? "null" : "!null";
        String value = parts.length > 2 ? parts[2] : null;

        Object paramValue = parameters.get(paramName);

        switch (operator) {
        case "==":
        case "=":
            return String.valueOf(paramValue).equals(value);
        case "!=":
            return !String.valueOf(paramValue).equals(value);
        case ">":
            return compareValues(paramValue, value) > 0;
        case "<":
            return compareValues(paramValue, value) < 0;
        case ">=":
            return compareValues(paramValue, value) >= 0;
        case "<=":
            return compareValues(paramValue, value) <= 0;
        case "null":
            return paramValue == null;
        case "!null":
            return paramValue != null;
        default:
            return false;
        }
    }

    private static int compareValues(Object value1, String value2) {
        if (value1 instanceof Number && value2.matches("-?\\d+(\\.\\d+)?")) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = Double.parseDouble(value2);
            return Double.compare(d1, d2);
        }
        return String.valueOf(value1).compareTo(value2);
    }
}