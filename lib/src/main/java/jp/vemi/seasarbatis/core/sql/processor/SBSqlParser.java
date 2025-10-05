/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.core.sql.processor;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jp.vemi.seasarbatis.core.sql.ParsedSql;
import jp.vemi.seasarbatis.exception.SBSqlParseException;

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
        Parser parser = new Parser(sql);
        List<Node> nodes = parser.parseNodes(false);
        Renderer renderer = new Renderer(parameters);
        for (Node node : nodes) {
            renderer.render(node);
        }
        return ParsedSql.builder()
                .sql(renderer.getSql())
                .parameterNames(renderer.getParameterNames())
                .parameterValues(renderer.getParameterValues())
                .build();
    }

    private interface Node {
        RenderOutput render(Map<String, Object> parameters);
    }

    private static final class RenderOutput {
        private static final RenderOutput EMPTY = new RenderOutput("", Collections.emptyList(), false,
                Collections.emptyMap());

        private final String sql;
        private final List<String> parameterNames;
        private final boolean dynamic;
        private final Map<String, Object> parameterValues;

        private RenderOutput(String sql, List<String> parameterNames, boolean dynamic,
                Map<String, Object> parameterValues) {
            this.sql = sql;
            this.parameterNames = parameterNames;
            this.dynamic = dynamic;
            this.parameterValues = parameterValues;
        }
    }

    private static final class TextNode implements Node {
        private final String text;

        private TextNode(String text) {
            this.text = text;
        }

        @Override
        public RenderOutput render(Map<String, Object> parameters) {
            return new RenderOutput(text, Collections.emptyList(), false, Collections.emptyMap());
        }
    }

    private static final class PlaceholderNode implements Node {
        private final String name;
        private final String defaultLiteral;

        private PlaceholderNode(String name, String defaultLiteral) {
            this.name = name;
            this.defaultLiteral = defaultLiteral == null ? "" : defaultLiteral;
        }

        @Override
        public RenderOutput render(Map<String, Object> parameters) {
            boolean hasParam = parameters != null && parameters.containsKey(name);
            if (hasParam) {
                Map<String, Object> safeParameters = Objects.requireNonNull(parameters);
                Object value = safeParameters.get(name);
                if (isCollectionLike(value)) {
                    return renderCollectionValues(value);
                }
                Map<String, Object> values = new LinkedHashMap<>();
                values.put(name, value);
                return new RenderOutput("#{" + name + "}", Collections.singletonList(name), true, values);
            }
            if (!defaultLiteral.isEmpty()) {
                return new RenderOutput(defaultLiteral, Collections.emptyList(), false, Collections.emptyMap());
            }
            return RenderOutput.EMPTY;
        }

        private RenderOutput renderCollectionValues(Object value) {
            List<Object> elements = toElementList(value);
            if (elements.isEmpty()) {
                if (!defaultLiteral.isEmpty()) {
                    return new RenderOutput(defaultLiteral, Collections.emptyList(), false, Collections.emptyMap());
                }
                return RenderOutput.EMPTY;
            }

            Map<String, Object> expandedValues = new LinkedHashMap<>();
            List<String> names = new ArrayList<>();
            List<String> placeholders = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                String elementName = name + "_" + i;
                names.add(elementName);
                placeholders.add("#{" + elementName + "}");
                expandedValues.put(elementName, elements.get(i));
            }

            String joined = String.join(", ", placeholders);
            String segment = shouldWrapWithParentheses() ? "(" + joined + ")" : joined;
            return new RenderOutput(segment,
                    names,
                    true,
                    expandedValues);
        }

        private boolean shouldWrapWithParentheses() {
            return true;
        }

        private boolean isCollectionLike(Object value) {
            if (value == null) {
                return false;
            }
            if (value instanceof Collection<?>) {
                return true;
            }
            return value.getClass().isArray() && !(value instanceof byte[]) && !(value instanceof char[]);
        }

        private List<Object> toElementList(Object value) {
            if (value instanceof Collection<?>) {
                return new ArrayList<>((Collection<?>) value);
            }
            if (value != null && value.getClass().isArray()) {
                int length = Array.getLength(value);
                List<Object> list = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    list.add(Array.get(value, i));
                }
                return list;
            }
            return Collections.emptyList();
        }
    }

    private static final class BeginNode implements Node {
        private final List<Node> children;

        private BeginNode(List<Node> children) {
            this.children = children;
        }

        @Override
        public RenderOutput render(Map<String, Object> parameters) {
            RenderOutput content = renderChildren(children, parameters);
            if (content.sql.isBlank()) {
                return RenderOutput.EMPTY;
            }
            String normalized = content.sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
            if (!content.dynamic && ("WHERE 1=1".equals(normalized) || "WHERE 1 = 1".equals(normalized))) {
                return RenderOutput.EMPTY;
            }
            return new RenderOutput(content.sql,
                    content.parameterNames,
                    content.dynamic,
                    content.parameterValues);
        }
    }

    private static final class IfNode implements Node {
        private final String condition;
        private final List<Node> children;

        private IfNode(String condition, List<Node> children) {
            this.condition = condition;
            this.children = children;
        }

        @Override
        public RenderOutput render(Map<String, Object> parameters) {
            boolean result = ConditionEvaluator.evaluate(condition, parameters);
            if (!result) {
                return RenderOutput.EMPTY;
            }
            RenderOutput content = renderChildren(children, parameters);
            return new RenderOutput(content.sql,
                    content.parameterNames,
                    true,
                    content.parameterValues);
        }
    }

    private static final class Renderer {
        private final Map<String, Object> parameters;
        private final StringBuilder sql = new StringBuilder();
        private final List<String> parameterNames = new ArrayList<>();
        private final Map<String, Object> parameterValues = new LinkedHashMap<>();

        private Renderer(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        private void render(Node node) {
            RenderOutput output = node.render(parameters);
            if (!output.sql.isEmpty()) {
                sql.append(output.sql);
            }
            if (!output.parameterNames.isEmpty()) {
                parameterNames.addAll(output.parameterNames);
            }
            if (!output.parameterValues.isEmpty()) {
                parameterValues.putAll(output.parameterValues);
            }
        }

        private String getSql() {
            return sql.toString();
        }

        private List<String> getParameterNames() {
            return parameterNames;
        }

        private Map<String, Object> getParameterValues() {
            return parameterValues;
        }
    }

    private static RenderOutput renderChildren(List<Node> children, Map<String, Object> parameters) {
        if (children == null || children.isEmpty()) {
            return RenderOutput.EMPTY;
        }
        StringBuilder buffer = new StringBuilder();
        List<String> names = new ArrayList<>();
        boolean dynamic = false;
        Map<String, Object> values = new LinkedHashMap<>();
        for (Node child : children) {
            RenderOutput output = child.render(parameters);
            if (!output.sql.isEmpty()) {
                buffer.append(output.sql);
            }
            if (!output.parameterNames.isEmpty()) {
                names.addAll(output.parameterNames);
            }
            if (output.dynamic) {
                dynamic = true;
            }
            if (!output.parameterValues.isEmpty()) {
                values.putAll(output.parameterValues);
            }
        }
        if (buffer.length() == 0) {
            return RenderOutput.EMPTY;
        }
        return new RenderOutput(buffer.toString(),
                names.isEmpty() ? Collections.emptyList() : names,
                dynamic,
                values.isEmpty() ? Collections.emptyMap() : values);
    }

    private static final class Parser {
        private final String sql;
        private int index;

        private Parser(String sql) {
            this.sql = sql;
        }

        private List<Node> parseNodes(boolean inBlock) {
            List<Node> nodes = new ArrayList<>();
            StringBuilder textBuffer = new StringBuilder();
            while (index < sql.length()) {
                if (peek("/*")) {
                    flushText(nodes, textBuffer);
                    int commentEnd = sql.indexOf("*/", index + 2);
                    if (commentEnd < 0) {
                        throw new SBSqlParseException("SQLコメントが正しく閉じられていません");
                    }
                    String body = sql.substring(index + 2, commentEnd).trim();
                    index = commentEnd + 2;
                    String upperBody = body.toUpperCase(Locale.ROOT);
                    if ("BEGIN".equals(upperBody)) {
                        List<Node> children = parseNodes(true);
                        nodes.add(new BeginNode(children));
                    } else if ("END".equals(upperBody)) {
                        if (!inBlock) {
                            throw new SBSqlParseException("対応するBEGIN/IFが存在しません: " + sql);
                        }
                        return nodes;
                    } else if (upperBody.startsWith("IF ")) {
                        String condition = body.substring(2).trim();
                        List<Node> children = parseNodes(true);
                        nodes.add(new IfNode(condition, children));
                    } else {
                        nodes.add(parsePlaceholderNode(body));
                    }
                } else {
                    textBuffer.append(sql.charAt(index));
                    index++;
                }
            }
            if (inBlock) {
                throw new SBSqlParseException("/*END*/ が不足しています: " + sql);
            }
            flushText(nodes, textBuffer);
            return nodes;
        }

        private void flushText(List<Node> nodes, StringBuilder textBuffer) {
            if (textBuffer.length() > 0) {
                nodes.add(new TextNode(textBuffer.toString()));
                textBuffer.setLength(0);
            }
        }

        private boolean peek(String value) {
            return sql.startsWith(value, index);
        }

        private Node parsePlaceholderNode(String body) {
            if (body.isEmpty()) {
                throw new SBSqlParseException("空のSQLコメントが存在します: " + sql);
            }
            String name = body.trim();
            String defaultLiteral = captureDefaultLiteral();
            return new PlaceholderNode(name, defaultLiteral);
        }

        private String captureDefaultLiteral() {
            int start = index;
            if (start >= sql.length()) {
                return "";
            }

            char first = sql.charAt(start);
            if (Character.isWhitespace(first)) {
                return "";
            }

            int end = start;
            if (first == '\'') {
                end = parseStringLiteral(start);
            } else if (first == '(') {
                end = parseParentheses(start);
            } else {
                while (end < sql.length()) {
                    char c = sql.charAt(end);
                    if (Character.isWhitespace(c) || c == ',' || c == ')' || c == ';') {
                        break;
                    }
                    end++;
                }
            }

            String literal = sql.substring(start, end);
            index = end;
            return literal;
        }

        private int parseStringLiteral(int start) {
            int pos = start + 1;
            while (pos < sql.length()) {
                char c = sql.charAt(pos);
                if (c == '\'') {
                    if (pos + 1 < sql.length() && sql.charAt(pos + 1) == '\'') {
                        pos += 2;
                        continue;
                    }
                    return pos + 1;
                }
                pos++;
            }
            return sql.length();
        }

        private int parseParentheses(int start) {
            int depth = 1;
            int pos = start + 1;
            while (pos < sql.length() && depth > 0) {
                char c = sql.charAt(pos);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                pos++;
            }
            return pos;
        }
    }

    private static final class ConditionEvaluator {
        private final String expression;
        private final Map<String, Object> parameters;
        private int index;

        private ConditionEvaluator(String expression, Map<String, Object> parameters) {
            this.expression = expression;
            this.parameters = parameters;
        }

        private static boolean evaluate(String expression, Map<String, Object> parameters) {
            if (expression == null || expression.trim().isEmpty()) {
                throw new SBSqlParseException("空の条件式が指定されました");
            }
            ConditionEvaluator evaluator = new ConditionEvaluator(expression, parameters);
            boolean result = evaluator.parseOr();
            evaluator.skipWhitespace();
            if (!evaluator.isEnd()) {
                String remaining = evaluator.expression.substring(evaluator.index).trim();
                if (!remaining.isEmpty()) {
                    throw new SBSqlParseException(
                            "条件式の解析に失敗しました: " + expression + " (未処理: " + remaining + ")");
                }
                throw new SBSqlParseException("条件式の解析に失敗しました: " + expression);
            }
            return result;
        }

        private boolean parseOr() {
            boolean value = parseAnd();
            while (true) {
                skipWhitespace();
                if (matchKeyword("OR")) {
                    boolean right = parseAnd();
                    value = value || right;
                } else {
                    break;
                }
            }
            return value;
        }

        private boolean parseAnd() {
            boolean value = parsePrimary();
            while (true) {
                skipWhitespace();
                if (matchKeyword("AND")) {
                    boolean right = parsePrimary();
                    value = value && right;
                } else {
                    break;
                }
            }
            return value;
        }

        private boolean parsePrimary() {
            skipWhitespace();
            if (match('(')) {
                boolean value = parseOr();
                skipWhitespace();
                if (!match(')')) {
                    throw new SBSqlParseException("括弧が閉じられていません: " + expression);
                }
                return value;
            }
            return parseComparison();
        }

        private boolean parseComparison() {
            String leftIdentifier = parseIdentifier();
            if (leftIdentifier == null || leftIdentifier.isEmpty()) {
                throw new SBSqlParseException("条件式の左辺が不正です: " + expression);
            }

            skipWhitespace();
            if (matchKeyword("IS")) {
                boolean not = matchKeyword("NOT");
                if (!matchKeyword("NULL")) {
                    throw new SBSqlParseException("NULL 判定の構文が不正です: " + expression);
                }
                Object value = getParameter(leftIdentifier);
                return not ? value != null : value == null;
            }

            String operator = parseOperator();
            if (operator == null) {
                throw new SBSqlParseException("演算子が見つかりません: " + expression);
            }
            skipWhitespace();
            ConditionValue rightValue = parseValue();
            Object leftValue = getParameter(leftIdentifier);

            switch (operator) {
            case "==":
            case "=":
                return equalsFlexible(leftValue, rightValue.value);
            case "!=":
                return !equalsFlexible(leftValue, rightValue.value);
            case ">":
                return compareFlexible(leftValue, rightValue.value) > 0;
            case "<":
                return compareFlexible(leftValue, rightValue.value) < 0;
            case ">=":
                return compareFlexible(leftValue, rightValue.value) >= 0;
            case "<=":
                return compareFlexible(leftValue, rightValue.value) <= 0;
            default:
                throw new SBSqlParseException("未サポートの演算子です: " + operator);
            }
        }

        private Object getParameter(String name) {
            if (parameters == null) {
                return null;
            }
            if (parameters.containsKey(name)) {
                return parameters.get(name);
            }
            return null;
        }

        private String parseIdentifier() {
            skipWhitespace();
            int start = index;
            while (index < expression.length()) {
                char c = expression.charAt(index);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) {
                return null;
            }
            return expression.substring(start, index);
        }

        private String parseOperator() {
            skipWhitespace();
            if (matchString("==")) {
                return "==";
            }
            if (matchString("!=")) {
                return "!=";
            }
            if (matchString(">=")) {
                return ">=";
            }
            if (matchString("<=")) {
                return "<=";
            }
            if (matchString(">")) {
                return ">";
            }
            if (matchString("<")) {
                return "<";
            }
            if (matchString("=")) {
                return "=";
            }
            return null;
        }

        private ConditionValue parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new SBSqlParseException("右辺の値が不足しています: " + expression);
            }
            char c = expression.charAt(index);
            if (c == '\'') {
                String literal = parseQuotedString();
                return new ConditionValue(literal);
            }
            if (Character.isDigit(c) || c == '-' || c == '+') {
                String number = parseNumber();
                return new ConditionValue(parseNumberValue(number));
            }
            String word = parseIdentifier();
            if (word == null) {
                throw new SBSqlParseException("右辺の値を解析できません: " + expression);
            }
            String lower = word.toLowerCase(Locale.ROOT);
            switch (lower) {
            case "null":
                return new ConditionValue(null);
            case "true":
                return new ConditionValue(Boolean.TRUE);
            case "false":
                return new ConditionValue(Boolean.FALSE);
            default:
                Object value = parameters != null && parameters.containsKey(word) ? parameters.get(word) : word;
                return new ConditionValue(value);
            }
        }

        private String parseQuotedString() {
            int start = index + 1;
            int pos = start;
            StringBuilder sb = new StringBuilder();
            while (pos < expression.length()) {
                char c = expression.charAt(pos);
                if (c == '\'') {
                    if (pos + 1 < expression.length() && expression.charAt(pos + 1) == '\'') {
                        sb.append('\'');
                        pos += 2;
                        continue;
                    }
                    index = pos + 1;
                    return sb.toString();
                }
                sb.append(c);
                pos++;
            }
            index = expression.length();
            return sb.toString();
        }

        private String parseNumber() {
            int start = index;
            while (index < expression.length()) {
                char c = expression.charAt(index);
                if (Character.isDigit(c) || c == '.' || c == '-' || c == '+') {
                    index++;
                } else {
                    break;
                }
            }
            return expression.substring(start, index);
        }

        private Object parseNumberValue(String literal) {
            try {
                if (literal.contains(".")) {
                    return Double.parseDouble(literal);
                }
                return Long.parseLong(literal);
            } catch (NumberFormatException e) {
                return literal;
            }
        }

        private void skipWhitespace() {
            while (index < expression.length()) {
                char c = expression.charAt(index);
                if (Character.isWhitespace(c)) {
                    index++;
                } else {
                    break;
                }
            }
        }

        private boolean matchKeyword(String keyword) {
            skipWhitespace();
            int len = keyword.length();
            if (expression.regionMatches(true, index, keyword, 0, len)) {
                int end = index + len;
                if (end == expression.length() || !Character.isLetterOrDigit(expression.charAt(end))) {
                    index = end;
                    return true;
                }
            }
            return false;
        }

        private boolean match(char c) {
            if (index < expression.length() && expression.charAt(index) == c) {
                index++;
                return true;
            }
            return false;
        }

        private boolean matchString(String token) {
            if (expression.startsWith(token, index)) {
                index += token.length();
                return true;
            }
            return false;
        }

        private boolean isEnd() {
            return index >= expression.length();
        }
    }

    private static final class ConditionValue {
        private final Object value;

        private ConditionValue(Object value) {
            this.value = value;
        }
    }

    private static boolean equalsFlexible(Object left, Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (isNumeric(left) && isNumeric(right)) {
            return Double.compare(asDouble(left), asDouble(right)) == 0;
        }
        if (left instanceof Boolean || right instanceof Boolean) {
            return Objects.equals(asBoolean(left), asBoolean(right));
        }
        if (left instanceof LocalDate && right instanceof String) {
            LocalDate parsed = parseLocalDate((String) right);
            return Objects.equals(left, parsed);
        }
        if (left instanceof LocalDateTime && right instanceof String) {
            LocalDateTime parsed = parseLocalDateTime((String) right);
            return Objects.equals(left, parsed);
        }
        return Objects.equals(String.valueOf(left), String.valueOf(right));
    }

    private static int compareFlexible(Object left, Object right) {
        if (left == null || right == null) {
            return 0;
        }
        if (isNumeric(left) && isNumeric(right)) {
            return Double.compare(asDouble(left), asDouble(right));
        }
        if (left instanceof Comparable && right instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparableLeft = (Comparable<Object>) left;
                return comparableLeft.compareTo(right);
            } catch (ClassCastException ignore) {
                // fallthrough to string comparison
            }
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    private static boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) {
                return false;
            }
            try {
                new BigDecimal(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private static double asDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value).trim());
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean(((String) value).trim());
        }
        return null;
    }

    private static LocalDate parseLocalDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(value, formatter);
            } catch (Exception ignore) {
                return null;
            }
        }
    }
}