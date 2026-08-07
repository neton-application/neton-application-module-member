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
