-- =============================================
-- module-member V003 — profile 字段扩展（PostgreSQL）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §2
--
-- 增列：
--   username             — 平台账号名，唯一索引；application 是守门人
--   username_updated_at  — 最近一次改名时间戳（millis），30 天限改判断依据
--   gender               — 0=unknown / 1=male / 2=female / 9=other
--   bio                  — 个性签名 0–200 字符
--   birthday             — ISO YYYY-MM-DD 字符串（避免跨端 DATE 时区歧义）

ALTER TABLE member_users
    ADD COLUMN IF NOT EXISTS username            VARCHAR(64),
    ADD COLUMN IF NOT EXISTS username_updated_at BIGINT,
    ADD COLUMN IF NOT EXISTS gender              SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bio                 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS birthday            VARCHAR(10);

-- username UNIQUE 约束（PostgreSQL 中 UNIQUE 默认允许多个 NULL 值）
CREATE UNIQUE INDEX IF NOT EXISTS uq_member_users_username ON member_users(username);
