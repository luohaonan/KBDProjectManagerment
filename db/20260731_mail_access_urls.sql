-- Mail notification access URLs for existing installations.
-- The INSERT statements keep administrator-customized values when the keys already exist.

INSERT INTO system_config (config_key, config_value, description)
VALUES ('app.internal.url', 'http://192.168.39.233:18080/', '内网系统访问地址，用于邮件通知中的登录链接')
ON DUPLICATE KEY UPDATE config_key = config_key;

INSERT INTO system_config (config_key, config_value, description)
VALUES ('app.external.url', 'http://343t787f48.wicp.vip/', '外网系统访问地址，用于邮件通知中的登录链接')
ON DUPLICATE KEY UPDATE config_key = config_key;

-- Upgrade the old fallback only when it still points to a development address.
UPDATE system_config
SET config_value = 'http://343t787f48.wicp.vip/',
    description = '旧版系统访问地址兼容配置'
WHERE config_key = 'app.base.url'
  AND (config_value IS NULL
    OR TRIM(config_value) = ''
    OR config_value LIKE 'http://localhost:%'
    OR config_value LIKE 'http://127.0.0.1:%');