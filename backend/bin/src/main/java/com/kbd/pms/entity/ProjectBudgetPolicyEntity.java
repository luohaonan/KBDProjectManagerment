package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "project_budget_policy")
public class ProjectBudgetPolicyEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "yellow_threshold", nullable = false, precision = 6, scale = 4)
  private BigDecimal yellowThreshold;

  @Column(name = "red_threshold", nullable = false, precision = 6, scale = 4)
  private BigDecimal redThreshold;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode = "CNY";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectBudgetPolicyEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public BigDecimal getYellowThreshold() { return yellowThreshold; }
  public void setYellowThreshold(BigDecimal yellowThreshold) { this.yellowThreshold = yellowThreshold; }
  public BigDecimal getRedThreshold() { return redThreshold; }
  public void setRedThreshold(BigDecimal redThreshold) { this.redThreshold = redThreshold; }
  public String getCurrencyCode() { return currencyCode; }
  public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

