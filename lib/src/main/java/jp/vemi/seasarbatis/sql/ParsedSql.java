package jp.vemi.seasarbatis.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 解析済みSQLの結果を保持するクラス
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ParsedSql {
    private String sql;
    @lombok.Builder.Default
    private List<Object> parameters = new ArrayList<>();
    @lombok.Builder.Default
    private Map<String, Integer> parameterPositions = new TreeMap<>();

    /**
     * パラメータを追加します
     * 
     * @param parameter
     */
    public void addParameter(Object parameter) {
        parameters.add(parameter);
    }
}
