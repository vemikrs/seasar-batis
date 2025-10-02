DROP TABLE IF EXISTS sbtest_users;
CREATE TABLE sbtest_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sequence_no INT,
    amount DECIMAL(10,2),
    rate FLOAT,
    score DOUBLE,
    is_active TINYINT(1),
    name VARCHAR(100),
    description TEXT,
    memo MEDIUMTEXT,
    char_code CHAR(3),
    created_at TIMESTAMP,
    updated_at DATETIME,
    birth_date DATE,
    work_time TIME,
    status VARCHAR(20),
    user_type VARCHAR(50),
    preferences VARCHAR(1000),
    PRIMARY KEY (id),
    INDEX idx_name (name),
    INDEX idx_status (status)
);