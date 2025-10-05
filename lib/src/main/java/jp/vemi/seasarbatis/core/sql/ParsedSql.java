package jp.vemi.seasarbatis.core.sql;

/**
 * 解析済みSQLの結果を保持するクラス
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ParsedSql {
    private String sql;
    private java.util.List<String> parameterNames;
    private java.util.Map<String, Object> parameterValues;
}
