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
    memo MEDIUMTEXT,
    char_code CHAR(3),

    -- 日付時刻型
    created_at TIMESTAMP,
    updated_at DATETIME,
    birth_date DATE,
    work_time TIME,

    -- バイナリ型
    profile_image BLOB,
    attachment MEDIUMBLOB,

    -- 列挙型
    status ENUM('ACTIVE', 'INACTIVE', 'DELETED'),
    user_type SET('ADMIN', 'USER', 'GUEST'),
    
    -- JSON型 (MySQL 5.7以降)
    preferences JSON,

    PRIMARY KEY (id),
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;