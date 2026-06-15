-- MEMBER-IDENTITY-ADAPTER: 身份来源追溯列。
-- builtin=内置账号;privchat=privchat user_id(member_users.id 即 privchat user_id)。
-- 创建后不可由普通业务接口修改;builtin→privchat 迁移走专门 migration。
ALTER TABLE member_users ADD COLUMN identity_provider VARCHAR(32) NOT NULL DEFAULT 'builtin';
