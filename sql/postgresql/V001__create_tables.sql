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

