/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.vemi.seasarbatis.core.builder.SBDeleteBuilder;
import jp.vemi.seasarbatis.core.builder.SBSelectBuilder;
import jp.vemi.seasarbatis.core.builder.SBUpdateBuilder;
import jp.vemi.seasarbatis.core.criteria.ComplexWhere;
import jp.vemi.seasarbatis.core.criteria.SimpleWhere;
import jp.vemi.seasarbatis.core.meta.SBColumnMeta;
import jp.vemi.seasarbatis.core.meta.SBTableMeta;
import jp.vemi.seasarbatis.sql.executor.SBQueryExecutor;
import jp.vemi.seasarbatis.util.SBEntityClassUtils;

/**
 * JDBC操作を簡素化するマネージャークラス。
 * Seasar2のJdbcManagerに似た操作性を提供します。
 * 
 * @author H.Kurosawa
 * @version 1.0
 * @since 2025/01/01
 */
public class SBJdbcManager {
    private static final Logger logger = LoggerFactory.getLogger(SBJdbcManager.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final SBQueryExecutor queryExecutor;

    public SBJdbcManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.queryExecutor = new SBQueryExecutor(sqlSessionFactory);
    }

    // SQL実行
    /**
     * SQL文に基づいて検索を実行します。
     * 
     * @param <T>    戻り値の要素型
     * @param sql    SQL文
     * @param params パラメータ
     * @return 検索結果のリスト
     */
    public <T> List<T> selectBySql(String sql, Map<String, Object> params) {
        return queryExecutor.select(sql, params);
    }

    /**
     * SQLファイルに基づいて検索を実行します。
     * 
     * @param <T>     戻り値の要素型
     * @param sqlFile SQLファイルのパス
     * @param params  パラメータ
     * @return 検索結果のリスト
     */
    public <T> List<T> selectBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, "SELECT");
    }

    /**
     * INSERT文を実行します。
     */
    public int insert(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, "INSERT");
    }

    /**
     * SQLファイルからINSERT文を実行します。
     */
    public int insertBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, "INSERT");
    }

    /**
     * UPDATE文を実行します。
     * 
     * @param sql    SQL文
     * @param params パラメータ
     * @return 更新された行数
     */
    public int update(String sql, Map<String, Object> params) {
        return queryExecutor.update(sql, params);
    }

    /**
     * SQLファイルからUPDATE文を実行します。
     */
    public int updateBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, "UPDATE");
    }

    /**
     * DELETE文を実行します。
     */
    public int delete(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, "DELETE");
    }

    /**
     * SQLファイルからDELETE文を実行します。
     */
    public int deleteBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, "DELETE");
    }

    // ---------- エンティティ操作 ----------
    /**
     * 主キーに基づいてエンティティを検索します。（例外をスローしない）
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @param primaryKeys 主キーの値（複数可）
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    public <T> T findByPkNoException(Class<T> entityClass, Object... primaryKeys) {
        PrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entityClass);
        if (primaryKeys.length != pkInfo.columnNames.size()) {
            throw new IllegalArgumentException(
                    String.format("主キーの数が一致しません。期待値: %d, 実際: %d",
                            pkInfo.columnNames.size(), primaryKeys.length));
        }

        String tableName = getTableName(entityClass);
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");

        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0)
                sql.append(" AND ");
            sql.append(pkInfo.columnNames.get(i))
                    .append(" = /*pk").append(i).append("*/0");
            params.put("pk" + i, primaryKeys[i]);
        }

        List<Map<String, Object>> results = queryExecutor.select(sql.toString(), params);
        return results.isEmpty() ? null : SBEntityClassUtils.mapToEntity(entityClass, results.get(0));
    }

    /**
     * 主キーに基づいてエンティティを検索します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @param primaryKeys 主キーの値（複数可）
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    public <T> T findByPk(Class<T> entityClass, Object... primaryKeys) {
        T entity = findByPkNoException(entityClass, primaryKeys);
        if (entity == null) {
            throw new IllegalArgumentException("エンティティが見つかりません: " + entityClass.getName());
        }
        return entity;
    }

    /**
     * エンティティの全件を検索します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @return エンティティのリスト
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        String tableName = getTableName(entityClass);
        String sql = "SELECT * FROM " + tableName;
        List<Map<String, Object>> result = queryExecutor.select(sql, new HashMap<>());
        if (result == null) {
            return java.util.Collections.emptyList();
        }
        return result.stream()
                .map(row -> SBEntityClassUtils.mapToEntity(entityClass, row))
                .collect(Collectors.toList());
    }

    /**
     * エンティティを新規登録します。
     * 
     * @param <T>    エンティティの型
     * @param entity 登録するエンティティ
     * @return 登録されたエンティティ
     */
    public <T> T insert(T entity) {
        String tableName = getTableName(entity.getClass());
        Map<String, Object> params = getEntityParams(entity);

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder values = new StringBuilder(") VALUES (");

        params.forEach((column, value) -> {
            sql.append(column).append(", ");
            values.append("/*").append(column).append("*/null, ");
        });

        sql.setLength(sql.length() - 2);
        values.setLength(values.length() - 2);
        sql.append(values).append(")");

        queryExecutor.execute(sql.toString(), params, "INSERT");
        return entity;
    }

    /**
     * 主キーに基づいてエンティティを更新します。
     * 
     * @param <T>    エンティティの型
     * @param entity 更新するエンティティ
     * @return 更新されたエンティティ
     * @throws IllegalArgumentException 主キーが設定されていない場合
     */
    public <T> T updateByPk(T entity) {
        String tableName = getTableName(entity.getClass());
        Map<String, Object> pkValues = getPrimaryKeyValues(entity);

        if (pkValues.isEmpty()) {
            throw new IllegalArgumentException("主キーが設定されていません");
        }

        Map<String, Object> params = getEntityParams(entity);
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");

        // 主キー以外のカラムを更新対象とする
        params.forEach((column, value) -> {
            if (!pkValues.containsKey(column)) {
                sql.append(column)
                        .append(" = /*")
                        .append(column)
                        .append("*/null, ");
            }
        });

        sql.setLength(sql.length() - 2);
        sql.append(" WHERE ");

        // 複数の主キーでWHERE句を構築
        int pkCount = 0;
        for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
            if (pkCount++ > 0)
                sql.append(" AND ");
            sql.append(pk.getKey())
                    .append(" = /*pk")
                    .append(pkCount)
                    .append("*/0");
            params.put("pk" + pkCount, pk.getValue());
        }

        queryExecutor.execute(sql.toString(), params, "UPDATE");
        return entity;
    }

    /**
     * 主キーに基づいてエンティティを削除します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @param primaryKeys 主キーの値（複数可）
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    public <T> void deleteByPk(Class<T> entityClass, Object... primaryKeys) {
        PrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entityClass);
        if (primaryKeys.length != pkInfo.columnNames.size()) {
            throw new IllegalArgumentException(
                    String.format("主キーの数が一致しません。期待値: %d, 実際: %d",
                            pkInfo.columnNames.size(), primaryKeys.length));
        }

        String tableName = getTableName(entityClass);
        StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE ");

        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < primaryKeys.length; i++) {
            if (i > 0)
                sql.append(" AND ");
            sql.append(pkInfo.columnNames.get(i))
                    .append(" = /*pk").append(i).append("*/0");
            params.put("pk" + i, primaryKeys[i]);
        }

        queryExecutor.execute(sql.toString(), params, "DELETE");
    }

    /**
     * エンティティを登録または更新します。
     * 主キーが設定されており、レコードが存在する場合は更新を行います。
     * それ以外の場合は新規登録を行います。
     * 
     * @param <T>    エンティティの型
     * @param entity 登録または更新するエンティティ
     * @return 処理されたエンティティ
     */
    public <T> T insertOrUpdate(T entity) {
        Map<String, Object> pkValues = getPrimaryKeyValues(entity);

        // 主キーが未設定の場合はINSERT
        if (pkValues.values().stream().allMatch(value -> value == null)) {
            logger.debug("主キーが未設定のため、INSERTを実行します");
            return insert(entity);
        }

        // 主キーで検索して存在確認
        Class<?> entityClass = entity.getClass();
        String tableName = getTableName(entityClass);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " WHERE ");

        Map<String, Object> params = new HashMap<>();
        int pkCount = 0;
        for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
            if (pkCount++ > 0) {
                sql.append(" AND ");
            }
            sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
            params.put("pk" + pkCount, pk.getValue());
        }

        List<Map<String, Object>> result = queryExecutor.select(sql.toString(), params);
        long count = ((Number) result.get(0).values().iterator().next()).longValue();

        if (count > 0) {
            logger.debug("レコードが存在するため、UPDATEを実行します");
            return updateByPk(entity);
        } else {
            logger.debug("レコードが存在しないため、INSERTを実行します");
            return insert(entity);
        }
    }

    // ---------- Fluent API ----------
    /**
     * エンティティに対するSelect操作を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @return SelectビルダーのFromインスタンス
     */
    public <T> SBSelectBuilder<T> from(Class<T> entityClass) {
        return new SBSelectBuilder<T>(this, entityClass);
    }

    /**
     * UPDATE文の構築を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass 更新対象のエンティティクラス
     * @return UpdateBuilderインスタンス
     */
    public <T> SBUpdateBuilder<T> update(Class<T> entityClass) {
        return new SBUpdateBuilder<>(this, entityClass);
    }

    /**
     * DELETE文の構築を開始します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass 削除対象のエンティティクラス
     * @return DeleteBuilderインスタンス
     */
    public <T> SBDeleteBuilder<T> delete(Class<T> entityClass) {
        return new SBDeleteBuilder<>(this, entityClass);
    }

    /**
     * 条件式を生成するSimpleWhereインスタンスを作成します。
     * 
     * @return 新しいSimpleWhereインスタンス
     */
    public SimpleWhere where() {
        return new SimpleWhere();
    }

    /**
     * 複合条件式を生成するComplexWhereインスタンスを作成します。
     * 
     * @return 新しいComplexWhereインスタンス
     */
    public ComplexWhere complexWhere() {
        return new ComplexWhere();
    }

    // トランザクション管理
    /**
     * トランザクション内で処理を実行します。
     */
    public void transaction(TransactionCallback callback) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            try {
                callback.execute(this);
                session.commit();
            } catch (Exception e) {
                session.rollback();
                logger.error("トランザクション実行エラー", e);
                throw new RuntimeException("トランザクション実行に失敗しました", e);
            }
        }
    }

    // Utility methods
    public <T> String getTableName(Class<T> entityClass) {
        SBTableMeta tableMeta = entityClass.getAnnotation(SBTableMeta.class);
        if (tableMeta != null) {
            String schema = tableMeta.schema();
            String tableName = tableMeta.name();
            return schema.isEmpty() ? tableName : schema + "." + tableName;
        }
        logger.warn("@SBTableMetaが見つかりません: {}", entityClass.getName());
        return entityClass.getSimpleName().toLowerCase();
    }

    private <T> Map<String, Object> getEntityParams(T entity) {
        Map<String, Object> params = new HashMap<>();
        Arrays.stream(entity.getClass().getDeclaredFields())
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                        if (columnMeta != null) {
                            params.put(columnMeta.name(), field.get(entity));
                        } else {
                            params.put(field.getName(), field.get(entity));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("パラメータの取得に失敗しました", e);
                    }
                });
        return params;
    }

    private <T> PrimaryKeyInfo getPrimaryKeyInfo(Class<T> entityClass) {
        List<Field> pkFields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> {
                    SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                    return columnMeta != null && columnMeta.primaryKey();
                })
                .collect(Collectors.toList());

        if (pkFields.isEmpty()) {
            throw new IllegalStateException("主キーが見つかりません: " + entityClass.getName());
        }

        List<String> pkColumnNames = pkFields.stream()
                .map(field -> field.getAnnotation(SBColumnMeta.class).name())
                .collect(Collectors.toList());

        return new PrimaryKeyInfo(pkFields, pkColumnNames);
    }

    private <T> Map<String, Object> getPrimaryKeyValues(T entity) {
        PrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entity.getClass());
        Map<String, Object> pkValues = new HashMap<>();

        pkInfo.fields.forEach(field -> {
            try {
                field.setAccessible(true);
                String columnName = field.getAnnotation(SBColumnMeta.class).name();
                pkValues.put(columnName, field.get(entity));
            } catch (Exception e) {
                throw new RuntimeException("主キーの取得に失敗しました: " + field.getName(), e);
            }
        });

        return pkValues;
    }

    /**
     * トランザクションコールバックインターフェース
     */
    public interface TransactionCallback {
        void execute(SBJdbcManager manager) throws Exception;
    }

    private static class PrimaryKeyInfo {
        final List<Field> fields;
        final List<String> columnNames;

        PrimaryKeyInfo(List<Field> fields, List<String> columnNames) {
            this.fields = fields;
            this.columnNames = columnNames;
        }
    }

}