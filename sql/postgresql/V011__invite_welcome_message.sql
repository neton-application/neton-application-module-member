-- MEMBER_INVITE_CODE:邀请码级自动打招呼用语(运营在后台按码配置;空=用全局 conf 兜底)。
ALTER TABLE member_invite_codes ADD COLUMN IF NOT EXISTS welcome_message TEXT;
