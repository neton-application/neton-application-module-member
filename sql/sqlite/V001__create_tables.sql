-- =============================================
-- module-member 全部建表语句 (SQLite)
-- =============================================

CREATE TABLE IF NOT EXISTS member_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mobile TEXT,
    password TEXT,
    nickname TEXT NOT NULL DEFAULT '',
    avatar TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    level_id INTEGER,
    experience INTEGER NOT NULL DEFAULT 0,
    point INTEGER NOT NULL DEFAULT 0,
    group_id INTEGER,
    register_ip TEXT,
    login_ip TEXT,
    login_date INTEGER,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_users_mobile ON member_users(mobile);

CREATE TABLE IF NOT EXISTS member_levels (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 0,
    experience INTEGER NOT NULL DEFAULT 0,
    discount INTEGER NOT NULL DEFAULT 100,
    icon TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS member_level_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    level_id INTEGER NOT NULL,
    level INTEGER NOT NULL DEFAULT 0,
    reason TEXT,
    description TEXT,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_level_records_user ON member_level_records(user_id);

CREATE TABLE IF NOT EXISTS member_point_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    biz_type INTEGER NOT NULL DEFAULT 0,
    biz_id TEXT,
    title TEXT NOT NULL,
    point INTEGER NOT NULL DEFAULT 0,
    total_point INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_point_records_user ON member_point_records(user_id);

CREATE TABLE IF NOT EXISTS member_sign_in_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    day INTEGER NOT NULL DEFAULT 0,
    point INTEGER NOT NULL DEFAULT 0,
    experience INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS member_sign_in_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    day INTEGER NOT NULL DEFAULT 0,
    point INTEGER NOT NULL DEFAULT 0,
    experience INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_sign_in_records_user ON member_sign_in_records(user_id);

CREATE TABLE IF NOT EXISTS member_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    remark TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS member_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS member_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT NOT NULL,
    value TEXT NOT NULL,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_member_configs_key ON member_configs(config_key);

CREATE TABLE IF NOT EXISTS member_addresses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    mobile TEXT NOT NULL,
    area_code TEXT,
    detail_address TEXT NOT NULL,
    default_status INTEGER NOT NULL DEFAULT 0,
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_addresses_user ON member_addresses(user_id);
