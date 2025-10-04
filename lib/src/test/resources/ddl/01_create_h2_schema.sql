DROP TABLE IF EXISTS sbtest_users;
CREATE TABLE sbtest_users (
    -- 数値型
    id BIGINT NOT NULL AUTO_INCREMENT,
    sequence_no INT,
    amount DECIMAL(10,2),
    rate FLOAT,
    score DOUBLE,
    is_active TINYINT(1),

    -- 文字型
    name VARCHAR(100),
    description TEXT,
    memo TEXT,
    char_code CHAR(3),

    -- 日付時刻型
    created_at TIMESTAMP,
    updated_at DATETIME,
    birth_date DATE,
    work_time TIME,

    -- バイナリ型
    profile_image BLOB,
    attachment BLOB,

    -- 列挙型 (H2では文字列として表現)
    status VARCHAR(20) CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    user_type VARCHAR(50), -- H2では SET 型を直接サポートしないため VARCHAR を使用
    
    -- JSON型 (H2では TEXT として格納)
    preferences TEXT,

    PRIMARY KEY (id)
);

-- インデックス作成
CREATE INDEX idx_name ON sbtest_users (name);
CREATE INDEX idx_status ON sbtest_users (status);