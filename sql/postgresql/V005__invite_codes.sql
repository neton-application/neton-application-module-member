-- MEMBER_INVITE_CODE v1.0:邀请码 + 邀请记录(注册绑定/补填绑定,自动加好友状态)。
CREATE TABLE IF NOT EXISTS member_invite_codes (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id BIGINT,
    max_uses      INT NOT NULL DEFAULT 0,
    used_count    INT NOT NULL DEFAULT 0,
    status        SMALLINT NOT NULL DEFAULT 1,
    expires_at    BIGINT,
    remark        VARCHAR(255),
    created_by    BIGINT NOT NULL,
    created_at    BIGINT NOT NULL,
    updated_at    BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_invite_codes_owner ON member_invite_codes(owner_user_id);

CREATE TABLE IF NOT EXISTS member_invite_records (
    id                         BIGSERIAL PRIMARY KEY,
    code_id                    BIGINT NOT NULL,
    code                       VARCHAR(32) NOT NULL,
    inviter_user_id            BIGINT,
    invitee_user_id            BIGINT NOT NULL UNIQUE,
    register_mode              VARCHAR(32) NOT NULL,
    register_identifier_masked VARCHAR(64),
    bind_scene                 SMALLINT NOT NULL,
    auto_friend_status         SMALLINT NOT NULL DEFAULT 0,
    auto_friend_error          VARCHAR(255),
    bound_at                   BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_invite_records_code ON member_invite_records(code_id);
CREATE INDEX IF NOT EXISTS idx_member_invite_records_inviter ON member_invite_records(inviter_user_id);
