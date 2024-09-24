CREATE TABLE `uc_user`
(
    `id`             BIGINT UNSIGNED  NOT NULL PRIMARY KEY,
    `created_at`     BIGINT UNSIGNED  NULL                  DEFAULT NULL,
    `created_by`     VARCHAR(32)      NULL COMMENT 'Create User Id',
    `updated_at`     BIGINT UNSIGNED  NULL                  DEFAULT NULL,
    `updated_by`     VARCHAR(32)      NULL COMMENT 'Last updated by who',
    `deleted`        BIT              NOT NULL              DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `username`       VARCHAR(32)      NOT NULL COMMENT 'User Name',
    `nickname`       VARCHAR(64)      NULL                  DEFAULT NULL COMMENT 'nickname',
    `first_name`     VARCHAR(128)     NULL                  DEFAULT NULL COMMENT 'first name',
    `last_name`      VARCHAR(128)     NULL                  DEFAULT NULL COMMENT 'last name',
    `phone_number`   VARCHAR(20)      NULL                  DEFAULT NULL COMMENT 'phone number',
    `phone_prefix`   VARCHAR(10)      NULL                  DEFAULT NULL COMMENT 'international prefix',
    `email`          VARCHAR(256)     NULL                  DEFAULT NULL COMMENT 'email',
    `passwd`         VARCHAR(512)     NOT NULL              DEFAULT '' COMMENT 'password',
    `avatar`         VARCHAR(1024)    NULL                  DEFAULT NULL COMMENT 'avatar',
    `gender`         ENUM ('MALE','FEMALE','MIX','UNKNOWN') DEFAULT 'UNKNOWN' COMMENT 'gender',
    `status`         ENUM ('PENDING','ACTIVATED',
        'LOCKED','DISABLED','EXPIRED')                      DEFAULT 'PENDING' COMMENT 'status of user',
    `referrer_id`    BIGINT UNSIGNED  NOT NULL              DEFAULT 0 COMMENT 'the id of Referrer',
    `referrers`      VARCHAR(256)     NULL                  DEFAULT NULL COMMENT 'the chain of referrer id seperated by /,max 20',
    `referrer_level` TINYINT UNSIGNED NOT NULL              DEFAULT '0' COMMENT 'the referrer level',
    `recommend_code` VARCHAR(32)      NULL                  DEFAULT NULL COMMENT 'the recommend code of this user',
    `channel`        VARCHAR(16)      NULL                  DEFAULT NULL COMMENT 'the channel from which the user come',
    `ip`             VARCHAR(256)     NOT NULL COMMENT 'the ip address(v4 or v6) from which the user come',
    `device`         VARCHAR(24)      NULL                  DEFAULT NULL COMMENT 'the device on which the user come',
    `remark`         VARCHAR(255)     NULL                  DEFAULT NULL COMMENT 'remark',
    UNIQUE KEY `UDX_USERNAME` (`username`),
    INDEX `IDX_CREATE_AT` (created_at ASC),
    INDEX `IDX_PARENT` (`referrer_id` ASC),
    INDEX `IDX_PHONE` (`phone_number`),
    INDEX `IDX_CHANNEL` (`channel`),
    INDEX `IDX_REFERRERS` (`referrers`),
    INDEX `IDX_RECOMMEND_CODE` (`recommend_code`)
) COMMENT ='Users';

CREATE TABLE `uc_user_meta`
(
    `id`         BIGINT UNSIGNED NOT NULL COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL       DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL       DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL   DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `type`       ENUM ('S','I','L','D','F') DEFAULT 'S' COMMENT 'value type: S-string;I-int;L-long;D-double;F-float',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `name`       VARCHAR(32)     NOT NULL COMMENT 'meta name',
    `value`      LONGTEXT                   DEFAULT null COMMENT 'value',
    `remark`     TEXT                       DEFAULT null COMMENT 'remark',
    PRIMARY KEY (`id`),
    UNIQUE KEY `UDX_USER_META` (`uid`, `name`)
) COMMENT ='User meta';

CREATE TABLE `uc_oauth`
(
    `id`              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at`      BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by`      VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at`      BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by`      VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`         BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `uid`             BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'User ID',
    `provider`        VARCHAR(24)     NOT NULL COMMENT 'OpenID Provider',
    `open_id`         VARCHAR(256)    NOT NULL COMMENT 'OpenID',
    `union_id`        VARCHAR(256)    NOT NULL COMMENT 'UnionID',
    `login_time`      BIGINT UNSIGNED COMMENT 'first login time',
    `device`          VARCHAR(24) COMMENT 'first login device',
    `ip`              VARCHAR(256) COMMENT 'the ip address(v4 or v6) from which the user login',
    `last_login_time` BIGINT UNSIGNED COMMENT 'last login time',
    `last_device`     VARCHAR(24) COMMENT 'last login device',
    `last_ip`         VARCHAR(256) COMMENT 'the last ip address(v4 or v6) from which the user login',
    `remark`          VARCHAR(255)    NULL     DEFAULT NULL COMMENT 'remark',
    UNIQUE KEY `UDX_TYPE_ID` (`provider`, `union_id`),
    INDEX `FK_USER_ID` (`uid` asc)
) COMMENT ='Oauth2.0 grants';

CREATE TABLE `uc_oauth_meta`
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `oauth_id`   BIGINT UNSIGNED NOT NULL,
    `name`       VARCHAR(32)     NOT NULL COMMENT 'name',
    `value`      TEXT COMMENT 'value',
    `remark`     VARCHAR(255)    NULL     DEFAULT NULL COMMENT 'remark',
    UNIQUE KEY `UDX_ID_NAME` (`oauth_id`, `name`)
) COMMENT ='oauth meta';

CREATE TABLE `uc_oauth_session`
(
    `id`            BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at`    BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by`    VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at`    BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by`    VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`       BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `oauth_id`      BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'oauth id',
    `uid`           BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'user id',
    `grant_code`    VARCHAR(256)    NULL COMMENT 'grant code',
    `access_token`  VARCHAR(256)    NULL COMMENT 'access token',
    `refresh_token` VARCHAR(256)    NULL COMMENT 'refresh token',
    `expiration`    BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'expire time',
    `device`        VARCHAR(24)     NOT NULL COMMENT 'the device from which the user login',
    `simulator`     BIT             NOT NULL DEFAULT FALSE COMMENT 'if the device is a simulator',
    `ip`            VARCHAR(256)    NOT NULL COMMENT 'the ip from which the user login',
    `extra`         LONGTEXT        NULL     DEFAULT NULL COMMENT 'extra data(prefer json format)',
    UNIQUE KEY (`oauth_id`),
    KEY `IDX_CTIME` (`created_at`),
    KEY `IDX_EXPIRE` (`expiration`),
    KEY `IDX_TOKEN` (`access_token`),
    KEY `IDX_UID` (`uid`)
) COMMENT ='oauth login sessions';

CREATE TABLE `uc_user_security_qa`
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `uid`        BIGINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'user id',
    `qid`        VARCHAR(32)     NOT NULL COMMENT 'the id of question',
    `answer`     VARCHAR(255)    NOT NULL COMMENT 'user answer of the question',
    unique key UDX_UID_QID (`uid`, `qid`)
) comment = 'user security question and answer';

CREATE TABLE `uc_user_mfa`
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `uid`        bigint unsigned NOT NULL COMMENT 'user id',
    `auth_type`  varchar(128)    NOT NULL COMMENT 'auth type',
    `phone`      varchar(64)              DEFAULT NULL COMMENT 'phone number',
    `email`      varchar(256)             DEFAULT NULL COMMENT 'email address',
    `secret_key` varchar(256)             DEFAULT NULL COMMENT 'secret key of mfa',
    UNIQUE KEY `uniq_uid_auth_type` (`uid`, `auth_type`)
) comment = 'User multiple factor authenticate configuration';

CREATE TABLE uc_tenant
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `name`       VARCHAR(64)     NOT NULL COMMENT 'name',
    `status`     ENUM ('NEW','PENDING','DECLINED',
        'REJECTED','ACTIVATED','LOCKED',
        'DISABLED','EXPIRED')             DEFAULT 'NEW' COMMENT 'status of tenant',
    `expire_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'expire date timestamp',
    `authed_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'authenticated date timestamp',
    INDEX IDX_STATUS (`status`),
    INDEX IDX_EXPIRE_AT (`expire_at` asc)
) COMMENT = 'tenants';

CREATE TABLE `uc_tenant_meta`
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL       DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL       DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL   DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `type`       ENUM ('S','I','L','D','F') DEFAULT 'S' COMMENT 'value type: S-string;I-int;L-long;D-double;F-float',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `name`       VARCHAR(32)     NOT NULL COMMENT 'meta name',
    `value`      LONGTEXT                   DEFAULT null COMMENT 'value',
    `remark`     TEXT                       DEFAULT null COMMENT 'remark',
    UNIQUE KEY `UDX_USER_META` (`tenant_id`, `name`)
) COMMENT ='User meta data';

CREATE TABLE `uc_tenant_user`
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT 'tenant id',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `sa`         BIT             NOT NULL DEFAULT FALSE COMMENT 'the super admin of this tenant',
    UNIQUE KEY `UDX_USER_ID` (`uid` asc, `tenant_id` ASC),
    INDEX `UDX_TENANT_ID` (`tenant_id` asc)
) COMMENT = 'the users of tenant';

CREATE TABLE uc_organization
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT 'tenant id',
    `name`       VARCHAR(64)     NOT NULL COMMENT 'name',
    `icon`       VARCHAR(128)    NULL COMMENT 'icon',
    `remark`     TEXT                     DEFAULT null COMMENT 'remark',
    INDEX `UDX_TENANT_ID` (`tenant_id` asc)
) COMMENT = 'organization';

CREATE TABLE uc_department
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT 'tenant id',
    `name`       VARCHAR(64)     NOT NULL COMMENT 'name',
    `org_id`     BIGINT UNSIGNED NOT NULL COMMENT 'organization id',
    `icon`       VARCHAR(128)    NULL COMMENT 'icon',
    `remark`     TEXT                     DEFAULT null COMMENT 'remark',
    INDEX `UDX_TENANT_ID` (`tenant_id` asc),
    INDEX `UDX_ORG_ID` (`org_id` asc)
) COMMENT = 'department';

CREATE TABLE uc_job_level
(
    `id`         BIGINT UNSIGNED  NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED  NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)      NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED  NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)      NULL COMMENT 'Last updated by who',
    `deleted`    BIT              NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED  NOT NULL COMMENT 'tenant id',
    `org_id`     BIGINT UNSIGNED  NOT NULL COMMENT 'organization id',
    `name`       VARCHAR(64)      NOT NULL COMMENT 'name',
    `level`      INTEGER UNSIGNED NOT NULL COMMENT 'job level',
    `icon`       VARCHAR(128)     NULL COMMENT 'icon',
    `remark`     TEXT                      DEFAULT null COMMENT 'remark',
    INDEX `UDX_TENANT_ID` (`tenant_id` asc),
    INDEX `UDX_ORG_ID` (`org_id` asc)
) COMMENT = 'job level';

CREATE TABLE uc_job
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT 'tenant id',
    `org_id`     BIGINT UNSIGNED NOT NULL COMMENT 'organization id',
    `depart_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'department id',
    `name`       VARCHAR(64)     NOT NULL COMMENT 'job name',
    `level_id`   BIGINT UNSIGNED NOT NULL COMMENT 'job level',
    `icon`       VARCHAR(128)    NULL COMMENT 'icon',
    `remark`     TEXT                     DEFAULT null COMMENT 'remark',
    INDEX `UDX_TENANT_ID` (`tenant_id` asc),
    INDEX `UDX_ORG_ID` (`org_id` asc),
    INDEX `UDX_DEPART_ID` (`depart_id` asc),
    INDEX `UDX_LEVEL_ID` (`level_id` asc)
) COMMENT = 'job';

CREATE TABLE uc_user_organization
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `org_id`     BIGINT UNSIGNED NOT NULL COMMENT 'organization id',
    INDEX UDX_ROLE (`org_id`),
    INDEX UDX_UID (`uid`),
    INDEX IDX_TENANT_UID (`tenant_id`, `uid`),
    INDEX IDX_TENANT_ROLE (`tenant_id`, `org_id`)
) COMMENT = 'user roles';

CREATE TABLE uc_user_department
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `depart_id`  BIGINT UNSIGNED NOT NULL COMMENT 'department id',
    INDEX UDX_ROLE (`depart_id`),
    INDEX UDX_UID (`uid`),
    INDEX IDX_TENANT_UID (`tenant_id`, `uid`),
    INDEX IDX_TENANT_ROLE (`tenant_id`, `depart_id`)
) COMMENT = 'user department';

CREATE TABLE uc_user_job
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `depart_id`  BIGINT UNSIGNED NOT NULL COMMENT 'department id',
    `job_id`     BIGINT UNSIGNED NOT NULL COMMENT 'job id',
    INDEX UDX_ROLE (`depart_id`),
    INDEX UDX_UID (`uid`),
    INDEX IDX_TENANT_UID (`tenant_id`, `uid`),
    INDEX IDX_TENANT_ROLE (`tenant_id`, `depart_id`),
    INDEX IDX_TENANT_JOB (`tenant_id`, `job_id`)
) COMMENT = 'user job';

CREATE TABLE uc_role
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `role`       VARCHAR(32)     NOT NULL COMMENT 'role',
    `name`       VARCHAR(128)    NOT NULL COMMENT 'role name',
    `builtin`    BIT             NOT NULL DEFAULT FALSE COMMENT 'builtin role, cannot be deleted',
    `provider`   VARCHAR(24)     NOT NULL COMMENT 'provider(db, ldap or ad)',
    UNIQUE KEY UDX_ROLE (`role`, `tenant_id`),
    INDEX IDX_TENANT_ID (`tenant_id`)
) COMMENT = 'roles';

CREATE TABLE uc_role_children
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `role_id`    BIGINT UNSIGNED NOT NULL COMMENT 'Role Id',
    `child_id`   BIGINT UNSIGNED NOT NULL COMMENT 'Child Role Id',
    INDEX IDX_ROLE (`role_id`),
    INDEX IDX_CHILD (`child_id`)
) COMMENT = 'role children';

CREATE TABLE uc_user_role
(
    `id`         BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by` VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at` BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by` VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`    BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `tenant_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `uid`        BIGINT UNSIGNED NOT NULL COMMENT 'user id',
    `role_id`    BIGINT UNSIGNED NOT NULL COMMENT 'role id',
    INDEX UDX_ROLE (`role_id`),
    INDEX UDX_UID (`uid`),
    INDEX IDX_TENANT_UID (`tenant_id`, `uid`),
    INDEX IDX_TENANT_ROLE (`tenant_id`, `role_id`)
) COMMENT = 'user roles';

CREATE TABLE uc_privilege
(
    `id`          BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at`  BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by`  VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at`  BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by`  VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `deleted`     BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `name`        VARCHAR(256)    NOT NULL COMMENT 'the name of this privilege',
    `type`        VARCHAR(24)     NOT NULL COMMENT 'the type of this privilege',
    `builtin`     BIT             NOT NULL DEFAULT FALSE COMMENT 'builtin privilege, cannot be deleted',
    `permission`  VARCHAR(128)    NOT NULL COMMENT 'the permission of this privilege',
    `extra`       TEXT            NULL COMMENT 'extra data of this privilege',
    `description` TEXT            NULL COMMENT 'the description',
    `remark`      TEXT            NULL COMMENT 'the remark',
    INDEX IDX_TENANT_ID (`tenant_id`)
) COMMENT = 'privileges';

CREATE TABLE uc_role_privilege
(
    `id`           BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'id',
    `created_at`   BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by`   VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at`   BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by`   VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `tenant_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'tenant id',
    `deleted`      BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `role_id`      BIGINT UNSIGNED NOT NULL COMMENT 'ROLE ID',
    `privilege_id` BIGINT UNSIGNED NOT NULL COMMENT 'privilege id',
    INDEX IDX_PRIVILEGE_ID (`privilege_id`),
    INDEX IDX_TENANT_ID (`tenant_id`)
) COMMENT = 'role privileges';

-- username: admin
INSERT INTO uc_user (id, created_at, created_by, updated_at, updated_by, deleted, username, nickname, first_name,
                     last_name, phone_number, phone_prefix, email, passwd, avatar, gender, status, referrer_id,
                     referrers, referrer_level, recommend_code, channel, ip, device, remark)
VALUES (1, 1218153600, '1', 1218153600, '1', false, 'admin', 'Administrator', 'Admin', null, null, null, null,
        '$2a$10$lda8JKIdmgV8mXLFZVTiVOgHaiQuRJXtyL55RbECrs0HtkHf4ZHy.', null, 'UNKNOWN', 'ACTIVATED', 0, null, 0, null,
        null, '127.0.0.1', 'pc', null);

-- oauth
INSERT INTO uc_oauth (id, created_at, created_by, updated_at, updated_by, deleted, uid, provider, open_id,
                      union_id, login_time, device, ip, last_login_time, last_device, last_ip, remark)
VALUES (1, 1218153600, '1', 1218153600, '1', false, 1, 'db', 'admin', 'admin', 1218153600, 'pc', '127.0.0.1', null,
        null, null, null);

-- roles
INSERT INTO uc_role (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role, name, builtin,
                     provider)
VALUES (1, 1218153600, '1', 1218153600, '1', 0, false, 'sa', 'Super Administrator', true, 'db');
INSERT INTO uc_role (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role, name, builtin,
                     provider)
VALUES (2, 1218153600, '1', 1218153600, '1', 0, false, 'admin', 'Administrator', true, 'db');
INSERT INTO uc_role (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role, name, builtin,
                     provider)
VALUES (3, 1218153600, '1', 1218153600, '1', 0, false, 'user', 'Authenticated User', true, 'db');

-- privileges
INSERT INTO uc_privilege (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, name, type, builtin,
                          permission, extra, description, remark)
VALUES (1, 1218153600, '1', 11218153600, '1', 0, false, 'All Privileges', 'core', true, '*:*', null, null, null);

-- privileges of role
INSERT INTO uc_role_privilege (id, created_at, created_by, updated_at, updated_by, tenant_id, deleted, role_id,
                               privilege_id)
VALUES (1, 1218153600, '1', 1218153600, '1', 0, false, 1, 1);

-- role of user
INSERT INTO uc_user_role (id, created_at, created_by, updated_at, updated_by, deleted, tenant_id, uid, role_id)
VALUES (1, 1218153600, '1', 1218153600, '1', false, 0, 1, 1);


alter table uc_oauth_session
    drop key oauth_id;

create index idx_oauth_id on uc_oauth_session (oauth_id);

alter table uc_user_meta
    add tenant_id bigint unsigned null comment 'Tenant Id' after updated_by;

alter table uc_user_meta
    drop key UDX_USER_META,
    add constraint UDX_USER_META unique (tenant_id, uid, name);

CREATE TABLE `uc_security_resource`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'id',
    `created_at`  BIGINT UNSIGNED NULL     DEFAULT NULL,
    `created_by`  VARCHAR(32)     NULL COMMENT 'Create User Id',
    `updated_at`  BIGINT UNSIGNED NULL     DEFAULT NULL,
    `updated_by`  VARCHAR(32)     NULL COMMENT 'Last updated by who',
    `deleted`     BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    `pid`         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'the parent id of this resource',
    `rid`         VARCHAR(32)     NOT NULL COMMENT 'the resource id',
    `name`        VARCHAR(64)     NOT NULL COMMENT 'the resource name',
    `actions`     VARCHAR(512)    NOT NULL DEFAULT 'c,r,u,d' COMMENT 'actions can be performed on resource',
    `explorer`    VARCHAR(256)             DEFAULT NULL COMMENT 'a FQDN class name for exploring the ids of this resource ',
    `description` VARCHAR(1024)            DEFAULT NULL COMMENT 'the description of this resource',
    PRIMARY KEY (`id`),
    UNIQUE KEY `UDX_NAME` (`pid`, `name`)
) COMMENT ='Security Resource';

alter table uc_tenant
    add domain varchar(64) null comment 'the domain of this tenant' after name;

alter table uc_role
    add description TEXT null comment 'Description' after provider;

drop index IDX_PHONE on uc_user;

create unique index UDX_EMAIL on uc_user (email);

create unique index UDX_PHONE on uc_user (phone_prefix, phone_number);

alter table uc_user_mfa
    add enabled boolean default false null comment 'enabled or not';

DROP INDEX UDX_EMAIL ON uc_user;
CREATE INDEX IDX_EMAIL ON uc_user (email);

DROP INDEX UDX_PHONE ON uc_user;
CREATE INDEX IDX_PHONE ON uc_user (phone_number, phone_prefix);

ALTER TABLE `uc_oauth`
    ADD `recommend_code` VARCHAR(32) NULL DEFAULT NULL COMMENT 'recommend code' AFTER `union_id`;
