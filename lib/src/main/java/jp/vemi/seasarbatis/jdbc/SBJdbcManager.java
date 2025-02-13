/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static jp.vemi.seasarbatis.core.sql.CommandType.DELETE;
import static jp.vemi.seasarbatis.core.sql.CommandType.INSERT;
import static jp.vemi.seasarbatis.core.sql.CommandType.SELECT;
import static jp.vemi.seasarbatis.core.sql.CommandType.UPDATE;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import jp.vemi.seasarbatis.core.sql.executor.SBQueryExecutor;

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
     * @param <T>        戻り値の要素型
     * @param sql        SQL文
     * @param params     パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> Select<T> selectBySql(String sql, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select()
                .from(resultType)
                .withSql(sql)
                .withParams(params);
    }

    /**
     * SQLファイルに基づいて検索を実行します。
     * 
     * @param <T>        戻り値の要素型
     * @param sqlFile    SQLファイルのパス
     * @param params     パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> Select<T> selectBySqlFile(String sqlFile, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select()
                .from(resultType)
                .withSqlFile(sqlFile)
                .withParams(params);
    }

    /**
     * INSERT文を実行します。
     */
    public int insert(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, INSERT);
    }

    /**
     * SQLファイルからINSERT文を実行します。
     */
    public int insertBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, INSERT);
    }

    /**
     * UPDATE文を実行します。
     * 
     * @param sql    SQL文
     * @param params パラメータ
     * @return 更新された行数
     */
    public int update(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, UPDATE);
    }

    /**
     * SQLファイルからUPDATE文を実行します。
     */
    public int updateBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, UPDATE);
    }

    /**
     * DELETE文を実行します。
     */
    public int delete(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, DELETE);
    }

    /**
     * SQLファイルからDELETE文を実行します。
     */
    public int deleteBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, DELETE);
    }

    // ---------- エンティティ操作 ----------
    /**
     * 主キーに基づいてエンティティを検索します。
     * 
     * @param <T>    エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> Select<T> findByPk(T entity) {
        return this.<T>select()
                .from((Class<T>) entity.getClass())
                .byPrimaryKey(getPrimaryKeyValues(entity));
    }

    /**
     * 主キーに基づいてエンティティを検索します。（例外をスローしない）
     * 
     * @param <T>    エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> Select<T> findByPkNoException(T entity) {
        return this.<T>select()
                .from((Class<T>) entity.getClass())
                .byPrimaryKey(getPrimaryKeyValues(entity))
                .suppressException();
    }

    /**
     * エンティティの全件を検索します。
     * 
     * @param <T>         エンティティの型
     * @param entityClass エンティティのクラス
     * @return エンティティのリスト
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        return this.<T>select().from(entityClass).getResultList();
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

        queryExecutor.execute(sql.toString(), params, INSERT);

        T newEntity = findByPk(entity).getSingleResult();
        return newEntity;
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

        queryExecutor.execute(sql.toString(), params, UPDATE);
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

        queryExecutor.execute(sql.toString(), params, DELETE);
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

        List<Map<String, Object>> result = queryExecutor.execute(sql.toString(), params, SELECT);
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

    /**
     * 型安全な検索クエリを開始します
     * 
     * @param <T> エンティティの型
     * @return 型安全な検索ビルダー
     */
    public <T> Select<T> select() {
        return new Select<>(this);
    }

    // Selectクラス
    public class Select<T> {
        @SuppressWarnings("unused")
        private final SBJdbcManager jdbcManager;
        private Class<T> entityClass;
        private String sql;
        private String sqlFile;
        private Map<String, Object> params = new HashMap<>();
        private Map<String, Object> primaryKeys;
        private boolean suppressException;

        private Select(SBJdbcManager jdbcManager) {
            this.jdbcManager = jdbcManager;
        }

        public Select<T> from(Class<T> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        public Select<T> withSql(String sql) {
            this.sql = sql;
            return this;
        }

        public Select<T> withSqlFile(String sqlFile) {
            this.sqlFile = sqlFile;
            return this;
        }

        public Select<T> withParams(Map<String, Object> params) {
            this.params.putAll(params);
            return this;
        }

        public Select<T> byPrimaryKey(Map<String, Object> primaryKeys) {
            this.primaryKeys = primaryKeys;
            return this;
        }

        public Select<T> suppressException() {
            this.suppressException = true;
            return this;
        }

        public T getSingleResult() {
            List<T> results = getResultList();
            if (results.isEmpty()) {
                if (!suppressException) {
                    throw new IllegalStateException("結果が見つかりません");
                }
                return null;
            }
            if (results.size() > 1) {
                throw new IllegalStateException("複数の結果が見つかりました");
            }
            return results.get(0);
        }

        public List<T> getResultList() {
            try {
                if (sql != null) {
                    return queryExecutor.executeSelect(sql, params, entityClass);
                } else if (sqlFile != null) {
                    return queryExecutor.executeFile(sqlFile, params, SELECT);
                } else if (primaryKeys != null) {
                    // 主キーによる検索のロジック
                    PrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entityClass);
                    String tableName = getTableName(entityClass);
                    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM " + tableName + " WHERE ");

                    for (int i = 0; i < primaryKeys.size(); i++) {
                        if (i > 0) {
                            sqlBuilder.append(" AND ");
                        }
                        String propertyName = pkInfo.columnNames.get(i);
                        sqlBuilder.append(propertyName)
                                .append(" = /*pk").append(i).append("*/").append(i);
                        params.put("pk" + i, primaryKeys.get(propertyName));
                    }

                    return queryExecutor.executeSelect(sqlBuilder.toString(), params, entityClass);
                } else {
                    // 全件検索
                    String tableName = getTableName(entityClass);
                    return queryExecutor.executeSelect("SELECT * FROM " + tableName, params, entityClass);
                }
            } catch (Exception e) {
                if (suppressException) {
                    logger.warn("検索実行中の例外を抑制: {}", e.getMessage());
                    return Collections.emptyList();
                }
                throw new RuntimeException("検索実行中にエラーが発生しました", e);
            }
        }
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