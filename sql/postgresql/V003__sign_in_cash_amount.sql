-- =============================================
-- module-member 签到现金奖励 (PostgreSQL)
-- 签到配置/记录增加 cash_amount（单位：分）。
-- 0 = 纯积分/经验（存量行为不变）；> 0 = 该天签到额外发放现金奖励。
-- 记录表保存当次发放快照，对账锚点：wallet ledger biz_id = 记录 id。
-- =============================================
SET search_path = public;

ALTER TABLE member_sign_in_configs
    ADD COLUMN IF NOT EXISTS cash_amount bigint DEFAULT 0 NOT NULL;

ALTER TABLE member_sign_in_records
    ADD COLUMN IF NOT EXISTS cash_amount bigint DEFAULT 0 NOT NULL;
