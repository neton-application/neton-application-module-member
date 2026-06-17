-- REPORT P0: 用户举报记录（App Store UGC 1.2 最小治理）。
-- 客户端：消息长按举报 / 用户资料页举报用户。admin 后台消费举报记录。
CREATE TABLE IF NOT EXISTS public.member_reports (
    id bigserial PRIMARY KEY,
    reporter_uid bigint NOT NULL,
    target_type character varying(16) NOT NULL,
    target_id character varying(64) NOT NULL,
    reported_uid bigint,
    reason integer NOT NULL,
    description character varying(512),
    evidence_message_id bigint,
    status smallint DEFAULT 0 NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_reports_reporter ON public.member_reports(reporter_uid);
CREATE INDEX IF NOT EXISTS idx_member_reports_reported ON public.member_reports(reported_uid);
CREATE INDEX IF NOT EXISTS idx_member_reports_status ON public.member_reports(status);
