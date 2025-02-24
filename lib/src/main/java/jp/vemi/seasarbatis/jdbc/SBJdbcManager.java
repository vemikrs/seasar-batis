/*
 * Copyright (C) 2025 VEMI, All Rights Reserved.
 */
package jp.vemi.seasarbatis.jdbc;

import static jp.vemi.seasarbatis.core.sql.CommandType.DELETE;
import static jp.vemi.seasarbatis.core.sql.CommandType.INSERT;
import static jp.vemi.seasarbatis.core.sql.CommandType.SELECT;
import static jp.vemi.seasarbatis.core.sql.CommandType.UPDATE;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
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
import jp.vemi.seasarbatis.exception.SBEntityException;
import jp.vemi.seasarbatis.exception.SBException;
import jp.vemi.seasarbatis.exception.SBIllegalStateException;
import jp.vemi.seasarbatis.exception.SBNoResultException;
import jp.vemi.seasarbatis.exception.SBNonUniqueResultException;
import jp.vemi.seasarbatis.exception.SBOptimisticLockException;
import jp.vemi.seasarbatis.exception.SBSQLException;
import jp.vemi.seasarbatis.exception.SBTransactionException;

/**
 * JDBC操作を簡素化するマネージャークラス。 Seasar2のJdbcManagerに似た操作性を提供します。
 * <p>
 * 本クラスは、MyBatisのSqlSessionを使用してデータベース操作を提供します。
 * </p>
 *
 * @author H.Kurosawa
 * @version 1.0.0
 * @since 2025/01/01
 */
public class SBJdbcManager {
    private static final Logger logger = LoggerFactory.getLogger(SBJdbcManager.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final SBQueryExecutor queryExecutor;
    private final SBTransactionOperation txOperation;

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param sqlSessionFactory {@link SqlSessionFactory}
     */
    public SBJdbcManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.txOperation = new SBTransactionOperation(sqlSessionFactory);
        this.queryExecutor = new SBQueryExecutor(sqlSessionFactory.getConfiguration(), txOperation);
    }

    /**
     * {@link SBJdbcManager}を構築します。
     *
     * @param dataSource {@link DataSource}
     */
    public SBJdbcManager(DataSource dataSource) {
        this(createSqlSessionFactory(dataSource));
    }

    /**
     * SqlSessionFactoryを生成します。
     *
     * @param dataSource {@link DataSource}
     * @return {@link SqlSessionFactory}
     */
    private static SqlSessionFactory createSqlSessionFactory(DataSource dataSource) {
        try (Reader reader = Resources.getResourceAsReader("mybatis-config.xml")) {
            SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader);
            factory.getConfiguration().setEnvironment(new org.apache.ibatis.mapping.Environment("development",
                    new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(), dataSource));
            logger.info("MyBatis設定を読み込みました");
            return factory;
        } catch (IOException e) {
            logger.error("MyBatis設定の読み込みに失敗しました: {}", e.getMessage(), e);
            throw new SBException("MyBatis設定の読み込みに失敗しました", e);
        }
    }

    /**
     * {@link SqlSessionFactory}を取得します。
     *
     * @return {@link SqlSessionFactory}
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    // SQL実行
    /**
     * SQL文に基づいて検索を実行します。
     *
     * @param <T> 戻り値の要素型
     * @param sql SQL文
     * @param params パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> Select<T> selectBySql(String sql, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select().from(resultType).withSql(sql).withParams(params);
    }

    /**
     * SQLファイルに基づいて検索を実行します。
     *
     * @param <T> 戻り値の要素型
     * @param sqlFile SQLファイルのパス
     * @param params パラメータ
     * @param resultType 結果のマッピング先クラス
     * @return 検索結果のリスト
     */
    public <T> Select<T> selectBySqlFile(String sqlFile, Map<String, Object> params, Class<T> resultType) {
        return this.<T>select().from(resultType).withSqlFile(sqlFile).withParams(params);
    }

    /**
     * INSERT文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 実行結果
     */
    public int insert(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, INSERT);
    }

    /**
     * SQLファイルからINSERT文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 実行結果
     */
    public int insertBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, INSERT);
    }

    /**
     * UPDATE文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 更新された行数
     */
    public int update(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, UPDATE);
    }

    /**
     * SQLファイルからUPDATE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 更新された行数
     */
    public int updateBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, UPDATE);
    }

    /**
     * DELETE文を実行します。
     *
     * @param sql SQL文
     * @param params パラメータ
     * @return 削除された行数
     */
    public int delete(String sql, Map<String, Object> params) {
        return queryExecutor.execute(sql, params, DELETE);
    }

    /**
     * SQLファイルからDELETE文を実行します。
     *
     * @param sqlFile SQLファイルパス
     * @param params パラメータ
     * @return 削除された行数
     */
    public int deleteBySqlFile(String sqlFile, Map<String, Object> params) {
        return queryExecutor.executeFile(sqlFile, params, DELETE);
    }

    // ---------- エンティティ操作 ----------
    /**
     * 主キーに基づいてエンティティを検索します。
     *
     * @param <T> エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> Select<T> findByPk(T entity) {
        return this.<T>select().from((Class<T>) entity.getClass()).byPrimaryKey(getPrimaryKeyValues(entity));
    }

    /**
     * 主キーに基づいてエンティティを検索します。（例外をスローしない）
     *
     * @param <T> エンティティの型
     * @param entity 検索対象のPK情報を含むエンティティ
     * @return 検索されたエンティティ、存在しない場合はnull
     * @throws IllegalArgumentException 指定された主キーの数が不正な場合
     */
    @SuppressWarnings("unchecked")
    public <T> Select<T> findByPkNoException(T entity) {
        return this.<T>select().from((Class<T>) entity.getClass()).byPrimaryKey(getPrimaryKeyValues(entity))
                .suppressException();
    }

    /**
     * エンティティの全件を検索します。
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティのクラス
     * @return エンティティのリスト
     */
    public <T> List<T> findAll(Class<T> entityClass) {
        return this.<T>select().from(entityClass).getResultList();
    }

    /**
     * エンティティを新規登録します。
     *
     * @param <T> エンティティの型
     * @param entity 登録するエンティティ
     * @return 登録されたエンティティ
     */
    public <T> T insert(T entity) {
        return insert(entity, false);
    }

    /**
     * エンティティを新規登録します。
     *
     * @param <T> エンティティの型
     * @param entity 登録するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 登録されたエンティティ
     */
    public <T> T insert(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
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

            @SuppressWarnings("unchecked")
            Select<T> newSelect = this.<T>select().from((Class<T>) entity.getClass())
                    .byPrimaryKey(getPrimaryKeyValues(entity));
            return newSelect.getSingleResult();
        });
    }

    /**
     * 主キーに基づいてエンティティを更新します。
     *
     * @param <T> エンティティの型
     * @param entity 更新するエンティティ
     * @return 更新されたエンティティ
     * @throws IllegalArgumentException 主キーが設定されていない場合
     */
    public <T> T update(T entity) {
        return update(entity, false);
    }

    /**
     * 主キーに基づいてエンティティを更新します。
     *
     * @param <T> エンティティの型
     * @param entity 更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 更新されたエンティティ
     * @throws IllegalArgumentException 主キーが設定されていない場合
     */
    @SuppressWarnings("unchecked")
    public <T> T update(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            String tableName = getTableName(entity.getClass());
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);

            if (pkValues.isEmpty()) {
                throw new SBIllegalStateException("主キーが設定されていません");
            }

            Map<String, Object> params = getEntityParams(entity);
            // 主キーを params から削除
            pkValues.keySet().forEach(params::remove);

            StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");

            // 主キー以外のカラムを更新対象とする
            params.forEach((column, value) -> {
                if (!pkValues.containsKey(column)) {
                    sql.append(column).append(" = /*").append(column).append("*/null, ");
                }
            });

            sql.setLength(sql.length() - 2);
            sql.append(" WHERE ");

            // 複数の主キーでWHERE句を構築
            int pkCount = 0;
            for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
                if (pkCount++ > 0)
                    sql.append(" AND ");
                sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
                params.put("pk" + pkCount, pk.getValue());
            }

            int updatedRows = queryExecutor.execute(sql.toString(), params, UPDATE);
            if (updatedRows == 0) {
                throw new SBOptimisticLockException("更新対象のレコードが見つかりませんでした。他のトランザクションによって更新された可能性があります。", entity,
                        pkValues.keySet().toArray(new String[0]));
            }

            List<T> newEntity = queryExecutor
                    .executeSelect(
                            "SELECT * FROM " + tableName + " WHERE "
                                    + pkValues.entrySet().stream().map(e -> e.getKey() + " = '" + e.getValue() + "'")
                                            .collect(Collectors.joining(" AND ")),
                            params, (Class<T>) entity.getClass());
            return newEntity.isEmpty() ? null : newEntity.get(0);
        });
    }

    /**
     * エンティティを1件削除します。
     * 
     * <p>
     * エンティティの主キー情報に基づいて、該当するレコードを削除します。 主キーが設定されていない場合は例外がスローされます。
     * </p>
     *
     * @param <T> エンティティの型
     * @param entity 削除対象のエンティティ
     * @return 削除された件数
     * @throws SBIllegalStateException 主キーが設定されていない場合
     */
    public <T> int delete(T entity) {
        return delete(entity, false);
    }

    /**
     * エンティティを1件削除します。
     * 
     * <p>
     * エンティティの主キー情報に基づいて、該当するレコードを削除します。 主キーが設定されていない場合は例外がスローされます。
     * </p>
     *
     * @param <T> エンティティの型
     * @param entity 削除対象のエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 削除された件数
     * @throws SBIllegalStateException 主キーが設定されていない場合
     */
    public <T> int delete(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);
            if (pkValues.isEmpty()) {
                throw new SBIllegalStateException("主キーが設定されていません: " + entity.getClass().getName());
            }

            String tableName = getTableName(entity.getClass());
            StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE ");

            Map<String, Object> params = new HashMap<>();
            int pkCount = 0;
            for (Map.Entry<String, Object> pk : pkValues.entrySet()) {
                if (pkCount++ > 0) {
                    sql.append(" AND ");
                }
                sql.append(pk.getKey()).append(" = /*pk").append(pkCount).append("*/0");
                params.put("pk" + pkCount, pk.getValue());
            }

            return queryExecutor.execute(sql.toString(), params, DELETE);
        });
    }

    /**
     * エンティティを登録または更新します。 主キーが設定されており、レコードが存在する場合は更新を行います。 それ以外の場合は新規登録を行います。
     *
     * @param <T> エンティティの型
     * @param entity 登録または更新するエンティティ
     * @return 処理されたエンティティ
     */
    public <T> T insertOrUpdate(T entity) {
        return insertOrUpdate(entity, false);
    }

    /**
     * エンティティを登録または更新します。 主キーが設定されており、レコードが存在する場合は更新を行います。 それ以外の場合は新規登録を行います。
     *
     * @param <T> エンティティの型
     * @param entity 登録または更新するエンティティ
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     * @return 処理されたエンティティ
     */
    public <T> T insertOrUpdate(T entity, boolean isIndependentTransaction) {
        return executeWithTransaction(isIndependentTransaction, () -> {
            Map<String, Object> pkValues = getPrimaryKeyValues(entity);

            // 主キーが未設定の場合はINSERT
            if (pkValues.values().stream().allMatch(value -> value == null)) {
                logger.debug("主キーが未設定のため、INSERTを実行します");
                return insert(entity, isIndependentTransaction);
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
                return update(entity, isIndependentTransaction);
            } else {
                logger.debug("レコードが存在しないため、INSERTを実行します");
                return insert(entity, isIndependentTransaction);
            }
        });
    }

    private <T> T executeWithTransaction(boolean isIndependentTransaction, Callable<T> operation) {
        if (isIndependentTransaction) {
            try {
                SqlSession session = sqlSessionFactory.openSession(false);
                txOperation.begin(session);
                try {
                    T result = operation.call();
                    txOperation.commit();
                    return result;
                } catch (Exception e) {
                    txOperation.rollback();
                    throw new SBSQLException("SQL実行エラー", e);
                } finally {
                    txOperation.end();
                }
            } catch (Exception e) {
                throw new SBSQLException("トランザクション実行エラー", e);
            }
        }

        // 既存のトランザクションが存在しない場合は新規作成
        if (!txOperation.isActive()) {
            txOperation.begin(sqlSessionFactory.openSession(false));
        }

        try {
            return operation.call();
        } catch (SBException e) {
            throw e; // SBExceptionはそのままスロー
        } catch (Exception e) {
            throw new SBSQLException("SQL実行エラー", e);
        }
    }

    private <T> T executeInNewTransaction(Function<SBJdbcManager, T> function) {
        SBJdbcManager innerManager = new SBJdbcManager(sqlSessionFactory);
        SqlSession session = sqlSessionFactory.openSession(ExecutorType.REUSE, false);

        try {
            innerManager.txOperation.begin(session);
            T result = function.apply(innerManager);
            innerManager.txOperation.commit();
            return result;
        } catch (Exception e) {
            innerManager.txOperation.rollback();
            logger.error("トランザクション実行エラー", e);
            throw new SBTransactionException("トランザクション実行に失敗しました", e);
        } finally {
            innerManager.txOperation.end();
        }
    }

    // ---------- Fluent API ----------
    /**
     * エンティティに対するSelect操作を開始します。
     * 
     * @param <T> エンティティの型
     * @param entityClass エンティティのクラス
     * @return SelectビルダーのFromインスタンス
     */
    public <T> SBSelectBuilder<T> from(Class<T> entityClass) {
        return new SBSelectBuilder<T>(this, entityClass);
    }

    /**
     * UPDATE文の構築を開始します。
     * 
     * @param <T> エンティティの型
     * @param entityClass 更新対象のエンティティクラス
     * @return UpdateBuilderインスタンス
     */
    public <T> SBUpdateBuilder<T> update(Class<T> entityClass) {
        return new SBUpdateBuilder<>(this, entityClass);
    }

    /**
     * DELETE文の構築を開始します。
     * 
     * @param <T> エンティティの型
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

    // ---------- トランザクション管理 ----------
    /**
     * トランザクション処理を実行します。
     *
     * @param callback トランザクションコールバック
     */
    public void transaction(TransactionCallback callback) {
        transaction(callback, false);
    }

    /**
     * トランザクション処理を実行します。
     *
     * @param callback トランザクションコールバック
     * @param isIndependentTransaction 独立したトランザクションで実行するかどうか
     */
    public void transaction(TransactionCallback callback, boolean isIndependentTransaction) {
        if (isIndependentTransaction) {
            executeInNewTransaction(manager -> {
                try {
                    callback.execute(manager);
                    return null;
                } catch (Exception e) {
                    throw new SBException(e);
                }
            });
            return;
        }

        try {
            callback.execute(this);
            txOperation.commit();
        } catch (Exception e) {
            txOperation.rollback();
            throw new SBTransactionException("トランザクション実行に失敗しました", e);
        } finally {
            txOperation.end();
        }
    }

    // Utility methods
    /**
     * エンティティクラスからテーブル名を取得します。
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティクラス
     * @return テーブル名
     */
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

    /**
     * エンティティからパラメータマップを取得します。
     *
     * @param <T> エンティティの型
     * @param entity エンティティ
     * @return パラメータマップ
     */
    private <T> Map<String, Object> getEntityParams(T entity) {
        Map<String, Object> params = new HashMap<>();
        Arrays.stream(entity.getClass().getDeclaredFields()).forEach(field -> {
            try {
                field.setAccessible(true);
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                String columnName = (columnMeta != null) ? columnMeta.name() : field.getName();
                Object value = null;

                // boolean型のフィールドの場合、isXxx形式のgetterメソッドを試す
                if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    try {
                        String getterName = "is" + Character.toUpperCase(field.getName().charAt(0))
                                + field.getName().substring(1);
                        java.lang.reflect.Method getter = entity.getClass().getMethod(getterName);
                        value = getter.invoke(entity);
                    } catch (NoSuchMethodException e) {
                        // isXxx形式のgetterメソッドがない場合は、そのままfield.get()を試す
                        value = field.get(entity);
                    }
                } else {
                    value = field.get(entity);
                }
                params.put(columnName, value);
            } catch (Exception e) {
                throw new SBException("パラメータの取得に失敗しました", e);
            }
        });
        return params;
    }

    /**
     * エンティティクラスから主キー情報を取得します。
     *
     * @param <T> エンティティの型
     * @param entityClass エンティティクラス
     * @return 主キー情報
     */
    private <T> PrimaryKeyInfo getPrimaryKeyInfo(Class<T> entityClass) {
        List<Field> pkFields = Arrays.stream(entityClass.getDeclaredFields()).filter(field -> {
            SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
            return columnMeta != null && columnMeta.primaryKey();
        }).collect(Collectors.toList());

        if (pkFields.isEmpty()) {
            throw new SBIllegalStateException("主キーが見つかりません: " + entityClass.getName());
        }

        List<String> pkColumnNames = pkFields.stream().map(field -> field.getAnnotation(SBColumnMeta.class).name())
                .collect(Collectors.toList());

        return new PrimaryKeyInfo(pkFields, pkColumnNames);
    }

    /**
     * エンティティから主キーの値を取得します。
     *
     * @param <T> エンティティの型
     * @param entity エンティティ
     * @return 主キーの値
     */
    private <T> Map<String, Object> getPrimaryKeyValues(T entity) {
        PrimaryKeyInfo pkInfo = getPrimaryKeyInfo(entity.getClass());
        Map<String, Object> pkValues = new HashMap<>();
        pkInfo.fields.forEach(field -> {
            try {
                field.setAccessible(true);
                SBColumnMeta columnMeta = field.getAnnotation(SBColumnMeta.class);
                pkValues.put(columnMeta.name(), field.get(entity));
            } catch (Exception e) {
                throw new SBEntityException("主キーの値の取得に失敗しました", e);
            }
        });
        return pkValues;
    }

    /**
     * トランザクションコールバックインターフェース
     */
    public interface TransactionCallback {
        /**
         * トランザクション内で実行する処理を定義します。
         *
         * @param manager JDBCマネージャー
         * @throws Exception 処理中に例外が発生した場合
         */
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

    /**
     * Selectクラス
     *
     * @param <T> エンティティの型
     */
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

        /**
         * 検索対象のエンティティクラスを設定します。
         *
         * @param entityClass エンティティクラス
         * @return Selectインスタンス
         */
        public Select<T> from(Class<T> entityClass) {
            this.entityClass = entityClass;
            return this;
        }

        /**
         * 実行するSQL文を設定します。
         *
         * @param sql SQL文
         * @return Selectインスタンス
         */
        public Select<T> withSql(String sql) {
            this.sql = sql;
            return this;
        }

        /**
         * 実行するSQLファイルパスを設定します。
         *
         * @param sqlFile SQLファイルパス
         * @return Selectインスタンス
         */
        public Select<T> withSqlFile(String sqlFile) {
            this.sqlFile = sqlFile;
            return this;
        }

        /**
         * パラメータを設定します。
         *
         * @param params パラメータ
         * @return Selectインスタンス
         */
        public Select<T> withParams(Map<String, Object> params) {
            this.params.putAll(params);
            return this;
        }

        /**
         * 主キーによる検索条件を設定します。
         *
         * @param primaryKeys 主キー
         * @return Selectインスタンス
         */
        public Select<T> byPrimaryKey(Map<String, Object> primaryKeys) {
            this.primaryKeys = primaryKeys;
            return this;
        }

        /**
         * 例外を抑制するかどうかを設定します。
         *
         * @return Selectインスタンス
         */
        public Select<T> suppressException() {
            this.suppressException = true;
            return this;
        }

        /**
         * 検索結果を1件返します。
         *
         * @return 検索結果
         * @throws SBNoResultException 検索結果が0件の場合
         * @throws SBNonUniqueResultException 検索結果が複数件存在する場合
         */
        public T getSingleResult() {
            List<T> results = getResultList();
            if (results.isEmpty()) {
                if (!suppressException) {
                    throw new SBNoResultException("検索結果が0件でした");
                }
                return null;
            }
            if (results.size() > 1) {
                throw new SBNonUniqueResultException("検索結果が複数件存在します: " + results.size() + "件");
            }
            return results.get(0);
        }

        /**
         * 検索結果をリストで返します。
         *
         * @return 検索結果
         */
        public List<T> getResultList() {
            try {
                if (!txOperation.isActive()) {
                    txOperation.begin(sqlSessionFactory.openSession(false));
                }

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
                        sqlBuilder.append(propertyName).append(" = /*pk").append(i).append("*/").append(i);
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
                    logger.warn("検索実行中の例外を抑制します。: {}", e.getMessage());
                    return Collections.emptyList();
                }
                throw new SBException("検索実行中にエラーが発生しました", e);
            }
        }
    }

    /**
     * 主キー情報
     */
    private static class PrimaryKeyInfo {
        final List<Field> fields;
        final List<String> columnNames;

        PrimaryKeyInfo(List<Field> fields, List<String> columnNames) {
            this.fields = fields;
            this.columnNames = columnNames;
        }
    }
}