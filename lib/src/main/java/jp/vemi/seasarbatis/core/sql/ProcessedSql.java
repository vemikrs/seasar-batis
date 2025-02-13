package jp.vemi.seasarbatis.core.sql;

import java.util.List;

/**
 * 処理済みSQLとパラメータ情報を保持するクラス
 */
@lombok.Data
@lombok.Builder
public class ProcessedSql {
    private final String sql;
    private final List<Object> params; // バインドパラメータのリスト（順序保持）

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return params;
    }
}