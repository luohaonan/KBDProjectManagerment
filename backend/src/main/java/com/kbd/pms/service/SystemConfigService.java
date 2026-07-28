package com.kbd.pms.service;

import com.kbd.pms.entity.SystemConfigEntity;
import com.kbd.pms.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemConfigService {

  private final SystemConfigRepository configRepository;

  public SystemConfigService(SystemConfigRepository configRepository) {
    this.configRepository = configRepository;
  }

  /** 获取单个配置值 */
  @Transactional(readOnly = true)
  public String getConfig(String key) {
    return configRepository.findById(key)
        .map(SystemConfigEntity::getConfigValue)
        .orElse(null);
  }

  /** 获取所有邮件配置（密码脱敏） */
  @Transactional(readOnly = true)
  public Map<String, String> getMailConfig() {
    Map<String, String> result = new LinkedHashMap<>();
    String[] keys = {
        "mail.smtp.host", "mail.smtp.port", "mail.smtp.username",
        "mail.smtp.password", "mail.smtp.ssl", "mail.from.address", "mail.enabled"
    };
    for (String key : keys) {
      String value = getConfig(key);
      if ("mail.smtp.password".equals(key) && value != null && !value.isEmpty()) {
        value = "****";
      }
      result.put(key, value != null ? value : "");
    }
    return result;
  }

  /** 批量保存邮件配置（密码为 "****" 时保留原值） */
  @Transactional
  public void saveMailConfig(Map<String, String> configs) {
    for (Map.Entry<String, String> entry : configs.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if ("mail.smtp.password".equals(key) && "****".equals(value)) {
        continue; // 密码未修改，保留原值
      }

      SystemConfigEntity entity = configRepository.findById(key).orElse(null);
      if (entity == null) {
        entity = new SystemConfigEntity();
        entity.setConfigKey(key);
      }
      entity.setConfigValue(value);
      entity.setUpdatedAt(Instant.now());
      configRepository.save(entity);
    }
  }

  /** 构建供 EmailService 使用的 JavaMail 属性 */
  public java.util.Properties buildMailProperties() {
    String host = getConfig("mail.smtp.host");
    String port = getConfig("mail.smtp.port");
    String ssl = getConfig("mail.smtp.ssl");

    java.util.Properties props = new java.util.Properties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    if (host != null) props.put("mail.smtp.host", host);
    if (port != null) props.put("mail.smtp.port", port);
    if ("true".equals(ssl)) {
      props.put("mail.smtp.ssl.enable", "true");
    }
    props.put("mail.smtp.connectiontimeout", "10000");
    props.put("mail.smtp.timeout", "10000");
    return props;
  }
}