-- Normalize member read permissions to resource-level `query` capabilities.
-- Route shapes such as page/list/get are transport details and must not create
-- separate RBAC capabilities for the same resource.

WITH canonical(id, permission) AS (
    VALUES
        (300::bigint, 'member:user:query'),
        (301::bigint, 'member:tag:query'),
        (302::bigint, 'member:level:query'),
        (303::bigint, 'member:group:query'),
        (304::bigint, 'member:point:query'),
        (305::bigint, 'member:signin:query'),
        (306::bigint, 'member:signin:query'),
        (307::bigint, 'member:config:query'),
        (308::bigint, 'member:invite-code:query')
)
UPDATE system_menus menu
   SET permission = canonical.permission,
       updated_at = (extract(epoch from now()) * 1000)::bigint
  FROM canonical
 WHERE menu.id = canonical.id
   AND menu.permission IS DISTINCT FROM canonical.permission;

-- V004/V006 created query button nodes below page menus. Once the page itself
-- carries the query capability these two nodes are redundant.
DELETE FROM system_role_menus
 WHERE menu_id IN (3050, 3070)
   AND EXISTS (
       SELECT 1
         FROM system_menus menu
        WHERE menu.id = system_role_menus.menu_id
          AND menu.permission IN ('member:signin:query', 'member:invite-code:query')
   );

DELETE FROM system_menus
 WHERE id IN (3050, 3070)
   AND permission IN ('member:signin:query', 'member:invite-code:query');

-- Query permissions live on page nodes. Only mutations and auxiliary user
-- resources remain as button capabilities.
WITH required(parent_id, name, permission, sort) AS (
    VALUES
        (300::bigint, '会员修改',       'member:user:update',      1),
        (300::bigint, '会员地址查询',   'member:address:query',    2),
        (300::bigint, '昵称词库查询',   'member:nickname:query',   3),
        (300::bigint, '新增昵称词条',   'member:nickname:create',  4),
        (300::bigint, '修改昵称词条',   'member:nickname:update',  5),
        (300::bigint, '删除昵称词条',   'member:nickname:delete',  6),

        (301::bigint, '新增会员标签',   'member:tag:create',       1),
        (301::bigint, '修改会员标签',   'member:tag:update',       2),
        (301::bigint, '删除会员标签',   'member:tag:delete',       3),

        (302::bigint, '新增会员等级',   'member:level:create',     1),
        (302::bigint, '修改会员等级',   'member:level:update',     2),
        (302::bigint, '删除会员等级',   'member:level:delete',     3),

        (303::bigint, '新增会员分组',   'member:group:create',     1),
        (303::bigint, '修改会员分组',   'member:group:update',     2),
        (303::bigint, '删除会员分组',   'member:group:delete',     3),

        (307::bigint, '修改会员配置',   'member:config:update',    1)
)
INSERT INTO system_menus (
    parent_id, name, permission, type, sort, status, created_at, updated_at
)
SELECT
    required.parent_id,
    required.name,
    required.permission,
    3,
    required.sort,
    1,
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now()) * 1000)::bigint
FROM required
WHERE NOT EXISTS (
    SELECT 1
      FROM system_menus existing
     WHERE existing.permission = required.permission
);

-- Built-in administrators retain the complete member administration surface.
-- Match stable role codes instead of deployment-specific numeric IDs.
INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT
    role.id,
    menu.id,
    (extract(epoch from now()) * 1000)::bigint
FROM system_roles role
CROSS JOIN system_menus menu
WHERE role.code IN ('super_admin', 'admin')
  AND (
      menu.id IN (3, 300, 301, 302, 303, 304, 305, 306, 307, 308)
      OR menu.permission IN (
          'member:user:query',
          'member:user:update',
          'member:address:query',
          'member:nickname:query',
          'member:nickname:create',
          'member:nickname:update',
          'member:nickname:delete',
          'member:tag:query',
          'member:tag:create',
          'member:tag:update',
          'member:tag:delete',
          'member:level:query',
          'member:level:create',
          'member:level:update',
          'member:level:delete',
          'member:group:query',
          'member:group:create',
          'member:group:update',
          'member:group:delete',
          'member:point:query',
          'member:config:query',
          'member:config:update',
          'member:signin:query',
          'member:signin:create',
          'member:signin:update',
          'member:signin:delete',
          'member:invite-code:query',
          'member:invite-code:create',
          'member:invite-code:update',
          'member:invite-code:delete'
      )
  )
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menus existing
       WHERE existing.role_id = role.id
         AND existing.menu_id = menu.id
  );

SELECT setval(
    pg_get_serial_sequence('system_menus', 'id'),
    (SELECT MAX(id) FROM system_menus)
);
