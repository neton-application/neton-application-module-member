-- MEMBER_INVITE_CODE §7:菜单 + 权限点(与 controller @Permission 一字不差,一次配齐)。
INSERT INTO system_menus (id, parent_id, name, path, component, permission, type, sort, status, created_at, updated_at)
VALUES
 (307, 3, '邀请码管理', 'invite/code', 'member/invite/code/index', 'member:invite-code:list', 2, 8, 1,
  (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_menus (id, parent_id, name, permission, type, sort, status, created_at, updated_at)
VALUES
 (3070, 307, '邀请码查询', 'member:invite-code:query', 3, 1, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3071, 307, '新增邀请码', 'member:invite-code:create', 3, 2, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3072, 307, '修改邀请码', 'member:invite-code:update', 3, 3, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3073, 307, '删除邀请码', 'member:invite-code:delete', 3, 4, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint)
ON CONFLICT (id) DO NOTHING;

INSERT INTO system_role_menus (role_id, menu_id)
SELECT r.id, m.id FROM system_roles r CROSS JOIN system_menus m
WHERE r.id IN (1, 2) AND m.id IN (307, 3070, 3071, 3072, 3073)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('system_menus','id'), (SELECT MAX(id) FROM system_menus));
