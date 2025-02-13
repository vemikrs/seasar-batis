package jp.vemi.seasarbatis.sql;

/**
 * 処理済みSQLとパラメータ情報を保持するクラス
 */
@lombok.Data
@lombok.Builder
public class ProcessedSql {
    private final String sql;
    private final java.util.List<Object> parameters;
    private final java.util.Map<String, Integer> parameterPositions;

    /**
     * MyBatis実行用のパラメータMapを生成
     */
    public java.util.Map<String, Object> createExecutionParameters() {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("_sql", sql);
        for (int i = 0; i < parameters.size(); i++) {
            params.put("param" + i, parameters.get(i));
        }
        return params;
    }
}