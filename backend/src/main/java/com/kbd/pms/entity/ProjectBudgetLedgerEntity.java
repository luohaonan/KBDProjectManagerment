package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project_budget_ledger")
public class ProjectBudgetLedgerEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "occurred_on", nullable = false)
  private LocalDate occurredOn;

  @Enumerated(EnumType.STRING)
  @Column(name = "expense_category", nullable = false, length = 16)
  private Enums.ExpenseCategory expenseCategory;

  @Column(name = "amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "vendor_name", length = 256)
  private String vendorName;

  @Column(name = "reference_no", length = 64)
  private String referenceNo;

  @Column(name = "description", length = 512)
  private String description;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public ProjectBudgetLedgerEntity() {}

  public Long getId() { return id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public LocalDate getOccurredOn() { return occurredOn; }
  public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
  public Enums.ExpenseCategory getExpenseCategory() { return expenseCategory; }
  public void setExpenseCategory(Enums.ExpenseCategory expenseCategory) { this.expenseCategory = expenseCategory; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getVendorName() { return vendorName; }
  public void setVendorName(String vendorName) { this.vendorName = vendorName; }
  public String getReferenceNo() { return referenceNo; }
  public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

