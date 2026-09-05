-- member 模块（postgresql）—— 1.0.0 beta1 合并基线。
--
-- 🔴 **只给全新数据库用。** 由原来的 15 个迁移脚本按执行顺序拼接而成：
-- 顺序不变、语句不变，所以结果与逐条执行完全一致。
--
-- 为什么是拼接而不是导出结构快照：这些脚本里有 init_data / seed_menus 这类
-- **种子数据**，`pg_dump --schema-only` 会把它们丢掉，而只导结构就得再手工把
-- INSERT 补回来——那一步没有任何东西能验证对错。拼接则由构造保证等价。
--
-- 拼接的代价是留下了少量互相抵消的步骤（先加列、后改列）。它们无害，但**不要**
-- 试图"顺手清理"：清理一次就等于重新引入一个没人验证过的结构。
--
-- 存量库怎么办：本发布不提供原地升级。Neton 的迁移器按 checksum 校验，V001 变了
-- 就会拒绝启动——这是有意的，见 MigrationEngine 的 CHECKSUM_MISMATCH。
--
-- 加新东西请新增 V002、V003…，不要改这个文件。


-- ─────────────────────────────────────────────────────────────
-- 原 V001__create_tables.sql
-- ─────────────────────────────────────────────────────────────

-- =============================================
-- module-member 全部建表语句 (PostgreSQL)
-- baseline: pg_dump of member_* tables (pre-release consolidation)
-- =============================================
SET search_path = public;


CREATE TABLE public.member_addresses (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    name character varying(64) NOT NULL,
    mobile character varying(32) NOT NULL,
    area_code character varying(16),
    detail_address character varying(512) NOT NULL,
    default_status smallint DEFAULT 0 NOT NULL,
    deleted smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_addresses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_addresses_id_seq OWNED BY public.member_addresses.id;

CREATE TABLE public.member_configs (
    id bigint NOT NULL,
    config_key character varying(128) NOT NULL,
    value text NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_configs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_configs_id_seq OWNED BY public.member_configs.id;

CREATE TABLE public.member_groups (
    id bigint NOT NULL,
    name character varying(64) NOT NULL,
    remark character varying(512),
    status smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_groups_id_seq OWNED BY public.member_groups.id;

CREATE TABLE public.member_level_records (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    level_id bigint NOT NULL,
    level integer DEFAULT 0 NOT NULL,
    reason character varying(255),
    description character varying(512),
    created_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_level_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_level_records_id_seq OWNED BY public.member_level_records.id;

CREATE TABLE public.member_levels (
    id bigint NOT NULL,
    name character varying(64) NOT NULL,
    level integer DEFAULT 0 NOT NULL,
    experience integer DEFAULT 0 NOT NULL,
    discount integer DEFAULT 100 NOT NULL,
    icon character varying(512),
    status smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_levels_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_levels_id_seq OWNED BY public.member_levels.id;

CREATE TABLE public.member_nickname_adjective (
    id bigint NOT NULL,
    word character varying(32) NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_nickname_adjective_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_nickname_adjective_id_seq OWNED BY public.member_nickname_adjective.id;

CREATE TABLE public.member_nickname_noun (
    id bigint NOT NULL,
    word character varying(32) NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_nickname_noun_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_nickname_noun_id_seq OWNED BY public.member_nickname_noun.id;

CREATE TABLE public.member_point_records (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    biz_type smallint DEFAULT 0 NOT NULL,
    biz_id character varying(128),
    title character varying(128) NOT NULL,
    point integer DEFAULT 0 NOT NULL,
    total_point integer DEFAULT 0 NOT NULL,
    description character varying(512),
    created_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_point_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_point_records_id_seq OWNED BY public.member_point_records.id;

CREATE TABLE public.member_sign_in_configs (
    id bigint NOT NULL,
    day integer DEFAULT 0 NOT NULL,
    point integer DEFAULT 0 NOT NULL,
    experience integer DEFAULT 0 NOT NULL,
    status smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_sign_in_configs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_sign_in_configs_id_seq OWNED BY public.member_sign_in_configs.id;

CREATE TABLE public.member_sign_in_records (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    day integer DEFAULT 0 NOT NULL,
    point integer DEFAULT 0 NOT NULL,
    experience integer DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_sign_in_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_sign_in_records_id_seq OWNED BY public.member_sign_in_records.id;

CREATE TABLE public.member_tags (
    id bigint NOT NULL,
    name character varying(64) NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL
);

CREATE SEQUENCE public.member_tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.member_tags_id_seq OWNED BY public.member_tags.id;

CREATE TABLE public.member_users (
    id bigint NOT NULL,
    mobile character varying(20),
    password character varying(255),
    nickname character varying(64) DEFAULT ''::character varying NOT NULL,
    avatar character varying(512),
    status smallint DEFAULT 0 NOT NULL,
    level_id bigint,
    experience integer DEFAULT 0 NOT NULL,
    point integer DEFAULT 0 NOT NULL,
    group_id bigint,
    register_ip character varying(64),
    login_ip character varying(64),
    login_date bigint,
    deleted smallint DEFAULT 0 NOT NULL,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL,
    username character varying(64),
    username_updated_at bigint,
    gender smallint DEFAULT 0 NOT NULL,
    bio character varying(200),
    birthday character varying(10),
    is_robot smallint DEFAULT 0 NOT NULL,
    identity_provider character varying(32) DEFAULT 'builtin'::character varying NOT NULL
);

ALTER TABLE ONLY public.member_addresses ALTER COLUMN id SET DEFAULT nextval('public.member_addresses_id_seq'::regclass);

ALTER TABLE ONLY public.member_configs ALTER COLUMN id SET DEFAULT nextval('public.member_configs_id_seq'::regclass);

ALTER TABLE ONLY public.member_groups ALTER COLUMN id SET DEFAULT nextval('public.member_groups_id_seq'::regclass);

ALTER TABLE ONLY public.member_level_records ALTER COLUMN id SET DEFAULT nextval('public.member_level_records_id_seq'::regclass);

ALTER TABLE ONLY public.member_levels ALTER COLUMN id SET DEFAULT nextval('public.member_levels_id_seq'::regclass);

ALTER TABLE ONLY public.member_nickname_adjective ALTER COLUMN id SET DEFAULT nextval('public.member_nickname_adjective_id_seq'::regclass);

ALTER TABLE ONLY public.member_nickname_noun ALTER COLUMN id SET DEFAULT nextval('public.member_nickname_noun_id_seq'::regclass);

ALTER TABLE ONLY public.member_point_records ALTER COLUMN id SET DEFAULT nextval('public.member_point_records_id_seq'::regclass);

ALTER TABLE ONLY public.member_sign_in_configs ALTER COLUMN id SET DEFAULT nextval('public.member_sign_in_configs_id_seq'::regclass);

ALTER TABLE ONLY public.member_sign_in_records ALTER COLUMN id SET DEFAULT nextval('public.member_sign_in_records_id_seq'::regclass);

ALTER TABLE ONLY public.member_tags ALTER COLUMN id SET DEFAULT nextval('public.member_tags_id_seq'::regclass);

ALTER TABLE ONLY public.member_addresses
    ADD CONSTRAINT member_addresses_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_configs
    ADD CONSTRAINT member_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_groups
    ADD CONSTRAINT member_groups_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_level_records
    ADD CONSTRAINT member_level_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_levels
    ADD CONSTRAINT member_levels_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_nickname_adjective
    ADD CONSTRAINT member_nickname_adjective_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_nickname_adjective
    ADD CONSTRAINT member_nickname_adjective_word_key UNIQUE (word);

ALTER TABLE ONLY public.member_nickname_noun
    ADD CONSTRAINT member_nickname_noun_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_nickname_noun
    ADD CONSTRAINT member_nickname_noun_word_key UNIQUE (word);

ALTER TABLE ONLY public.member_point_records
    ADD CONSTRAINT member_point_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_sign_in_configs
    ADD CONSTRAINT member_sign_in_configs_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_sign_in_records
    ADD CONSTRAINT member_sign_in_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_tags
    ADD CONSTRAINT member_tags_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.member_users
    ADD CONSTRAINT member_users_pkey PRIMARY KEY (id);

CREATE INDEX idx_member_addresses_user ON public.member_addresses USING btree (user_id);

CREATE UNIQUE INDEX idx_member_configs_key ON public.member_configs USING btree (config_key);

CREATE INDEX idx_member_level_records_user ON public.member_level_records USING btree (user_id);

CREATE INDEX idx_member_nickname_adjective_status ON public.member_nickname_adjective USING btree (status);

CREATE INDEX idx_member_nickname_noun_status ON public.member_nickname_noun USING btree (status);

CREATE INDEX idx_member_point_records_user ON public.member_point_records USING btree (user_id);

CREATE INDEX idx_member_sign_in_records_user ON public.member_sign_in_records USING btree (user_id);

CREATE INDEX idx_member_users_is_robot ON public.member_users USING btree (is_robot);

CREATE INDEX idx_member_users_mobile ON public.member_users USING btree (mobile);

CREATE UNIQUE INDEX uq_member_users_username ON public.member_users USING btree (username);


-- ─────────────────────────────────────────────────────────────
-- 原 V002__init_data.sql
-- ─────────────────────────────────────────────────────────────

-- =============================================
-- module-member 初始化数据 (PostgreSQL)
-- baseline: 会员中心 admin 菜单 seed (ON CONFLICT DO NOTHING)
-- =============================================
SET search_path = public;-- ── 后台菜单 ──────────────────────────────────────────────────────────
--
-- 🔴 **不写死菜单 id**：id 由 system_menus 的序列在安装时分配。
--
-- 以前每个模块把 id 硬编码在 SQL 里，模块之间就得就编号达成一致，而唯一的
-- 保护是 `ON CONFLICT (id) DO NOTHING`——撞号不会报错，只会**静默**丢菜单。
-- 实测后果：gateway 和 privchat 撞了 700-704，于是「令牌管理」「定价修改」这些
-- AI 网关的按钮被挂到了「用户管理」「群组管理」底下，而没有任何地方报错。
--
-- 现在父子关系在语句内部用**模块内唯一的菜单名**连接（同一模块内不允许重名），
-- 跨模块不再共享任何编号，撞号从结构上不可能发生。
--
-- 加菜单：往对应层级的 VALUES 里加一行即可，不需要挑号。
-- 改菜单：后续迁移按 permission 定位；若该 permission 在本模块内不唯一，
--         用 name 加父节点定位。

WITH lvl1 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    VALUES
        ('会员中心', '', 1, 0, '/member', NULL, 'ant-design:user-outlined', 3, 1, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint)
    RETURNING id, name
),
lvl2 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    SELECT v.name, v.permission, v.type,
           (SELECT p.id FROM lvl1 p WHERE p.name = v.parent_name),
           v.path, v.component, v.icon, v.sort, v.status, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint
    FROM (VALUES
        ('会员列表', 'member:user:query', 2, '会员中心', 'user', 'member/user/index', 'ant-design:user-outlined', 1, 1),
        ('会员标签', 'member:tag:query', 2, '会员中心', 'tag', 'member/tag/index', 'ant-design:tag-outlined', 2, 1),
        ('会员等级', 'member:level:query', 2, '会员中心', 'level', 'member/level/index', 'ant-design:trophy-outlined', 3, 1),
        ('会员分组', 'member:group:query', 2, '会员中心', 'group', 'member/group/index', 'ant-design:team-outlined', 4, 1),
        ('积分记录', 'member:point:query', 2, '会员中心', 'point/record', 'member/point/record/index', 'ant-design:star-outlined', 5, 1),
        ('签到配置', 'member:signin:query', 2, '会员中心', 'signin/config', 'member/signin/config/index', 'ant-design:calendar-outlined', 6, 1),
        ('签到记录', 'member:signin:query', 2, '会员中心', 'signin/record', 'member/signin/record/index', 'ant-design:check-circle-outlined', 7, 1),
        ('会员配置', 'member:config:query', 2, '会员中心', 'config', 'member/config/index', 'ant-design:setting-outlined', 8, 1),
        ('邀请码管理', 'member:invite-code:query', 2, '会员中心', 'invite/code', 'member/invite/code/index', NULL, 9, 1)
    ) AS v(name, permission, type, parent_name, path, component, icon, sort, status)
    RETURNING id, name
),
lvl3 AS (
    INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
    SELECT v.name, v.permission, v.type,
           (SELECT p.id FROM lvl2 p WHERE p.name = v.parent_name),
           v.path, v.component, v.icon, v.sort, v.status, (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint
    FROM (VALUES
        ('会员地址查询', 'member:address:query', 3, '会员列表', NULL, NULL, NULL, 2, 1),
        ('修改昵称词条', 'member:nickname:update', 3, '会员列表', NULL, NULL, NULL, 5, 1),
        ('昵称词库查询', 'member:nickname:query', 3, '会员列表', NULL, NULL, NULL, 3, 1),
        ('会员修改', 'member:user:update', 3, '会员列表', NULL, NULL, NULL, 1, 1),
        ('删除昵称词条', 'member:nickname:delete', 3, '会员列表', NULL, NULL, NULL, 6, 1),
        ('新增昵称词条', 'member:nickname:create', 3, '会员列表', NULL, NULL, NULL, 4, 1),
        ('重设会员密码', 'member:user:update-password', 3, '会员列表', NULL, NULL, NULL, 7, 1),
        ('删除会员标签', 'member:tag:delete', 3, '会员标签', NULL, NULL, NULL, 3, 1),
        ('新增会员标签', 'member:tag:create', 3, '会员标签', NULL, NULL, NULL, 1, 1),
        ('修改会员标签', 'member:tag:update', 3, '会员标签', NULL, NULL, NULL, 2, 1),
        ('新增会员等级', 'member:level:create', 3, '会员等级', NULL, NULL, NULL, 1, 1),
        ('删除会员等级', 'member:level:delete', 3, '会员等级', NULL, NULL, NULL, 3, 1),
        ('修改会员等级', 'member:level:update', 3, '会员等级', NULL, NULL, NULL, 2, 1),
        ('删除会员分组', 'member:group:delete', 3, '会员分组', NULL, NULL, NULL, 3, 1),
        ('新增会员分组', 'member:group:create', 3, '会员分组', NULL, NULL, NULL, 1, 1),
        ('修改会员分组', 'member:group:update', 3, '会员分组', NULL, NULL, NULL, 2, 1),
        ('新增签到配置', 'member:signin:create', 3, '签到配置', NULL, NULL, NULL, 2, 1),
        ('修改签到配置', 'member:signin:update', 3, '签到配置', NULL, NULL, NULL, 3, 1),
        ('删除签到配置', 'member:signin:delete', 3, '签到配置', NULL, NULL, NULL, 4, 1),
        ('修改会员配置', 'member:config:update', 3, '会员配置', NULL, NULL, NULL, 1, 1),
        ('新增邀请码', 'member:invite-code:create', 3, '邀请码管理', NULL, NULL, NULL, 2, 1),
        ('修改邀请码', 'member:invite-code:update', 3, '邀请码管理', NULL, NULL, NULL, 3, 1),
        ('删除邀请码', 'member:invite-code:delete', 3, '邀请码管理', NULL, NULL, NULL, 4, 1)
    ) AS v(name, permission, type, parent_name, path, component, icon, sort, status)
    RETURNING id, name
),
inserted AS (
        SELECT id, name FROM lvl1
        UNION ALL SELECT id, name FROM lvl2
        UNION ALL SELECT id, name FROM lvl3
)
INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT r.id, m.id, (extract(epoch from now()) * 1000)::bigint
FROM system_roles r
JOIN inserted m ON m.name IN (
        '会员中心',
        '会员修改',
        '会员分组',
        '会员列表',
        '会员地址查询',
        '会员标签',
        '会员等级',
        '会员配置',
        '修改会员分组',
        '修改会员标签',
        '修改会员等级',
        '修改会员配置',
        '修改昵称词条',
        '修改签到配置',
        '修改邀请码',
        '删除会员分组',
        '删除会员标签',
        '删除会员等级',
        '删除昵称词条',
        '删除签到配置',
        '删除邀请码',
        '新增会员分组',
        '新增会员标签',
        '新增会员等级',
        '新增昵称词条',
        '新增签到配置',
        '新增邀请码',
        '昵称词库查询',
        '积分记录',
        '签到记录',
        '签到配置',
        '邀请码管理',
        '重设会员密码'
)
WHERE r.code IN ('super_admin')
ON CONFLICT DO NOTHING;



-- nickname 词库 seed (NICK 填昵称引导 prefill); id/status/ts 走 default
INSERT INTO member_nickname_adjective (word) VALUES
('温柔'),
('温润'),
('温暖'),
('温煦'),
('温存'),
('温和'),
('温婉'),
('温雅'),
('清浅'),
('清雅'),
('清越'),
('清润'),
('清宁'),
('清澈'),
('清辉'),
('清绝'),
('清扬'),
('清和'),
('清欢'),
('清梦'),
('清歌'),
('清音'),
('清逸'),
('清韵'),
('清远'),
('清丽'),
('清芬'),
('清雪'),
('清霜'),
('清露'),
('清明'),
('清池'),
('清波'),
('清川'),
('清凉'),
('清秋'),
('清宵'),
('清旦'),
('清晨'),
('淡然'),
('淡漠'),
('淡薄'),
('淡雅'),
('淡墨'),
('淡蓝'),
('淡定'),
('淡泊'),
('淡远'),
('淡香'),
('淡影'),
('淡笑'),
('淡云'),
('淡月'),
('淡水'),
('淡情'),
('淡愁'),
('淡眉'),
('淡素'),
('淡淡'),
('恬淡'),
('恬静'),
('恬然'),
('恬美'),
('恬适'),
('恬安'),
('恬愉'),
('恬畅'),
('恬熙'),
('婉约'),
('婉转'),
('婉清'),
('婉若'),
('婉怡'),
('婉婉'),
('婉丽'),
('婉莹'),
('婉芸'),
('婉言'),
('朦胧'),
('朦朦'),
('朦昧'),
('朦影'),
('微醺'),
('微凉'),
('微暖'),
('微茫'),
('微亮'),
('微风'),
('微雨'),
('微光'),
('微醉'),
('微寒'),
('微雪'),
('微露'),
('微熹'),
('微馨'),
('微澜'),
('醺然'),
('醉意'),
('醉花'),
('醉墨'),
('醉月'),
('醉风'),
('醉雪'),
('醉竹'),
('醉柳'),
('醉吟'),
('旖旎'),
('缠绵'),
('缱绻'),
('缱怀'),
('葳蕤'),
('绰约'),
('含情'),
('含露'),
('含烟'),
('含霜'),
('含蕊'),
('含香'),
('含笑'),
('含黛'),
('脉脉'),
('惆怅'),
('凄迷'),
('落寞'),
('寂寥'),
('寂寞'),
('寂静'),
('寂寂'),
('寥落'),
('萧瑟'),
('萧索'),
('萧条'),
('萧萧'),
('萧然'),
('萧朗'),
('孑然'),
('孤独'),
('孤寂'),
('孤鸿'),
('孤舟'),
('孤云'),
('孤芳'),
('孤山'),
('孤月'),
('孤鹤'),
('独行'),
('独酌'),
('独醉'),
('独立'),
('独望'),
('独坐'),
('独歌'),
('独吟'),
('惊鸿'),
('惊羽'),
('惊雪'),
('惊雷'),
('惊霜'),
('飘渺'),
('缥缈'),
('迷离'),
('迷蒙'),
('迷醉'),
('迷漾'),
('迷茫'),
('缤纷'),
('纷飞'),
('纷扬'),
('纷扰'),
('纷然'),
('翩跹'),
('翩翩'),
('翩飞'),
('翩鸿'),
('翩然'),
('婆娑'),
('蹁跹'),
('逶迤'),
('迢遥'),
('迢迢'),
('迢递'),
('玲珑'),
('剔透'),
('冰雪'),
('冰心'),
('冰魄'),
('冰魂'),
('冰肌'),
('冰清'),
('冰洁'),
('冰寒'),
('冰封'),
('凝霜'),
('凝雪'),
('凝露'),
('凝眸'),
('凝望'),
('凝神'),
('凝思'),
('凝想'),
('明月'),
('朗月'),
('朗朗'),
('朗然'),
('明朗'),
('明亮'),
('明丽'),
('明媚'),
('明净'),
('明澈'),
('明远'),
('明晰'),
('澄澈'),
('澄明'),
('澄静'),
('澄碧'),
('澄黛'),
('澄空'),
('澄江'),
('澄潭'),
('澄心'),
('澄怀'),
('潇湘'),
('潇潇'),
('潇然'),
('潇朗'),
('风雅'),
('风骨'),
('风韵'),
('风华'),
('风流'),
('风采'),
('风度'),
('风姿'),
('风仪'),
('雅致'),
('雅韵'),
('雅意'),
('雅怀'),
('雅思'),
('雅趣'),
('雅言'),
('雅望'),
('雅集'),
('逸兴'),
('逸致'),
('逸趣'),
('逸怀'),
('逸品'),
('逸闲'),
('逸然'),
('逸群'),
('飘逸'),
('飘然'),
('飘忽'),
('飘扬'),
('飘香'),
('飘洒'),
('飘漾'),
('飘飘'),
('苍翠'),
('苍蓝'),
('苍青'),
('苍茫'),
('苍劲'),
('苍郁'),
('苍古'),
('苍润'),
('苍鸿'),
('绯红'),
('绯惘'),
('酡红'),
('酡颜'),
('胭脂'),
('朱砂'),
('丹墨'),
('朱红'),
('朱漆'),
('赤霞'),
('赤焰'),
('黛蓝'),
('黛色'),
('黛绿'),
('黛黄'),
('黛紫'),
('黛眉'),
('雪白'),
('雪青'),
('雪润'),
('雪意'),
('雪魂'),
('雪魄'),
('碧绿'),
('碧蓝'),
('碧色'),
('碧霄'),
('碧空'),
('碧浪'),
('碧潭'),
('翠绿'),
('翠青'),
('翠竹'),
('翠岭'),
('翠微'),
('翠华'),
('翠寒'),
('翠盖'),
('浅蓝'),
('浅黛'),
('浅碧'),
('浅紫'),
('浅红'),
('浅粉'),
('浅绿'),
('浅秋'),
('浅夏'),
('浅春'),
('浅冬'),
('浅笑'),
('浅唱'),
('浅吟'),
('浅斟'),
('晨曦'),
('晨光'),
('晨露'),
('晨钟'),
('晨星'),
('晨风'),
('晨雾'),
('朝霞'),
('朝阳'),
('朝露'),
('朝雾'),
('朝旭'),
('朝晖'),
('初阳'),
('初晓'),
('初露'),
('初雪'),
('初月'),
('初心'),
('初见'),
('初鸣'),
('初萌'),
('破晓'),
('拂晓'),
('晓风'),
('晓月'),
('晓星'),
('晓露'),
('晓雾'),
('晓寒'),
('晓妆'),
('晓光'),
('晓色'),
('暮霭'),
('暮色'),
('暮云'),
('暮雨'),
('暮春'),
('暮秋'),
('暮鸿'),
('暮鼓'),
('暮霞'),
('暮夜'),
('暮天'),
('晚晴'),
('晚秋'),
('晚霞'),
('晚风'),
('晚照'),
('晚归'),
('晚钟'),
('夜色'),
('夜风'),
('夜露'),
('夜雨'),
('夜雪'),
('夜阑'),
('夜深'),
('夜半'),
('夜月'),
('夜寒'),
('夜笛'),
('夜梦'),
('夜静'),
('月色'),
('月华'),
('月夜'),
('月白'),
('月隐'),
('月光'),
('月辉'),
('月明'),
('月影'),
('月落'),
('月皎'),
('月清'),
('月圆'),
('星辰'),
('星河'),
('星夜'),
('星辉'),
('星光'),
('星灿'),
('星海'),
('星汉'),
('星雨'),
('星空'),
('银光'),
('银汉'),
('银河'),
('银辉'),
('银白'),
('银影'),
('银烛'),
('银箭'),
('落日'),
('斜阳'),
('残阳'),
('残月'),
('残红'),
('残云'),
('残雪'),
('残烟'),
('残春'),
('残夏'),
('残秋'),
('残冬'),
('落英'),
('落樱'),
('落雪'),
('落叶'),
('落霞'),
('落晖'),
('落星'),
('落花'),
('秋霜'),
('秋风'),
('秋雨'),
('秋意'),
('秋月'),
('秋水'),
('秋色'),
('秋日'),
('秋宵'),
('秋韵'),
('春风'),
('春晓'),
('春潮'),
('春深'),
('春寒'),
('春暖'),
('春日'),
('春宵'),
('春雨'),
('春雪'),
('春色'),
('春意'),
('春韵'),
('春浅'),
('春晚'),
('春去'),
('春归'),
('夏夜'),
('夏雨'),
('夏蝉'),
('夏荷'),
('夏荫'),
('夏风'),
('夏阳'),
('夏雾'),
('冬雪'),
('冬月'),
('冬晨'),
('冬阳'),
('冬夜'),
('冬寒'),
('冬意'),
('冬韵'),
('冬岭'),
('冬云'),
('山月'),
('山色'),
('山雨'),
('山风'),
('山影'),
('山岚'),
('山霭'),
('山高'),
('山远'),
('山静'),
('山幽'),
('江月'),
('江风'),
('江南'),
('江北'),
('江岸'),
('江雪'),
('江湖'),
('海月'),
('海风'),
('海色'),
('海上'),
('海岸'),
('海角'),
('海天'),
('湖月'),
('湖光'),
('湖心'),
('湖色'),
('湖影'),
('湖波'),
('湖烟'),
('林涛'),
('林深'),
('林泉'),
('林月'),
('林风'),
('林雨'),
('松涛'),
('松风'),
('松月'),
('松烟'),
('松鹤'),
('松雪'),
('竹林'),
('竹月'),
('竹风'),
('竹露'),
('竹影'),
('竹梦'),
('竹烟'),
('梅雪'),
('梅影'),
('梅香'),
('梅梦'),
('兰心'),
('兰露'),
('兰风'),
('兰韵'),
('兰烟'),
('桂雨'),
('桂月'),
('桂魄'),
('桂魂'),
('桃源'),
('桃林'),
('桃花'),
('杏雨'),
('烟雨'),
('烟波'),
('烟岚'),
('烟柳'),
('烟霞'),
('烟云'),
('天青'),
('天蓝'),
('天涯'),
('天际'),
('天朗'),
('天清'),
('玉色'),
('玉影'),
('玉雪'),
('玉露'),
('玉光'),
('玉魂'),
('玉魄'),
('玉颜'),
('霁色'),
('霁光'),
('霁月'),
('霁雪'),
('霁雨'),
('风轻'),
('风暖'),
('风柔'),
('风寒'),
('风清'),
('风静'),
('雨疏'),
('雨丝'),
('雨痕'),
('雨意'),
('雨韵'),
('夜沉'),
('夜悠'),
('寒鸦'),
('寒梅'),
('寒星'),
('寒霜'),
('寒露'),
('寒月'),
('寒鸿'),
('寒泉'),
('寒夜'),
('古朴'),
('古拙'),
('古意'),
('古韵'),
('古色'),
('古道'),
('古风'),
('古典'),
('古雅'),
('深秋'),
('深夜'),
('深谷'),
('深巷'),
('深院'),
('深情'),
('深意'),
('深心'),
('深思'),
('深远'),
('长河'),
('长歌'),
('长风'),
('长亭'),
('长卷'),
('长夜'),
('长安'),
('长生'),
('长青'),
('长留')
ON CONFLICT (word) DO NOTHING;

-- nickname 词库 seed (NICK 填昵称引导 prefill); id/status/ts 走 default
INSERT INTO member_nickname_noun (word) VALUES
('明月'),
('朗月'),
('弯月'),
('新月'),
('满月'),
('初月'),
('晓月'),
('孤月'),
('落月'),
('玉月'),
('霁月'),
('寒月'),
('皎月'),
('皓月'),
('银月'),
('凉月'),
('月华'),
('月色'),
('月光'),
('月影'),
('星辰'),
('繁星'),
('北斗'),
('南柯'),
('启明'),
('流星'),
('晨星'),
('夜星'),
('银河'),
('河汉'),
('天河'),
('银汉'),
('辰宿'),
('寒星'),
('朝阳'),
('夕阳'),
('落日'),
('晨曦'),
('暮霞'),
('朝霞'),
('晚霞'),
('极光'),
('曙光'),
('晨光'),
('夕照'),
('晚照'),
('霞光'),
('天光'),
('辉光'),
('微光'),
('萤光'),
('海棠'),
('桃花'),
('梨花'),
('桂花'),
('茉莉'),
('玫瑰'),
('牡丹'),
('芍药'),
('月季'),
('玉兰'),
('紫罗'),
('丁香'),
('紫薇'),
('紫荆'),
('水仙'),
('百合'),
('兰花'),
('梅花'),
('秋菊'),
('野菊'),
('雏菊'),
('野薇'),
('蔷薇'),
('凌霄'),
('凌波'),
('合欢'),
('木槿'),
('扶桑'),
('紫藤'),
('锦绣'),
('木兰'),
('迎春'),
('含羞'),
('含笑'),
('含露'),
('木樨'),
('银桂'),
('丹桂'),
('金桂'),
('红梅'),
('腊梅'),
('寒梅'),
('疏梅'),
('孤梅'),
('老梅'),
('古梅'),
('野梅'),
('早梅'),
('春兰'),
('夏兰'),
('秋兰'),
('冬兰'),
('幽兰'),
('惠兰'),
('翠竹'),
('碧竹'),
('翠柏'),
('苍松'),
('古松'),
('野桂'),
('秋桂'),
('青莲'),
('白莲'),
('红莲'),
('碧莲'),
('雪莲'),
('青山'),
('苍山'),
('远山'),
('近山'),
('山影'),
('山峦'),
('山涧'),
('山泉'),
('山溪'),
('山岚'),
('山色'),
('山雪'),
('山风'),
('江南'),
('江北'),
('江流'),
('江波'),
('江月'),
('江岸'),
('江雪'),
('江风'),
('江湖'),
('江畔'),
('海角'),
('海上'),
('海岸'),
('海风'),
('海月'),
('海天'),
('海蓝'),
('海色'),
('海雪'),
('碧海'),
('沧海'),
('碧波'),
('碧泉'),
('碧潭'),
('清泉'),
('温泉'),
('玉泉'),
('灵泉'),
('古井'),
('古潭'),
('古池'),
('古渡'),
('古桥'),
('古道'),
('古寺'),
('古庙'),
('古亭'),
('古塔'),
('古园'),
('古巷'),
('古城'),
('古风'),
('古意'),
('古韵'),
('古卷'),
('古简'),
('烟波'),
('烟雨'),
('烟霞'),
('烟岚'),
('烟柳'),
('烟云'),
('烟雾'),
('烟水'),
('烟村'),
('烟岭'),
('玉笛'),
('玉箫'),
('玉珏'),
('玉佩'),
('玉珮'),
('玉砚'),
('玉墨'),
('玉笺'),
('玉简'),
('玉璞'),
('玉璧'),
('玉壶'),
('玉杯'),
('玉钗'),
('玉簪'),
('玉环'),
('玉钏'),
('玉镜'),
('玉樽'),
('玉觞'),
('古砚'),
('古琴'),
('古笛'),
('古剑'),
('古玉'),
('古鉴'),
('古墨'),
('古笺'),
('古酒'),
('古香'),
('古镜'),
('锦书'),
('锦瑟'),
('锦帆'),
('锦舟'),
('锦字'),
('锦笺'),
('锦衣'),
('锦帐'),
('锦囊'),
('锦袍'),
('锦缎'),
('锦绮'),
('锦扇'),
('茶盏'),
('茶碗'),
('茶盅'),
('茶炉'),
('茶烟'),
('酒杯'),
('酒盏'),
('酒壶'),
('酒旗'),
('酒帘'),
('酒痕'),
('酒色'),
('酒香'),
('酒令'),
('竹简'),
('铜镜'),
('明镜'),
('书页'),
('书卷'),
('诗册'),
('诗笺'),
('诗笔'),
('诗稿'),
('诗心'),
('诗酒'),
('砚墨'),
('墨香'),
('墨痕'),
('墨色'),
('墨韵'),
('墨竹'),
('墨梅'),
('墨兰'),
('笔意'),
('笔锋'),
('笔端'),
('笔花'),
('笺心'),
('笺香'),
('才子'),
('佳人'),
('书生'),
('隐者'),
('孤鸿'),
('游子'),
('旅人'),
('行者'),
('客人'),
('远客'),
('羁旅'),
('伊人'),
('故人'),
('故友'),
('旧友'),
('旅愁'),
('客愁'),
('旧梦'),
('旧事'),
('往事'),
('前尘'),
('前缘'),
('玉人'),
('月人'),
('雪人'),
('醉人'),
('痴人'),
('旧识'),
('新知'),
('知己'),
('挚友'),
('良朋'),
('雅士'),
('逸士'),
('居士'),
('道人'),
('仙翁'),
('野老'),
('樵夫'),
('渔翁'),
('墨客'),
('骚人'),
('词客'),
('诗客'),
('酒客'),
('山客'),
('野客'),
('风骨'),
('气韵'),
('格调'),
('雅致'),
('雅韵'),
('雅意'),
('逸兴'),
('逸致'),
('逸趣'),
('逸品'),
('性灵'),
('灵犀'),
('灵秀'),
('灵动'),
('灵气'),
('雅集'),
('清音'),
('绝响'),
('余音'),
('玄思'),
('玄机'),
('玄妙'),
('雅怀'),
('逸怀'),
('幽情'),
('幽意'),
('幽思'),
('幽梦'),
('幽谷'),
('幽径'),
('幽居'),
('雅望'),
('雅思'),
('雅趣'),
('雅言'),
('逸言'),
('吟咏'),
('吟唱'),
('低吟'),
('浅唱'),
('清啸'),
('长啸'),
('笑语'),
('笑意'),
('笑颜'),
('笑容'),
('笑靥'),
('眼眸'),
('眉眼'),
('眉宇'),
('眉弯'),
('眉黛'),
('眉峰'),
('眉间'),
('春风'),
('春雨'),
('春晓'),
('春潮'),
('春日'),
('春宵'),
('春寒'),
('春雪'),
('春色'),
('春意'),
('春韵'),
('春浅'),
('夏雨'),
('夏夜'),
('夏蝉'),
('夏荷'),
('夏荫'),
('夏风'),
('夏阳'),
('夏雾'),
('秋叶'),
('秋月'),
('秋水'),
('秋霜'),
('秋风'),
('秋雁'),
('秋色'),
('秋意'),
('秋宵'),
('秋韵'),
('秋雨'),
('冬雪'),
('冬月'),
('冬晨'),
('冬阳'),
('冬夜'),
('冬寒'),
('冬意'),
('冬韵'),
('白鹭'),
('锦鲤'),
('凤凰'),
('玄鸟'),
('朱鹊'),
('燕子'),
('紫燕'),
('雏燕'),
('双燕'),
('孤雁'),
('南雁'),
('北雁'),
('归雁'),
('惊鸿'),
('惊鹊'),
('喜鹊'),
('翠鸟'),
('翡翠'),
('锦雉'),
('锦鸡'),
('苍鹰'),
('白鹿'),
('白马'),
('雪驹'),
('玉马'),
('骐骥'),
('孤鹤'),
('仙鹤'),
('玄鹤'),
('野鹤'),
('丹凤'),
('鸿雁'),
('鸿鹄'),
('鸿影'),
('青鸟'),
('青鸾'),
('紫凤'),
('神龙'),
('青龙'),
('苍龙'),
('潜龙'),
('飞龙'),
('游龙'),
('霓裳'),
('霓虹'),
('霁霓'),
('彩虹'),
('锦云'),
('彩云'),
('祥云'),
('青云'),
('紫云'),
('红云'),
('白云'),
('碧云'),
('苍云'),
('孤云'),
('流云'),
('云海'),
('云岚'),
('云端'),
('云霓'),
('雨珠'),
('雨痕'),
('雨幕'),
('雨帘'),
('雨意'),
('雨韵'),
('雨声'),
('露珠'),
('露华'),
('露水'),
('露霜'),
('霜花'),
('霜痕'),
('霜色'),
('霜林'),
('雪花'),
('雪片'),
('雪海'),
('雪原'),
('雪意'),
('雪魂'),
('雪魄'),
('雪迹'),
('风韵'),
('风华'),
('风流'),
('风采'),
('风度'),
('风姿'),
('风仪'),
('风轻'),
('风暖'),
('风柔'),
('风寒'),
('风清'),
('风静'),
('风声'),
('风影'),
('风踪'),
('浪迹'),
('浪客'),
('浪游'),
('浪潮'),
('浪花'),
('浪痕'),
('长河'),
('长歌'),
('长风'),
('长亭'),
('长卷'),
('长夜'),
('扁舟'),
('轻舟'),
('画舫'),
('木兰舟'),
('渔舟'),
('归舟'),
('钓舟'),
('柴扉'),
('柴门'),
('柴桑'),
('柴荆'),
('帘幕'),
('珠帘'),
('绣帘'),
('垂帘'),
('卷帘'),
('玉书'),
('玉笈'),
('青笺'),
('红笺'),
('彩笺'),
('香笺'),
('新梦'),
('残梦'),
('浮梦'),
('幻梦'),
('酣梦'),
('甜梦'),
('游梦'),
('香梦'),
('寒梦'),
('归梦'),
('归路'),
('归人'),
('归云'),
('归鸿'),
('归心'),
('归途'),
('归隐'),
('归田'),
('归园'),
('归乡'),
('故园'),
('故乡'),
('故宅'),
('故苑'),
('故里'),
('故山'),
('故水'),
('旧宅'),
('旧雨'),
('旧时'),
('旧地'),
('旧岁'),
('旧情'),
('旧约'),
('旧诗'),
('旧居'),
('长安'),
('洛阳'),
('姑苏'),
('建康'),
('临安'),
('钱塘'),
('扬州'),
('广陵'),
('塞北'),
('天涯'),
('青冢'),
('驿路'),
('驿亭'),
('驿站'),
('关山'),
('关河'),
('关塞'),
('塞外'),
('塞月'),
('塞鸿'),
('塞云')
ON CONFLICT (word) DO NOTHING;


-- ─────────────────────────────────────────────────────────────
-- 原 V003__sign_in_cash_amount.sql
-- ─────────────────────────────────────────────────────────────

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

-- 绑到超级管理员(1)/管理员(2);


-- ─────────────────────────────────────────────────────────────
-- 原 V005__invite_codes.sql
-- ─────────────────────────────────────────────────────────────

-- MEMBER_INVITE_CODE v1.0:邀请码 + 邀请记录(注册绑定/补填绑定,自动加好友状态)。
CREATE TABLE IF NOT EXISTS member_invite_codes (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id BIGINT,
    max_uses      INT NOT NULL DEFAULT 0,
    used_count    INT NOT NULL DEFAULT 0,
    status        SMALLINT NOT NULL DEFAULT 1,
    expires_at    BIGINT,
    remark        VARCHAR(255),
    created_by    BIGINT NOT NULL,
    created_at    BIGINT NOT NULL,
    updated_at    BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_invite_codes_owner ON member_invite_codes(owner_user_id);

CREATE TABLE IF NOT EXISTS member_invite_records (
    id                         BIGSERIAL PRIMARY KEY,
    code_id                    BIGINT NOT NULL,
    code                       VARCHAR(32) NOT NULL,
    inviter_user_id            BIGINT,
    invitee_user_id            BIGINT NOT NULL UNIQUE,
    register_mode              VARCHAR(32) NOT NULL,
    register_identifier_masked VARCHAR(64),
    bind_scene                 SMALLINT NOT NULL,
    auto_friend_status         SMALLINT NOT NULL DEFAULT 0,
    auto_friend_error          VARCHAR(255),
    bound_at                   BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_member_invite_records_code ON member_invite_records(code_id);
CREATE INDEX IF NOT EXISTS idx_member_invite_records_inviter ON member_invite_records(inviter_user_id);


-- ─────────────────────────────────────────────────────────────
-- 原 V009__member_session_version.sql
-- ─────────────────────────────────────────────────────────────

-- 会话版本：builtin 身份后端的 token 携带 session_version，改密/登出全端时递增使旧 token 失效。
ALTER TABLE member_users ADD COLUMN IF NOT EXISTS session_version BIGINT NOT NULL DEFAULT 0;


-- ─────────────────────────────────────────────────────────────
-- 原 V010__member_id_sequence.sql
-- ─────────────────────────────────────────────────────────────

-- Builtin identity backend uses a plain database sequence for member ids
-- (small incrementing integers, JS-safe). Privchat mode still inserts the
-- external server user_id explicitly, which bypasses this sequence.
CREATE SEQUENCE IF NOT EXISTS member_users_id_seq AS bigint START WITH 10000 INCREMENT BY 1;

-- Keep the sequence ahead of existing rows, but ignore any oversized ids left
-- over from earlier experiments (JS Number is only exact below 2^53); floor 10000.
SELECT setval(
    'member_users_id_seq',
    GREATEST(
        (SELECT COALESCE(MAX(id), 0) FROM public.member_users WHERE id < 9007199254740991),
        10000
    )
);


-- ─────────────────────────────────────────────────────────────
-- 原 V011__invite_welcome_message.sql
-- ─────────────────────────────────────────────────────────────

-- MEMBER_INVITE_CODE:邀请码级自动打招呼用语(运营在后台按码配置;空=用全局 conf 兜底)。
ALTER TABLE member_invite_codes ADD COLUMN IF NOT EXISTS welcome_message TEXT;


-- ─────────────────────────────────────────────────────────────
-- 原 V013__member_guest_accounts.sql
-- ─────────────────────────────────────────────────────────────

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


-- ─────────────────────────────────────────────────────────────
-- 原 V014__member_mobile_unique.sql
-- ─────────────────────────────────────────────────────────────

-- 手机号唯一约束。
--
-- 手机号是登录凭据之一：短信登录靠 `oneWhere { mobile eq ... }` 反查账号，重复了就没法
-- 判断是谁。此前这个唯一性只活在应用层（bindMobileWithoutVerification 里的一次查询），
-- 数据库上只有一个普通索引 `idx_member_users_mobile` —— 并发下两个人同时绑同一个号，
-- 两次查询都会说"没人用"，然后双双写入。
--
-- 现在加代价最低：线上 2102 个账号里只有 11 个填了手机号，且互不重复。等积累到几万条
-- 再补，就得先清洗数据。
--
-- 只约束非空值：绝大多数账号没有手机号，NULL 在 Postgres 的唯一索引里互不冲突，但空串
-- 会——历史上有没有写进过空串不确定，用 WHERE 明确排除掉，别让这条迁移在某个环境上炸。
--
-- 保留原来的 idx_member_users_mobile 不动：它是普通索引，查询计划可能已经依赖它，而唯一
-- 索引承担的是约束职责。两者并存的冗余远比"删了发现某个查询变慢"便宜。

SET search_path = public;

CREATE UNIQUE INDEX IF NOT EXISTS uq_member_users_mobile
    ON member_users (mobile)
    WHERE mobile IS NOT NULL AND mobile <> '';


-- ─────────────────────────────────────────────────────────────
-- 原 V015__member_single_device.sql
-- ─────────────────────────────────────────────────────────────

-- 单设备登录：记录会员当前占用的设备
--
-- 换设备登录时与这一列比对，不同则自增 session_version 顶掉上一台。
-- 只记「当前这一台」而不是设备列表：需求是同时只允许一台，多留历史反而要额外定义
-- 「哪台才算当前」，而那正是这一列本身。要做登录历史应另建审计表，与鉴权解耦。
ALTER TABLE member_users
  ADD COLUMN current_device_id VARCHAR(64) NULL;
