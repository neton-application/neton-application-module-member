-- V006 seeded the invite-code menu at id=307, which collides with the member
-- config menu on deployments where 307 was already taken (ON CONFLICT skipped
-- the page row while the 3070-3073 buttons still landed). Converge both
-- histories to id=308.
DELETE FROM system_role_menus rm
 WHERE rm.menu_id = 307
   AND EXISTS (SELECT 1 FROM system_menus m WHERE m.id = 307 AND m.permission = 'member:invite-code:list');
DELETE FROM system_menus WHERE id = 307 AND permission = 'member:invite-code:list';

INSERT INTO system_menus (id, parent_id, name, path, component, permission, type, sort, status, created_at, updated_at)
VALUES (308, 3, '邀请码管理', 'invite/code', 'member/invite/code/index', 'member:invite-code:list', 2, 9, 1,
        (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint)
ON CONFLICT (id) DO NOTHING;

UPDATE system_menus SET parent_id = 308 WHERE id IN (3070, 3071, 3072, 3073);

INSERT INTO system_role_menus (role_id, menu_id)
SELECT r.id, 308
  FROM system_roles r
 WHERE r.id IN (1, 2)
   AND NOT EXISTS (SELECT 1 FROM system_role_menus rm WHERE rm.role_id = r.id AND rm.menu_id = 308);
