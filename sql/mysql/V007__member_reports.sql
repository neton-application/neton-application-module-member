-- REPORT P0: 用户举报记录（App Store UGC 1.2 最小治理）。
-- 客户端：消息长按举报 / 用户资料页举报用户。admin 后台消费举报记录。
CREATE TABLE IF NOT EXISTS member_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_uid BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    reported_uid BIGINT,
    reason INT NOT NULL,
    description VARCHAR(512),
    evidence_message_id BIGINT,
    status TINYINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_member_reports_reporter ON member_reports(reporter_uid);
CREATE INDEX idx_member_reports_reported ON member_reports(reported_uid);
CREATE INDEX idx_member_reports_status ON member_reports(status);
