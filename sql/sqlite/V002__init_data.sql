-- =============================================
-- module-member 初始化数据 (SQLite)
-- =============================================

-- =============================================
-- 菜单数据
-- type: 1=目录, 2=菜单, 3=按钮
-- status: 0=正常, 1=停用
-- =============================================

-- 会员中心（一级目录）
INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (3, '会员中心', '', 1, 0, '/member', NULL, 'ant-design:user-outlined', 3, 0, 0, 0);

-- =====================
-- 会员中心 (parent_id=3) - 二级菜单
-- =====================
INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (300, '会员列表', 'member:user:list', 2, 3, 'user', 'member/user/index', 'ant-design:user-outlined', 1, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (301, '会员标签', 'member:tag:list', 2, 3, 'tag', 'member/tag/index', 'ant-design:tag-outlined', 2, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (302, '会员等级', 'member:level:list', 2, 3, 'level', 'member/level/index', 'ant-design:trophy-outlined', 3, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (303, '会员分组', 'member:group:list', 2, 3, 'group', 'member/group/index', 'ant-design:team-outlined', 4, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (304, '积分记录', 'member:point:list', 2, 3, 'point/record', 'member/point/record/index', 'ant-design:star-outlined', 5, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (305, '签到配置', 'member:signin-config:list', 2, 3, 'signin/config', 'member/signin/config/index', 'ant-design:calendar-outlined', 6, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (306, '签到记录', 'member:signin-record:list', 2, 3, 'signin/record', 'member/signin/record/index', 'ant-design:check-circle-outlined', 7, 0, 0, 0);

INSERT OR IGNORE INTO system_menus (id, name, permission, type, parent_id, path, component, icon, sort, status, created_at, updated_at)
VALUES (307, '会员配置', 'member:config:list', 2, 3, 'config', 'member/config/index', 'ant-design:setting-outlined', 8, 0, 0, 0);
