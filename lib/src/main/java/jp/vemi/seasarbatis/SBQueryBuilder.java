/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLクエリの構築と処理を行うユーティリティクラス。
 * SQLファイルの読み込みと動的SQLの処理を提供します。
 * 
 * <p>
 * 主な機能：
 * <ul>
 * <li>SQLファイルの読み込み</li>
 * <li>動的SQLの処理</li>
 * <li>バインドパラメータの解決</li>
 * </ul>
 * 
 * <p>
 * 使用例：
 * 
 * <pre>{@code
 * String sql = SBQueryBuilder.loadSQLFromFile("queries/findUser.sql");
 * Map<String, Object> params = new HashMap<>();
 * params.put("userId", 1);
 * String processedSql = SBQueryBuilder.processSQL(sql, sqlSessionFactory, params);
 * }</pre>
 */
public class SBQueryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SBQueryBuilder.class);

    /**
     * SQLをビルドし、実行可能な形式に変換します。
     */
    public static String build(String sql, SqlSessionFactory sqlSessionFactory, Map<String, Object> parameters) {
        return processSQL(sql, sqlSessionFactory, parameters);
    }

    /**
     * 指定されたパスのSQLファイルを読み込みます。
     *
     * @param filePath SQLファイルのパス（クラスパスからの相対パス）
     * @return 読み込まれたSQL文字列
     * @throws IOException ファイルの読み込みに失敗した場合
     */
    public static String loadSQLFromFile(String filePath) throws IOException {
        Reader reader = Resources.getResourceAsReader(filePath);
        StringBuilder sql = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;
        while ((bytesRead = reader.read(buffer)) != -1) {
            sql.append(buffer, 0, bytesRead);
        }
        reader.close();
        return sql.toString();
    }

    /**
     * SQL文を処理し、バインドパラメータを解決します。
     *
     * @param sql               処理対象のSQL文
     * @param sqlSessionFactory SQLセッションファクトリー
     * @param parameters        バインドパラメータ
     * @return 処理済みのSQL文
     */
    public static String processSQL(String sql, SqlSessionFactory sqlSessionFactory, Map<String, Object> parameters) {
        SqlSource sqlSource = new DynamicSqlSource(sqlSessionFactory.getConfiguration(), new TextSqlNode(sql));
        MappedStatement.Builder msBuilder = new MappedStatement.Builder(sqlSessionFactory.getConfiguration(),
                "dynamicSQL", sqlSource, SqlCommandType.SELECT);
        MappedStatement ms = msBuilder.build();
        BoundSql boundSql = ms.getBoundSql(parameters);
        return boundSql.getSql();
    }

    /**
     * SQL文を処理し、バインドパラメータを解決します。
     * Seasar2形式のバインド変数をサポートします。
     */
    public static String processSQL(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }

        // IF条件の評価を実行
        String processedSql = processIfConditions(sql, parameters);
        logger.trace("After IF processing: {}", processedSql);

        // バインド変数の処理
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();

            // パターン1: クォートで囲まれた値
            String quotedPattern = "/\\*" + paramName + "\\*/['\"][^'\"]*['\"]";
            // パターン2: 数値リテラル
            String numberPattern = "/\\*" + paramName + "\\*/*-?[0-9.]+";
            // パターン3: IN句のリスト
            String inListPattern = "/\\*\\s*" + paramName + "\\s*\\*/\\s*\\([^)]*\\)";
            // パターン4: LIKEパターン
            String likePattern = "/\\*\\s*" + paramName + "\\s*\\*/\\s*['\"][%_]*[^'\"]*[%_]*['\"]";
            // パターン5: NULL値
            String nullPattern = "/\\*\\s*" + paramName + "\\s*\\*/\\s*null";

            processedSql = processedSql.replaceAll(quotedPattern, "?")
                    .replaceAll(numberPattern, "?")
                    .replaceAll(inListPattern, "?")
                    .replaceAll(likePattern, "?")
                    .replaceAll(nullPattern, "?");

        }

        // 不要なコメントの除去
        processedSql = processedSql.replaceAll("/\\*BEGIN\\*/", "")
                .replaceAll("/\\*END\\*/", "")
                .replaceAll("\\s+", " ")
                .trim();

        logger.info("Final SQL: {}", processedSql);
        return processedSql;
    }

    /**
     * IF条件を評価します。
     * ネストされた条件とBEGIN/ENDブロックをサポートします。
     *
     * @param sql        SQLクエリ文字列
     * @param parameters バインドパラメータ
     * @return 処理済みのSQL文字列
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
     */
    protected static String extractCondition(String line) {
        int start = line.indexOf("/*IF") + 4;
        int end = line.indexOf("*/", start);
        return line.substring(start, end).trim();
    }

    /**
     * 条件式を評価します。
     * AND/OR演算子をサポートします。
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