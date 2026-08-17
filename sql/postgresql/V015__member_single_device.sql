-- 单设备登录：记录会员当前占用的设备
--
-- 换设备登录时与这一列比对，不同则自增 session_version 顶掉上一台。
-- 只记「当前这一台」而不是设备列表：需求是同时只允许一台，多留历史反而要额外定义
-- 「哪台才算当前」，而那正是这一列本身。要做登录历史应另建审计表，与鉴权解耦。
ALTER TABLE member_users
  ADD COLUMN current_device_id VARCHAR(64) NULL;
