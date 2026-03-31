USE auth;

-- 认证中心不再维护权限模型，删除历史鉴权表。
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS account_role;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
