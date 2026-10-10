-- =============================================================================
-- member V004：成长数据交接 + 清理全新安装的残留
--
-- 账号能力已拆到 account 模块（account 的 V001 把 member_users 等表改名接管为 account_*）。
-- member 只留会员成长；它在 account_users 上挂着的四个成长字段，在这里搬到 member_profiles。
--
-- 为什么复制与删列都在这里、而不在 account：迁移按模块拓扑序整体执行，account 的全部脚本
-- 先跑完 member 才开始。若 account 自己删列，member 还没来得及把数据复制走。
-- =============================================================================
SET search_path = public;

-- ── 1. 会员档案 ─────────────────────────────────────────────────────────────
-- 自带 bigserial 主键 + user_id 唯一：ORM 的 insert 会剥掉调用方给的 id，用账号 id 当主键
-- 就得绕开 ORM 写原生 SQL。档案惰性创建，没产生过成长数据的账号没有这一行。
CREATE TABLE IF NOT EXISTS member_profiles (
    id         bigserial PRIMARY KEY,
    user_id    bigint NOT NULL,
    level_id   bigint,
    experience bigint DEFAULT 0 NOT NULL,
    point      integer DEFAULT 0 NOT NULL,
    group_id   bigint,
    created_at bigint DEFAULT 0 NOT NULL,
    updated_at bigint DEFAULT 0 NOT NULL,
    CONSTRAINT member_profiles_user_uq UNIQUE (user_id)
);
CREATE INDEX IF NOT EXISTS idx_member_profiles_level ON member_profiles (level_id);
CREATE INDEX IF NOT EXISTS idx_member_profiles_group ON member_profiles (group_id);

-- ── 2. 存量库：复制 → 校验 → 删列 ──────────────────────────────────────────
-- 只复制「有成长数据」的账号（任一字段非默认）。校验逐行比对四个字段，数量对不上就
-- RAISE 中止，**绝不在没确认复制完整的情况下删列**。
DO $$
DECLARE
    expected bigint;
    matched  bigint;
BEGIN
    IF to_regclass('public.account_users') IS NOT NULL AND EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = 'account_users' AND column_name = 'point'
    ) THEN
        INSERT INTO member_profiles (user_id, level_id, experience, point, group_id, created_at, updated_at)
        SELECT id, level_id, experience, point, group_id, created_at, updated_at
          FROM account_users
         WHERE level_id IS NOT NULL OR experience <> 0 OR point <> 0 OR group_id IS NOT NULL
        ON CONFLICT (user_id) DO NOTHING;

        SELECT count(*) INTO expected
          FROM account_users
         WHERE level_id IS NOT NULL OR experience <> 0 OR point <> 0 OR group_id IS NOT NULL;

        SELECT count(*) INTO matched
          FROM account_users a
          JOIN member_profiles p ON p.user_id = a.id
         WHERE (a.level_id IS NOT NULL OR a.experience <> 0 OR a.point <> 0 OR a.group_id IS NOT NULL)
           AND a.level_id IS NOT DISTINCT FROM p.level_id
           AND a.experience = p.experience
           AND a.point = p.point
           AND a.group_id IS NOT DISTINCT FROM p.group_id;

        IF matched <> expected THEN
            RAISE EXCEPTION 'member V004: growth data copy mismatch (expected %, matched %); columns NOT dropped', expected, matched;
        END IF;

        ALTER TABLE account_users
            DROP COLUMN level_id,
            DROP COLUMN experience,
            DROP COLUMN point,
            DROP COLUMN group_id;
    END IF;
END $$;

-- ── 3. 全新安装的残留 ────────────────────────────────────────────────────────
-- member 的 V001–V003 已执行过、带校验和，一个字节都不能改，所以全新安装时它们仍会建出
-- 旧名的身份表。正本已由 account 按终态建好，这些是空壳（昵称词库是同一份种子的重复）。
-- 存量库上它们早已被 account 改名接管、不存在，下面全部是 no-op。
DO $$
BEGIN
    IF to_regclass('public.account_users') IS NULL THEN
        RETURN;  -- 没装 account 不可能走到这里（member 依赖它）；保险起见什么都不删
    END IF;

    IF to_regclass('public.member_users') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM member_users) THEN
            RAISE EXCEPTION 'member V004: member_users still has rows while account_users exists; refusing to drop';
        END IF;
        DROP TABLE member_users;
        DROP SEQUENCE IF EXISTS member_users_id_seq;
    END IF;

    IF to_regclass('public.member_login_devices') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM member_login_devices) THEN
            RAISE EXCEPTION 'member V004: member_login_devices still has rows while account_login_devices exists; refusing to drop';
        END IF;
        DROP TABLE member_login_devices;
    END IF;

    -- 词库只在正本至少同样完整时才删重复的那份。
    -- 必须嵌套 IF：PL/pgSQL 的 AND 不保证短路，表不存在时引用它的子查询照样会报错。
    IF to_regclass('public.member_nickname_adjective') IS NOT NULL THEN
        IF (SELECT count(*) FROM account_nickname_adjective) >= (SELECT count(*) FROM member_nickname_adjective) THEN
            DROP TABLE member_nickname_adjective;
        END IF;
    END IF;
    IF to_regclass('public.member_nickname_noun') IS NOT NULL THEN
        IF (SELECT count(*) FROM account_nickname_noun) >= (SELECT count(*) FROM member_nickname_noun) THEN
            DROP TABLE member_nickname_noun;
        END IF;
    END IF;
END $$;

-- ── 4. 后台菜单 ────────────────────────────────────────────────────────────
-- 4.1 全新安装：member V001 种下的旧身份菜单（「会员列表」及其账号类按钮）在 account V001
--     已建好正本后成了重复。先把仍属于 member 的「会员地址查询」改挂到「账号列表」下
--     （存量库上它本来就随「会员列表」一起被改挂过去了），再删重复项。
UPDATE system_menus
   SET parent_id = (SELECT id FROM system_menus WHERE permission = 'account:user:query' AND type = 2 ORDER BY id LIMIT 1)
 WHERE permission = 'member:address:query'
   AND parent_id IN (SELECT id FROM system_menus WHERE permission = 'member:user:query')
   AND EXISTS (SELECT 1 FROM system_menus WHERE permission = 'account:user:query' AND type = 2);

DELETE FROM system_role_menus
 WHERE menu_id IN (SELECT id FROM system_menus WHERE permission IN (
        'member:user:query', 'member:user:update', 'member:user:update-password',
        'member:nickname:query', 'member:nickname:create', 'member:nickname:update', 'member:nickname:delete'));
DELETE FROM system_menus
 WHERE permission IN (
        'member:user:query', 'member:user:update', 'member:user:update-password',
        'member:nickname:query', 'member:nickname:create', 'member:nickname:update', 'member:nickname:delete');

-- 4.2 会员档案的两个权限点。挂在「账号列表」下：等级、积分、分组都在账号详情里操作。
INSERT INTO system_menus (name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
SELECT v.name, v.permission, 3,
       (SELECT id FROM system_menus WHERE permission = 'account:user:query' AND type = 2 ORDER BY id LIMIT 1),
       NULL, NULL, NULL, v.sort, 1,
       (extract(epoch from now()) * 1000)::bigint, (extract(epoch from now()) * 1000)::bigint
FROM (VALUES
    ('会员档案查询', 'member:profile:query', 10),
    ('会员档案修改', 'member:profile:update', 11)
) AS v(name, permission, sort)
WHERE NOT EXISTS (SELECT 1 FROM system_menus m WHERE m.permission = v.permission)
  AND EXISTS (SELECT 1 FROM system_menus WHERE permission = 'account:user:query' AND type = 2);

-- 4.3 授权。拆分前改等级 / 改积分用的是「会员修改」权限，现在单独成了 member:profile:update；
--     原来能改会员的角色（如今持有 account:user:update）照样能改档案，能力不因拆分丢失。
INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT r.id, m.id, (extract(epoch from now()) * 1000)::bigint
FROM system_roles r
JOIN system_menus m ON m.permission IN ('member:profile:query', 'member:profile:update')
WHERE r.code IN ('super_admin')
  -- system_role_menus 没有 (role_id, menu_id) 唯一约束，ON CONFLICT 拦不住重复，只能显式判存在。
  AND NOT EXISTS (SELECT 1 FROM system_role_menus x WHERE x.role_id = r.id AND x.menu_id = m.id);

INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT DISTINCT rm.role_id, prof.id, (extract(epoch from now()) * 1000)::bigint
FROM system_role_menus rm
JOIN system_menus src ON src.id = rm.menu_id
JOIN system_menus prof ON (src.permission = 'account:user:update' AND prof.permission = 'member:profile:update')
                       OR (src.permission = 'account:user:query'  AND prof.permission = 'member:profile:query')
WHERE NOT EXISTS (SELECT 1 FROM system_role_menus x WHERE x.role_id = rm.role_id AND x.menu_id = prof.id);
