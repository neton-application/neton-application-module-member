-- =============================================
-- module-member V004 — 陪玩机器人平台标记（MySQL）
-- =============================================
-- is_robot=1 系统生成的陪玩账号，不进普通会员列表/积分/每日统计。

ALTER TABLE member_users ADD COLUMN is_robot TINYINT NOT NULL DEFAULT 0;
CREATE INDEX idx_member_users_is_robot ON member_users(is_robot);
