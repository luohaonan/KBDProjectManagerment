package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project")
public class ProjectEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_no", nullable = false, length = 16)
  private String projectNo;

  @Column(name = "level_id", nullable = false)
  private Long levelId;

  @Column(name = "project_code", nullable = false, length = 32)
  private String projectCode;

  @Column(name = "project_name", nullable = false, length = 256)
  private String projectName;

  @Column(name = "target_pathway", length = 256)
  private String targetPathway;

  @Column(name = "indication", length = 256)
  private String indication;

  @Lob
  @Column(name = "tpp_summary")
  private String tppSummary;

  @Lob
  @Column(name = "description")
  private String description;

  @Lob
  @Column(name = "mechanism")
  private String mechanism;

  @Column(name = "unmet_needs", length = 512)
  private String unmetNeeds;

  @Lob
  @Column(name = "scientific_basis")
  private String scientificBasis;

  @Column(name = "expected_indication", length = 256)
  private String expectedIndication;

  @Column(name = "administration_route", length = 64)
  private String administrationRoute;

  @Column(name = "dosage_form", length = 64)
  private String dosageForm;

  @Column(name = "dosage_frequency", length = 64)
  private String dosageFrequency;

  @Lob
  @Column(name = "efficacy_target")
  private String efficacyTarget;

  @Lob
  @Column(name = "safety_advantage")
  private String safetyAdvantage;

  @Lob
  @Column(name = "differentiation")
  private String differentiation;

  @Column(name = "budget_total", precision = 18, scale = 2)
  private BigDecimal budgetTotal;

  @Column(name = "pm_user_id")
  private Long pmUserId;

  /** 发起人（立项申请人） */
  @Column(name = "initiator_user_id")
  private Long initiatorUserId;

  @Column(name = "pmc_committee_id")
  private Long pmcCommitteeId;

  /** 流程监管部门（制度：效率管理部负责 PMS 维护与流程） */
  @Column(name = "process_oversight_dept_id")
  private Long processOversightDeptId;

  @Column(name = "current_milestone_id")
  private Long currentMilestoneId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Enums.ProjectStatus status = Enums.ProjectStatus.DRAFT;

  /** 评审状态：null/PENDING_REVIEW/IN_REVIEW/APPROVED/REJECTED */
  @Column(name = "review_status", length = 32)
  private String reviewStatus;

  /** 评审提交时间 */
  @Column(name = "review_submitted_at")
  private LocalDateTime reviewSubmittedAt;

  @Lob
  @Column(name = "terminated_reason")
  private String terminatedReason;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "planned_pcc_date")
  private LocalDate plannedPccDate;

  @Column(name = "planned_ind_date")
  private LocalDate plannedIndDate;

  @Column(name = "planned_nda_date")
  private LocalDate plannedNdaDate;

  @Column(name = "budget_to_pcc", precision = 18, scale = 2)
  private BigDecimal budgetToPcc;

  @Lob
  @Column(name = "risk_scientific")
  private String riskScientific;

  @Lob
  @Column(name = "risk_competitive")
  private String riskCompetitive;

  @Lob
  @Column(name = "risk_regulatory")
  private String riskRegulatory;

  @Lob
  @Column(name = "suggestion_and_support")
  private String suggestionAndSupport;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_by")
  private Long updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ProjectEntity() {}

  public Long getId() { return id; }
  public String getProjectNo() { return projectNo; }
  public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
  public Long getLevelId() { return levelId; }
  public void setLevelId(Long levelId) { this.levelId = levelId; }
  public String getProjectCode() { return projectCode; }
  public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
  public String getProjectName() { return projectName; }
  public void setProjectName(String projectName) { this.projectName = projectName; }
  public String getTargetPathway() { return targetPathway; }
  public void setTargetPathway(String targetPathway) { this.targetPathway = targetPathway; }
  public String getIndication() { return indication; }
  public void setIndication(String indication) { this.indication = indication; }
  public String getTppSummary() { return tppSummary; }
  public void setTppSummary(String tppSummary) { this.tppSummary = tppSummary; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getMechanism() { return mechanism; }
  public void setMechanism(String mechanism) { this.mechanism = mechanism; }
  public String getUnmetNeeds() { return unmetNeeds; }
  public void setUnmetNeeds(String unmetNeeds) { this.unmetNeeds = unmetNeeds; }
  public String getScientificBasis() { return scientificBasis; }
  public void setScientificBasis(String scientificBasis) { this.scientificBasis = scientificBasis; }
  public String getExpectedIndication() { return expectedIndication; }
  public void setExpectedIndication(String expectedIndication) { this.expectedIndication = expectedIndication; }
  public String getAdministrationRoute() { return administrationRoute; }
  public void setAdministrationRoute(String administrationRoute) { this.administrationRoute = administrationRoute; }
  public String getDosageForm() { return dosageForm; }
  public void setDosageForm(String dosageForm) { this.dosageForm = dosageForm; }
  public String getDosageFrequency() { return dosageFrequency; }
  public void setDosageFrequency(String dosageFrequency) { this.dosageFrequency = dosageFrequency; }
  public String getEfficacyTarget() { return efficacyTarget; }
  public void setEfficacyTarget(String efficacyTarget) { this.efficacyTarget = efficacyTarget; }
  public String getSafetyAdvantage() { return safetyAdvantage; }
  public void setSafetyAdvantage(String safetyAdvantage) { this.safetyAdvantage = safetyAdvantage; }
  public String getDifferentiation() { return differentiation; }
  public void setDifferentiation(String differentiation) { this.differentiation = differentiation; }
  public BigDecimal getBudgetTotal() { return budgetTotal; }
  public void setBudgetTotal(BigDecimal budgetTotal) { this.budgetTotal = budgetTotal; }
  public Long getPmUserId() { return pmUserId; }
  public void setPmUserId(Long pmUserId) { this.pmUserId = pmUserId; }
  public Long getInitiatorUserId() { return initiatorUserId; }
  public void setInitiatorUserId(Long initiatorUserId) { this.initiatorUserId = initiatorUserId; }
  public Long getPmcCommitteeId() { return pmcCommitteeId; }
  public void setPmcCommitteeId(Long pmcCommitteeId) { this.pmcCommitteeId = pmcCommitteeId; }
  public Long getProcessOversightDeptId() { return processOversightDeptId; }
  public void setProcessOversightDeptId(Long processOversightDeptId) { this.processOversightDeptId = processOversightDeptId; }
  public Long getCurrentMilestoneId() { return currentMilestoneId; }
  public void setCurrentMilestoneId(Long currentMilestoneId) { this.currentMilestoneId = currentMilestoneId; }
  public Enums.ProjectStatus getStatus() { return status; }
  public void setStatus(Enums.ProjectStatus status) { this.status = status; }
  public String getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
  public LocalDateTime getReviewSubmittedAt() { return reviewSubmittedAt; }
  public void setReviewSubmittedAt(LocalDateTime reviewSubmittedAt) { this.reviewSubmittedAt = reviewSubmittedAt; }
  public String getTerminatedReason() { return terminatedReason; }
  public void setTerminatedReason(String terminatedReason) { this.terminatedReason = terminatedReason; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public LocalDate getPlannedPccDate() { return plannedPccDate; }
  public void setPlannedPccDate(LocalDate plannedPccDate) { this.plannedPccDate = plannedPccDate; }
  public LocalDate getPlannedIndDate() { return plannedIndDate; }
  public void setPlannedIndDate(LocalDate plannedIndDate) { this.plannedIndDate = plannedIndDate; }
  public LocalDate getPlannedNdaDate() { return plannedNdaDate; }
  public void setPlannedNdaDate(LocalDate plannedNdaDate) { this.plannedNdaDate = plannedNdaDate; }
  public BigDecimal getBudgetToPcc() { return budgetToPcc; }
  public void setBudgetToPcc(BigDecimal budgetToPcc) { this.budgetToPcc = budgetToPcc; }
  public String getRiskScientific() { return riskScientific; }
  public void setRiskScientific(String riskScientific) { this.riskScientific = riskScientific; }
  public String getRiskCompetitive() { return riskCompetitive; }
  public void setRiskCompetitive(String riskCompetitive) { this.riskCompetitive = riskCompetitive; }
  public String getRiskRegulatory() { return riskRegulatory; }
  public void setRiskRegulatory(String riskRegulatory) { this.riskRegulatory = riskRegulatory; }
  public String getSuggestionAndSupport() { return suggestionAndSupport; }
  public void setSuggestionAndSupport(String suggestionAndSupport) { this.suggestionAndSupport = suggestionAndSupport; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

