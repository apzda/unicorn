CREATE TABLE `apzda_base_setting`
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at  BIGINT UNSIGNED NULL     DEFAULT NULL,
    created_by  VARCHAR(32),
    updated_at  BIGINT UNSIGNED NULL     DEFAULT NULL,
    updated_by  VARCHAR(32),
    deleted     BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    tenant_id   VARCHAR(32) COMMENT 'the id of the tenant to which this setting belongs',
    version     smallint        NOT NULL COMMENT 'version no.',
    setting_key CHAR(32)        NOT NULL COMMENT 'The MD5 value of Setting Key',
    setting_cls VARCHAR(512)    NOT NULL COMMENT 'Setting class name',
    setting     LONGTEXT COMMENT 'Base64 encoded the JSON value of a Setting',
    UNIQUE INDEX `udx_setting_key` (setting_key)
) ENGINE = InnoDB COMMENT 'Settings';

CREATE TABLE `apzda_base_setting_revision`
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at  BIGINT UNSIGNED NULL     DEFAULT NULL,
    created_by  VARCHAR(32),
    updated_at  BIGINT UNSIGNED NULL     DEFAULT NULL,
    updated_by  VARCHAR(32),
    deleted     BIT             NOT NULL DEFAULT FALSE COMMENT 'Soft Deleted Flag',
    tenant_id   VARCHAR(32) COMMENT 'the id of the tenant to which this setting belongs',
    revision    INT UNSIGNED    NOT NULL COMMENT 'Revision Number',
    setting_key CHAR(32)        NOT NULL COMMENT 'The MD5 value of Setting Key',
    setting_cls VARCHAR(512)    NOT NULL COMMENT 'Setting class name',
    setting     LONGTEXT COMMENT 'Base64 encoded the JSON value of a Setting',
    UNIQUE INDEX `udx_setting_key` (setting_key, revision)
) ENGINE = InnoDB COMMENT 'Setting Revisions';
