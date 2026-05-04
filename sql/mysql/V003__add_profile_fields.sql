-- =============================================
-- module-member V003 — profile 字段扩展（MySQL）
-- =============================================
-- spec: spec/07-application/MODULE_MEMBER_PROFILE_SPEC.md §2

ALTER TABLE member_users
    ADD COLUMN username            VARCHAR(64),
    ADD COLUMN username_updated_at BIGINT,
    ADD COLUMN gender              SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN bio                 VARCHAR(200),
    ADD COLUMN birthday            VARCHAR(10);

CREATE UNIQUE INDEX uq_member_users_username ON member_users(username);
