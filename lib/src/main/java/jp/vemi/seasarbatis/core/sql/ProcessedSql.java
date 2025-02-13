package jp.vemi.seasarbatis.core.sql;

/**
 * 処理済みSQLとパラメータ情報を保持するクラス
 */
@lombok.Data
@lombok.Builder
public class ProcessedSql {
    private final String sql;
}