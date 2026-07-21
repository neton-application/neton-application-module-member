-- 会话版本：builtin 身份后端的 token 携带 session_version，改密/登出全端时递增使旧 token 失效。
ALTER TABLE member_users ADD COLUMN IF NOT EXISTS session_version BIGINT NOT NULL DEFAULT 0;
