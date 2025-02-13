/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.sql.ParsedSql;

/**
 * SQLクエリの解析と変換を行うユーティリティクラス。
 * Seasar2形式のSQLコメントを解析し、実行可能なSQLに変換します。
 * 
 * <p>
 * 主な機能：
 * <ul>
 * <li>動的SQLの条件判定（IF条件）</li>
 * <li>バインドパラメータの解決</li>
 * <li>SQLの正規化</li>
 * </ul>
 * </p>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>{@code
 * String sql = "SELECT * FROM users WHERE &#47;*IF age != null*&#47; age > &#47;*age*&#47;20 &#47;*END*&#47;";
 * Map<String, Object> params = new HashMap<>();
 * params.put("age", 25);
 * ParsedSql parsedSql = SBSqlParser.parse(sql, params);
 * // 実行結果：
 * // SQL: SELECT * FROM users WHERE age > #{age}
 * // パラメータ: [25]
 * }</pre>
 * </p>
 * 
 * @author H.Kurosawa
 * @version 1.0.0
 */
public class SBSqlParser {
    private static final Logger logger = LoggerFactory.getLogger(SBSqlParser.class);

    // バインド変数のパターン定義
    private static final List<Pattern> BIND_PATTERNS = Arrays.asList(
            Pattern.compile("/\\*\\s*([^\\s*]*)\\s*\\*/['\"]([^'\"]*)['\"]"), // 文字列リテラル
            Pattern.compile("/\\*\\s*([^\\s*]*)\\s*\\*/(-?[0-9.]+)"), // 数値リテラル
            Pattern.compile("/\\*\\s*([^\\s*]*)\\s*\\*/\\s*\\(([^)]*)\\)"), // IN句
            Pattern.compile("/\\*\\s*([^\\s*]*)\\s*\\*/\\s*null"), // NULL値
            Pattern.compile("/\\*\\s*([^\\s*]*)\\s*\\*/([^\\s,)]+)") // その他
    );

    /**
     * SQLを解析し、バインドパラメータを解決します。
     * <p>
     * IF条件を評価した後、Seasar2形式のSQLコメントで記述されたバインド変数を
     * MyBatis形式の「#{paramName}」に書き換えます。<br>
     * 変換された順序でパラメータはリストとして保持されます。
     * </p>
     *
     * @param sql        解析対象のSQL文
     * @param parameters バインドパラメータ
     * @return 処理済みのSQL文と順序付けられたパラメータを保持するParsedSqlオブジェクト
     */
    public static ParsedSql parse(String sql, Map<String, Object> parameters) {
        // IF条件の評価
        String processedSql = processIfConditions(sql, parameters);
        // バインド変数の解決（MyBatis形式への置換）
        return processBindVariables(processedSql, parameters);
    }

    /**
     * バインド変数を解決します。<br>
     * Seasar2形式のバインド変数（パターン1～パターン5）をサポートし、<br>
     * 置換された順序で変数のリストをorderedParametersに追加します。<br>
     * この実装では、PreparedStatement用の「?」ではなく、MyBatis形式の「#{paramName}」に置換します。
     *
     * @param sql        SQL文
     * @param parameters バインドパラメータ
     * @return 処理済みのSQL文と順序付けられたパラメータを保持するParsedSqlオブジェクト
     */
    protected static ParsedSql processBindVariables(String sql, Map<String, Object> parameters) {
        String processedSql = sql;
        List<Object> orderedParams = new ArrayList<>();

        for (Pattern pattern : BIND_PATTERNS) {
            Matcher m = pattern.matcher(processedSql);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String paramName = m.group(1).trim();
                if (parameters.containsKey(paramName)) {
                    // MyBatis形式のバインド変数に置換
                    m.appendReplacement(sb, "#{" + paramName + "}");
                    // パラメータを順序通りに保持
                    orderedParams.add(parameters.get(paramName));
                }
            }
            m.appendTail(sb);
            processedSql = sb.toString();
        }

        // SQL文の整形
        processedSql = cleanupSql(processedSql);

        return ParsedSql.builder()
                .sql(processedSql)
                .build();
    }

    /**
     * IF条件を評価します。
     * <p>
     * ネストされた条件とBEGIN/ENDブロックをサポートします。
     * </p>
     *
     * @param sql        SQLクエリ文字列
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
     * @param condition  条件式文字列
     * @param parameters バインドパラメータ
     * @return 条件式の評価結果
     */
    protected static boolean evaluateCondition(String condition, Map<String, Object> parameters) {
        if (condition.contains(" AND ")) {
            String[] conditions = condition.split(" AND ");
            return Arrays.stream(conditions)
                    .allMatch(c -> evaluateSingleCondition(c.trim(), parameters));
        }
        if (condition.contains(" OR ")) {
            String[] conditions = condition.split(" OR ");
            return Arrays.stream(conditions)
                    .anyMatch(c -> evaluateSingleCondition(c.trim(), parameters));
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