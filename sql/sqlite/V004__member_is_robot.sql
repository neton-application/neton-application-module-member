-- =============================================
-- module-member V004 — 陪玩机器人平台标记（SQLite）
-- =============================================
-- is_robot=1 系统生成的陪玩账号，不进普通会员列表/积分/每日统计。
-- 与 game_club_member.is_auto_player(club-scoped) 正交。
-- 回填跨表(game_club_member)依模块迁移顺序，sqlite 部署如需回填请单独执行。

ALTER TABLE member_users ADD COLUMN is_robot INTEGER NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_member_users_is_robot ON member_users(is_robot);
