package com.kbd.pms.service;

import com.kbd.pms.dto.ProjectChangeRequestDecisionRequest;
import com.kbd.pms.dto.ProjectChangeRequestDto;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.IamUserEntity;
import com.kbd.pms.entity.OrgDepartmentEntity;
import com.kbd.pms.entity.ProjectChangeRequestEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ProjectChangeRequestService {

  private final ProjectRepository projectRepository;
  private final ProjectChangeRequestRepository changeRequestRepository;
  private final IamUserRepository iamUserRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;

  public ProjectChangeRequestService(
      ProjectRepository projectRepository,
      ProjectChangeRequestRepository changeRequestRepository,
      IamUserRepository iamUserRepository,
      OrgDepartmentRepository orgDepartmentRepository) {
    this.projectRepository = projectRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.iamUserRepository = iamUserRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
  }

  /**
   * PM发起项目变更申请
   */
  @Transactional
  public ProjectChangeRequestDto submitChange(long projectId, ProjectChangeRequestDto request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    // 验证发起人是否为项目经理
    if (project.getPmUserId() == null || !project.getPmUserId().equals(request.requestedBy())) {
      throw new ApiException(403, "只有项目经理才能发起项目变更申请");
    }

    // 检查是否有进行中的变更申请
    List<ProjectChangeRequestEntity> activeList = changeRequestRepository
        .findByProjectIdOrderByRequestedAtDesc(projectId);
    for (ProjectChangeRequestEntity existing : activeList) {
      String status = existing.getStatus();
      if ("SUBMITTED".equals(status) || "EFFICIENCY_APPROVED".equals(status)) {
        throw new ApiException(409, "该项目已有进行中的变更申请");
      }
    }

    ProjectChangeRequestEntity entity = new ProjectChangeRequestEntity();
    entity.setProjectId(projectId);
    entity.setChangeType(request.changeType() != null ? Enums.ChangeType.valueOf(request.changeType()) : Enums.ChangeType.OTHER);
    entity.setReasonText(request.reasonText());
    entity.setAttachmentUri(request.attachmentUri());
    entity.setBeforeText(request.beforeText());
    entity.setAfterText(request.afterText());
    entity.setImpactMilestoneText(request.impactMilestoneText());
    entity.setImpactBudgetText(request.impactBudgetText());
    entity.setImpactResourceText(request.impactResourceText());
    entity.setRequestedBy(request.requestedBy());
    entity.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setStatus("SUBMITTED"); // 等待效率管理部审批
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  /**
   * 效率管理部部门负责人审批变更申请
   */
  @Transactional
  public ProjectChangeRequestDto approveByEfficiency(long changeId, long approverId, String decision, String opinion) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));

    if (!"SUBMITTED".equals(entity.getStatus())) {
      throw new ApiException(409, "当前状态不允许效率管理部审批: " + entity.getStatus());
    }

    // 验证用户是否为效率管理部负责人
    IamUserEntity user = iamUserRepository.findById(approverId)
        .orElseThrow(() -> new ApiException(404, "用户不存在"));
    OrgDepartmentEntity effDept = orgDepartmentRepository.findByDeptCode("ROSS_EFF").orElse(null);
    if (effDept == null || !effDept.getHeadUserId().equals(approverId)) {
      throw new ApiException(403, "只有效率管理部部门负责人才能审批");
    }

    if ("APPROVED".equals(decision)) {
      entity.setStatus("EFFICIENCY_APPROVED");
      // 进入PMC审批阶段
    } else {
      entity.setStatus("EFFICIENCY_REJECTED");
    }
    entity.setEfficiencyApproverId(approverId);
    entity.setEfficiencyOpinion(opinion);
    entity.setEfficiencyDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  /**
   * PMC审批变更申请
   */
  @Transactional
  public ProjectChangeRequestDto approveByPmc(long changeId, long approverId, ProjectChangeRequestDecisionRequest request) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));

    if (!"EFFICIENCY_APPROVED".equals(entity.getStatus())) {
      throw new ApiException(409, "当前状态不允许PMC审批: " + entity.getStatus());
    }

    switch (request.decision()) {
      case APPROVE -> {
        entity.setStatus("APPROVED");
        entity.setPmcDecision(Enums.PmcDecision.APPROVE);
        // 应用变更到项目
        applyChange(entity);
      }
      case REJECT -> {
        entity.setStatus("REJECTED");
        entity.setPmcDecision(Enums.PmcDecision.REJECT);
      }
      case CONDITIONAL_APPROVE -> {
        entity.setStatus("APPROVED");
        entity.setPmcDecision(Enums.PmcDecision.CONDITIONAL_APPROVE);
        applyChange(entity);
      }
    }

    entity.setPmcDecisionText(request.opinion());
    entity.setPmcDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setPmcDecidedBy(approverId);
    entity.setUpdatedAt(Instant.now());
    changeRequestRepository.save(entity);

    return toDto(entity);
  }

  /**
   * 将变更应用到项目
   */
  private void applyChange(ProjectChangeRequestEntity entity) {
    ProjectEntity project = projectRepository.findById(entity.getProjectId())
        .orElseThrow(() -> new ApiException(500, "项目不存在"));

    switch (entity.getChangeType()) {
      case MILESTONE_SCHEDULE -> {
        if (entity.getTargetMilestonePlannedDate() != null) {
          project.setPlannedPccDate(entity.getTargetMilestonePlannedDate());
        }
      }
      case BUDGET -> {
        if (entity.getRequestedBudgetAmount() != null) {
          project.setBudgetTotal(entity.getRequestedBudgetAmount());
        }
      }
      case OWNER_PM -> {
        if (entity.getNewPmUserId() != null) {
          project.setPmUserId(entity.getNewPmUserId());
        }
      }
      case PAUSE_TERMINATE -> {
        project.setStatus(Enums.ProjectStatus.TERMINATED);
        project.setTerminatedReason(entity.getReasonText());
      }
      // OBJECTIVE_SCOPE, OTHER: 不自动修改项目属性，由PMC手动确认
      default -> { /* no-op */ }
    }

    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);
  }

  /**
   * 获取项目的变更申请列表
   */
  @Transactional(readOnly = true)
  public List<ProjectChangeRequestDto> getProjectChangeRequests(long projectId) {
    return changeRequestRepository.findByProjectIdOrderByRequestedAtDesc(projectId)
        .stream().map(this::toDto).toList();
  }

  /**
   * 获取单个变更申请详情
   */
  @Transactional(readOnly = true)
  public ProjectChangeRequestDto getChangeRequest(long changeId) {
    return changeRequestRepository.findById(changeId)
        .map(this::toDto)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));
  }

  /**
   * 获取待审批的变更申请列表（效率管理部或PMC）
   */
  @Transactional(readOnly = true)
  public List<ProjectChangeRequestDto> getPendingChangeRequests() {
    List<ProjectChangeRequestEntity> all = changeRequestRepository.findAll();
    return all.stream()
        .filter(e -> "SUBMITTED".equals(e.getStatus()) || "EFFICIENCY_APPROVED".equals(e.getStatus()))
        .map(this::toDto)
        .toList();
  }

  // 兼容旧API
  @Transactional
  public ProjectChangeRequestDto executeDecision(long changeId, long approverId, ProjectChangeRequestDecisionRequest request) {
    ProjectChangeRequestEntity entity = changeRequestRepository.findById(changeId)
        .orElseThrow(() -> new ApiException(404, "变更申请不存在"));
    if ("SUBMITTED".equals(entity.getStatus())) {
      return approveByEfficiency(changeId, approverId, request.decision().name(), request.opinion());
    }
    return approveByPmc(changeId, approverId, request);
  }

  private ProjectChangeRequestDto toDto(ProjectChangeRequestEntity entity) {
    String projectName = projectRepository.findById(entity.getProjectId())
        .map(ProjectEntity::getProjectName).orElse(null);
    String requesterName = entity.getRequestedBy() != null
        ? iamUserRepository.findById(entity.getRequestedBy())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String efficiencyApproverName = entity.getEfficiencyApproverId() != null
        ? iamUserRepository.findById(entity.getEfficiencyApproverId())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String pmcDeciderName = entity.getPmcDecidedBy() != null
        ? iamUserRepository.findById(entity.getPmcDecidedBy())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;

    return new ProjectChangeRequestDto(
        entity.getId(), entity.getProjectId(), projectName,
        entity.getChangeType() != null ? entity.getChangeType().name() : null,
        entity.getReasonText(), entity.getAttachmentUri(),
        entity.getBeforeText(), entity.getAfterText(),
        entity.getImpactMilestoneText(), entity.getImpactBudgetText(),
        entity.getImpactResourceText(), entity.getRequestedBy(), requesterName,
        entity.getRequestedAt(), entity.getStatus(),
        entity.getEfficiencyApproverId(), efficiencyApproverName,
        entity.getEfficiencyOpinion(), entity.getEfficiencyDecidedAt(),
        entity.getPmcDecision() != null ? entity.getPmcDecision().name() : null,
        entity.getPmcDecisionText(), entity.getPmcDecidedAt(),
        entity.getPmcDecidedBy(), pmcDeciderName
    );
  }
}