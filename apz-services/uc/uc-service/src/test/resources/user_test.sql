INSERT INTO uc_user (id, created_at, created_by, updated_at, updated_by, deleted, username, nickname, first_name,
                     last_name, phone_number, phone_prefix, email, passwd, avatar, gender, status, referrer_id,
                     referrers, referrer_level, recommend_code, channel, ip, device, remark)
VALUES (2, 1218153600, '1', 1218153600, '1', false, 'gsvc', 'Administrator', 'Gsvc', null, null, null, null,
        '$2a$10$lda8JKIdmgV8mXLFZVTiVOgHaiQuRJXtyL55RbECrs0HtkHf4ZHy.', null, 'UNKNOWN', 'ACTIVATED', 0, null, 0, null,
        null, '127.0.0.1', 'pc', null);

INSERT INTO uc_oauth (id, created_at, created_by, updated_at, updated_by, deleted, uid, provider, open_id,
                      union_id, login_time, device, ip, last_login_time, last_device, last_ip, remark)
VALUES (2, 1218153600, '1', 1218153600, '1', false, 2, 'db', 'gsvc', 'gsvc', 1218153600, 'pc', '127.0.0.1', null,
        null, null, null);

INSERT INTO uc_tenant (id, created_at, created_by, updated_at, updated_by, deleted, name, status, expire_at, authed_at)
VALUES (1, 1218153600, '1', 1218153600, '1', false, 'Test', 'ACTIVATED', 0, 0);

INSERT INTO uc_tenant_user (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, sa)
VALUES (1, 1218153600, '1', 1218153600, '1', false, 1, 2, true);

INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (10, 0, '0', 0, '0', 0, false, 'S', 2, 'test', 'test string', null);
INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (20, 0, '0', 0, '0', 0, false, 'I', 2, 'int', '1', null);

INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (21, 0, '0', 0, '0', 1, false, 'I', 2, 'int', '2', null);

INSERT INTO uc_role (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role, name, builtin,
                     provider)
VALUES (10, 1218153600, '1', 1218153600, '1', 1, false, 'user', 'Authenticated User', true, 'db');

INSERT INTO uc_role_privilege (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role_id,
                               privilege_id)
VALUES (10, 1218153600, '1', 1218153600, '1', 1, false, 10, 1);

INSERT INTO uc_user_role (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, role_id)
VALUES (30, 1218153600, '1', 1218153600, '1', false, 0, 2, 2);

INSERT INTO uc_user_role (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, role_id)
VALUES (31, 1218153600, '1', 1218153600, '1', false, 1, 2, 10);
