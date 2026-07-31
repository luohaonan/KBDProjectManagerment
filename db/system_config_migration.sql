-- 系统配置表（邮件配置等）
CREATE TABLE IF NOT EXISTS system_config (
  config_key VARCHAR(64) PRIMARY KEY COMMENT '配置键',
  config_value TEXT COMMENT '配置值',
  description VARCHAR(256) COMMENT '配置说明',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 邮件配置初始数据
INSERT INTO system_config (config_key, config_value, description) VALUES
('mail.smtp.host', 'smtp.qiye.163.com', 'SMTP 服务器地址'),
('mail.smtp.port', '465', 'SMTP 端口'),
('mail.smtp.username', '', '发信邮箱账号'),
('mail.smtp.password', '', '发信邮箱密码/授权码'),
('mail.smtp.ssl', 'true', '是否启用 SSL'),
('mail.from.address', '', '发信人地址（通常与 username 相同）'),
('mail.enabled', 'false', '是否启用邮件发送'),
('app.internal.url', 'http://192.168.39.233:18080/', '内网系统访问地址，用于邮件通知中的登录链接'),
('app.external.url', 'http://343t787f48.wicp.vip/', '外网系统访问地址，用于邮件通知中的登录链接'),
('app.base.url', 'http://343t787f48.wicp.vip/', '旧版系统访问地址兼容配置')
ON DUPLICATE KEY UPDATE config_key = config_key;