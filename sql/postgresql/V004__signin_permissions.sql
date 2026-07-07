-- 签到管理权限点对齐 controller @Permission("member:signin:*")(MEMBER_SIGN_IN_REWARD 补丁③)。
-- 旧 seed 的 member:signin-config:list / member:signin-record:list 与 controller 校验值不一致,
-- 且缺 create/update/delete/query 按钮权限点 —— 管理员组即使绑了菜单也 Permission denied。
UPDATE system_menus SET permission = 'member:signin:list' WHERE id = 305 AND permission = 'member:signin-config:list';
UPDATE system_menus SET permission = 'member:signin:page' WHERE id = 306 AND permission = 'member:signin-record:list';

INSERT INTO system_menus (id, parent_id, name, permission, type, sort, status, created_at, updated_at)
VALUES
 (3050, 305, '签到配置查询', 'member:signin:query', 3, 1, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3051, 305, '新增签到配置', 'member:signin:create', 3, 2, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3052, 305, '修改签到配置', 'member:signin:update', 3, 3, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint),
 (3053, 305, '删除签到配置', 'member:signin:delete', 3, 4, 1, (extract(epoch from now())*1000)::bigint, (extract(epoch from now())*1000)::bigint)
ON CONFLICT (id) DO NOTHING;

-- 绑到超级管理员(1)/管理员(2);其它角色由运营在角色管理页自行勾选。
INSERT INTO system_role_menus (role_id, menu_id)
SELECT r.id, m.id FROM system_roles r CROSS JOIN system_menus m
WHERE r.id IN (1, 2) AND m.id IN (305, 306, 3050, 3051, 3052, 3053)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('system_menus','id'), (SELECT MAX(id) FROM system_menus));
