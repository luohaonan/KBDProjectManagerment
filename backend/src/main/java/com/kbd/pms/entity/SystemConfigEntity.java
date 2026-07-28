package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "system_config")
public class SystemConfigEntity {
  @Id
  @Column(name = "config_key", length = 64)
  private String configKey;

  @Column(name = "config_value", columnDefinition = "TEXT")
  private String configValue;

  @Column(name = "description", length = 256)
  private String description;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SystemConfigEntity() {}

  public String getConfigKey() { return configKey; }
  public void setConfigKey(String configKey) { this.configKey = configKey; }
  public String getConfigValue() { return configValue; }
  public void setConfigValue(String configValue) { this.configValue = configValue; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}