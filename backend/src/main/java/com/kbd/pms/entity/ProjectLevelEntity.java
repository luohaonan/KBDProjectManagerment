package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_level")
public class ProjectLevelEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "level_code", nullable = false, length = 8)
  private String levelCode;

  @Column(name = "level_name", nullable = false, length = 64)
  private String levelName;

  @Lob
  @Column(name = "definition_text", nullable = false)
  private String definitionText;

  @Lob
  @Column(name = "governance_text")
  private String governanceText;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = Boolean.TRUE;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ProjectLevelEntity() {}

  public Long getId() { return id; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getDefinitionText() { return definitionText; }
  public void setDefinitionText(String definitionText) { this.definitionText = definitionText; }
  public String getGovernanceText() { return governanceText; }
  public void setGovernanceText(String governanceText) { this.governanceText = governanceText; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

