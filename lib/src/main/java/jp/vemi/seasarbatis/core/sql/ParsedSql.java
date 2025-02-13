package jp.vemi.seasarbatis.core.sql;

import java.util.ArrayList;
import java.util.List;

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
    private List<Object> orderedParameters = new ArrayList<>();
}
