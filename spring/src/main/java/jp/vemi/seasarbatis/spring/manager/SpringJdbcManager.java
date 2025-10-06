/*
 * Copyright(c) 2025 VEMIDaS, All rights reserved.
 */
package jp.vemi.seasarbatis.spring.manager;

import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.seasarbatis.core.query.SBSelect;
import jp.vemi.seasarbatis.jdbc.SBJdbcManager;

/**
 * SpringのトランザクションマネージャーとSeasarBatisを統合するクラス。
 * <p>
 * <strong>非推奨：</strong> このクラスはv0.0.2で非推奨となりました。
 * 代わりに{@code jp.vemi.batisfluid.spring.core.SpringJdbcFlow}を使用してください。
 * </p>
 *
 * @author H.Kurosawa
 * @version 0.0.2
 * @deprecated v0.0.2以降は{@code jp.vemi.batisfluid.spring.core.SpringJdbcFlow}を使用してください。
 *             このクラスはv0.0.3以降で削除される予定です。
 */
@Deprecated(since = "0.0.2")
@Component
public class SpringJdbcManager extends SBJdbcManager {

    @Autowired
    public SpringJdbcManager(SqlSessionFactory sqlSessionFactory) {
        super(sqlSessionFactory);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> SBSelect<T> findByPk(T entity) {
        return super.findByPk(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> findAll(Class<T> entityClass) {
        return super.findAll(entityClass);
    }

    @Override
    @Transactional
    public <T> T insert(T entity) {
        return super.insert(entity);
    }

    @Override
    @Transactional
    public <T> T update(T entity) {
        return super.update(entity);
    }

    @Override
    @Transactional
    public <T> T insertOrUpdate(T entity) {
        return super.insertOrUpdate(entity);
    }

    @Override
    @Transactional
    public <T> int delete(T entity) {
        return super.delete(entity);
    }
}
