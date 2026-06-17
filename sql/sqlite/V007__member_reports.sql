-- REPORT P0: 用户举报记录（App Store UGC 1.2 最小治理）。
-- 客户端：消息长按举报 / 用户资料页举报用户。admin 后台消费举报记录。
CREATE TABLE IF NOT EXISTS member_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_uid INTEGER NOT NULL,
    target_type TEXT NOT NULL,            -- message / user / channel
    target_id TEXT NOT NULL,
    reported_uid INTEGER,                 -- 被举报用户（冗余，便于聚合）
    reason INTEGER NOT NULL,              -- ReportReason code
    description TEXT,
    evidence_message_id INTEGER,
    status INTEGER NOT NULL DEFAULT 0,    -- 0 待处理 / 1 处理中 / 2 已处理 / 3 已驳回
    deleted INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_member_reports_reporter ON member_reports(reporter_uid);
CREATE INDEX IF NOT EXISTS idx_member_reports_reported ON member_reports(reported_uid);
CREATE INDEX IF NOT EXISTS idx_member_reports_status ON member_reports(status);
