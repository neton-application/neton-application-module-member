-- 游客账号标记（CUSTOMER_SERVICE_PLATFORM_SPEC §2.1 前置 3、§3.2）
--
-- member_users 早就允许 mobile / username / password 全为空，无凭证账号本来合法；
-- 缺的只是「认得出它是哪一类」。没有标记的话，客服 widget 访客、游戏游客登录、
-- 试用账号会一并混进会员列表、等级榜、积分榜与人数统计。
--
-- 比照既有的 is_robot：会员子类型标记，列表/排行/统计默认排除，需要时显式带上。
-- 二者正交 —— 机器人不是游客，游客也不是机器人。
--
-- 游客升级为正式会员 = 给同一行绑上凭证并把本列清零；id 不变，所以 IM 身份与
-- 全部会话历史原样保留。

ALTER TABLE member_users ADD COLUMN IF NOT EXISTS is_guest SMALLINT NOT NULL DEFAULT 0;

-- 列表与统计几乎总是「排除游客」，按此建部分索引而不是全列索引
CREATE INDEX IF NOT EXISTS idx_member_users_guest ON member_users (is_guest) WHERE is_guest = 1;
