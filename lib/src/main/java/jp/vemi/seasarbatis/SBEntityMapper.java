/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

/**
 * エンティティの基本的なCRUD操作を提供するジェネリックマッパークラス。
 * MyBatisを使用してデータベース操作を実行します。
 * 
 * <p>このクラスは補助的なユーティリティとして提供されています。
 * MyBatis Generatorで生成される個別のMapperインターフェースが利用可能な場合は、
 * そちらの使用を推奨します。
 * 
 * <p>以下のような場合にこのクラスの使用を検討してください：
 * <ul>
 *   <li>簡易的なCRUD操作のみが必要な場合</li>
 *   <li>MyBatis Generator の使用が適さない小規模な実装の場合</li>
 *   <li>プロトタイプ開発やPoCでの利用</li>
 * </ul>
 *
 * @param <T> エンティティの型
 */
public class SBEntityMapper<T> {
    private final SqlSessionFactory sqlSessionFactory;
    private final Class<T> entityClass;

    /**
     * エンティティマッパーを初期化します。
     *
     * @param sqlSessionFactory SQLセッションファクトリー
     * @param entityClass マッピング対象のエンティティクラス
     */
    public SBEntityMapper(SqlSessionFactory sqlSessionFactory, Class<T> entityClass) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.entityClass = entityClass;
    }

    /**
     * すべてのエンティティを取得します。
     *
     * @return エンティティのリスト
     */
    public List<T> findAll() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectList(entityClass.getName() + ".findAll");
        }
    }

    /**
     * 指定されたIDのエンティティを取得します。
     *
     * @param id エンティティのID
     * @return 見つかったエンティティ、存在しない場合はnull
     */
    public T findById(Object id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.selectOne(entityClass.getName() + ".findById", id);
        }
    }

    /**
     * 新しいエンティティを挿入します。
     *
     * @param entity 挿入するエンティティ
     */
    public void insert(T entity) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.insert(entityClass.getName() + ".insert", entity);
            session.commit();
        }
    }

    /**
     * 既存のエンティティを更新します。
     *
     * @param entity 更新するエンティティ
     */
    public void update(T entity) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.update(entityClass.getName() + ".update", entity);
            session.commit();
        }
    }

    /**
     * 指定されたIDのエンティティを削除します。
     *
     * @param id 削除するエンティティのID
     */
    public void delete(Object id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            session.delete(entityClass.getName() + ".delete", id);
            session.commit();
        }
    }
}