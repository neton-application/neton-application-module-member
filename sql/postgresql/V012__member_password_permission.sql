-- Admin capability: reset a member's login password.
--
-- Deliberately a permission of its own instead of reusing `member:user:update`:
-- resetting a password means taking over the account, so it must be grantable
-- (and revocable) separately from ordinary profile edits.

WITH required(parent_id, name, permission, sort) AS (
    VALUES
        (300::bigint, '重设会员密码', 'member:user:update-password', 7)
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

-- Built-in administrators get it by default (same treatment as the other
-- member mutations in V008). Match stable role codes, not numeric ids.
INSERT INTO system_role_menus (role_id, menu_id, created_at)
SELECT
    role.id,
    menu.id,
    (extract(epoch from now()) * 1000)::bigint
FROM system_roles role
CROSS JOIN system_menus menu
WHERE role.code IN ('super_admin', 'admin')
  AND menu.permission = 'member:user:update-password'
  AND NOT EXISTS (
      SELECT 1
        FROM system_role_menus existing
       WHERE existing.role_id = role.id
         AND existing.menu_id = menu.id
  );
