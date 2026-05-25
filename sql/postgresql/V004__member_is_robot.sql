-- =============================================
-- module-member V004 — 陪玩机器人平台标记（PostgreSQL）
-- =============================================
-- spec: spec/07-application/GAME_CLUB_ROOM_TEMPLATE_SPEC.md（real/auto split 只给 admin）
--
-- is_robot = 平台账号来源标记：1=系统生成的陪玩账号，不进普通会员列表/积分/每日统计。
-- 与 game_club_member.is_auto_player(club-scoped 自动玩家标记) 正交，不冲突。
-- 不改 user_type、不改登录体系；陪玩号仍 mobile/password 为空 → 拿不到普通登录。

ALTER TABLE member_users ADD COLUMN IF NOT EXISTS is_robot SMALLINT NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_member_users_is_robot ON member_users(is_robot);

-- 回填现有陪玩号：member_users.id 命中 game_club_member.is_auto_player=1。
-- 守卫 game_club_member 存在（跨模块同库；fresh deploy 顺序无关时安全跳过）。
DO $$
BEGIN
    IF to_regclass('game_club_member') IS NOT NULL THEN
        UPDATE member_users SET is_robot = 1
        WHERE id IN (SELECT user_id FROM game_club_member WHERE is_auto_player = 1);
    END IF;
END $$;
