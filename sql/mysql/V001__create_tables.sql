-- =============================================
-- module-member 全部建表语句 (MySQL)
-- =============================================

CREATE TABLE IF NOT EXISTS member_users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    mobile VARCHAR(32),
    password VARCHAR(255),
    nickname VARCHAR(64) NOT NULL DEFAULT '',
    avatar VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 0,
    level_id BIGINT,
    experience INT NOT NULL DEFAULT 0,
    point INT NOT NULL DEFAULT 0,
    group_id BIGINT,
    register_ip VARCHAR(64),
    login_ip VARCHAR(64),
    login_date BIGINT,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_users_mobile ON member_users(mobile);

CREATE TABLE IF NOT EXISTS member_levels (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    level INT NOT NULL DEFAULT 0,
    experience INT NOT NULL DEFAULT 0,
    discount INT NOT NULL DEFAULT 100,
    icon VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_level_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    level_id BIGINT NOT NULL,
    level INT NOT NULL DEFAULT 0,
    reason VARCHAR(255),
    description VARCHAR(512),
    created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_level_records_user ON member_level_records(user_id);

CREATE TABLE IF NOT EXISTS member_point_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    biz_type TINYINT NOT NULL DEFAULT 0,
    biz_id VARCHAR(128),
    title VARCHAR(128) NOT NULL,
    point INT NOT NULL DEFAULT 0,
    total_point INT NOT NULL DEFAULT 0,
    description VARCHAR(512),
    created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_point_records_user ON member_point_records(user_id);

CREATE TABLE IF NOT EXISTS member_sign_in_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    day INT NOT NULL DEFAULT 0,
    point INT NOT NULL DEFAULT 0,
    experience INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_sign_in_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    day INT NOT NULL DEFAULT 0,
    point INT NOT NULL DEFAULT 0,
    experience INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_sign_in_records_user ON member_sign_in_records(user_id);

CREATE TABLE IF NOT EXISTS member_groups (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    remark VARCHAR(512),
    status TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_tags (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS member_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    value TEXT NOT NULL,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE UNIQUE INDEX idx_member_configs_key ON member_configs(config_key);

CREATE TABLE IF NOT EXISTS member_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    mobile VARCHAR(32) NOT NULL,
    area_code VARCHAR(16),
    detail_address VARCHAR(512) NOT NULL,
    default_status TINYINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_addresses_user ON member_addresses(user_id);
