INSERT INTO uc_tenant (id, created_at, created_by, updated_at, updated_by, deleted, name, status, expire_at, authed_at)
VALUES (2, 1218153600, '1', 1218153600, '1', false, 'Test2', 'ACTIVATED', 0, 0);

INSERT INTO uc_tenant_user (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, sa)
VALUES (2, 1218153600, '1', 1218153600, '1', false, 2, 2, true);
-- organization
INSERT INTO uc_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, icon, remark)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 'O1', null, null);
INSERT INTO uc_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, icon, remark)
VALUES (2, 1218153600, null, 1218153600, null, false, 1, 'O2', null, null);
INSERT INTO uc_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, icon, remark)
VALUES (3, 1218153600, null, 1218153600, null, false, 2, 'O3', null, null);

-- department
INSERT INTO uc_department (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, org_id, icon,
                           remark)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 'D1-1', 1, null, null);
INSERT INTO uc_department (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, org_id, icon,
                           remark)
VALUES (2, 1218153600, null, 1218153600, null, false, 1, 'D1-2', 2, null, null);
INSERT INTO uc_department (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, name, org_id, icon,
                           remark)
VALUES (3, 1218153600, null, 1218153600, null, false, 2, 'D2-1', 3, null, null);

-- job_level
INSERT INTO uc_job_level (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, org_id, name, level,
                          icon, remark)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 1, 'P1', 1, null, null);
INSERT INTO uc_job_level (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, org_id, name, level,
                          icon, remark)
VALUES (2, 1218153600, null, 1218153600, null, false, 2, 3, 'P3', 1, null, null);

-- job
INSERT INTO uc_job (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, org_id, depart_id, name,
                    level_id, icon, remark)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 1, 1, '软件开发工程师', 1, null, null);
INSERT INTO uc_job (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, org_id, depart_id, name,
                    level_id, icon, remark)
VALUES (2, 1218153600, null, 1218153600, null, false, 2, 3, 3, 'HR', 2, null, null);

-- user_organization
INSERT INTO uc_user_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, org_id)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 2, 1);
INSERT INTO uc_user_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, org_id)
VALUES (2, 1218153600, null, 1218153600, null, false, 1, 2, 2);
INSERT INTO uc_user_organization (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, org_id)
VALUES (3, 1218153600, null, 1218153600, null, false, 2, 2, 3);
-- user_department
INSERT INTO uc_user_department (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, depart_id)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 2, 1);
INSERT INTO uc_user_department (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, depart_id)
VALUES (2, 1218153600, null, 1218153600, null, false, 2, 2, 3);
-- user_job
INSERT INTO uc_user_job (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, depart_id, job_id)
VALUES (1, 1218153600, null, 1218153600, null, false, 1, 2, 1, 1);
INSERT INTO uc_user_job (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, depart_id, job_id)
VALUES (2, 1218153600, null, 1218153600, null, false, 2, 2, 3, 2);

INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (1000, 1218153600, null, 1218153600, null, 0, false, 'S', 2, 'CUR_TENANT_ID', '1', null);
INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (1001, 1218153600, null, 1218153600, null, 0, false, 'S', 2, 'CUR_ORG_ID', '1', null);
INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (1002, 1218153600, null, 1218153600, null, 0, false, 'S', 2, 'CUR_DEPT_ID', '1', null);
INSERT INTO uc_user_meta (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, type, uid, name,
                          value, remark)
VALUES (1003, 1218153600, null, 1218153600, null, 0, false, 'S', 2, 'CUR_JOB_ID', '1', null);
