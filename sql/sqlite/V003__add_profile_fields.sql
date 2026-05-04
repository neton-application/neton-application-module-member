-- =============================================
-- module-member V003 — profile 字段扩展（SQLite）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §2
--
-- SQLite 不支持单条 ALTER TABLE 多列；逐条加。

ALTER TABLE member_users ADD COLUMN username VARCHAR(64);
ALTER TABLE member_users ADD COLUMN username_updated_at BIGINT;
ALTER TABLE member_users ADD COLUMN gender SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE member_users ADD COLUMN bio VARCHAR(200);
ALTER TABLE member_users ADD COLUMN birthday VARCHAR(10);

CREATE UNIQUE INDEX IF NOT EXISTS uq_member_users_username ON member_users(username);
