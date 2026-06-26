package com.kbd.pms.service;

import com.kbd.pms.dto.TerminationRequestDto;
import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class ProjectTerminationService {

  private final ProjectRepository projectRepository;
  private final ProjectTerminationRequestRepository terminationRepository;
  private final ProjectTerminationTaskRepository terminationTaskRepository;
  private final IamUserRepository iamUserRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;

  public ProjectTerminationService(
      ProjectRepository projectRepository,
      ProjectTerminationRequestRepository terminationRepository,
      ProjectTerminationTaskRepository terminationTaskRepository,
      IamUserRepository iamUserRepository,
      OrgDepartmentRepository orgDepartmentRepository) {
    this.projectRepository = projectRepository;
    this.terminationRepository = terminationRepository;
    this.terminationTaskRepository = terminationTaskRepository;
    this.iamUserRepository = iamUserRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
  }

  /**
   * PM发起项目终止申请
   */
  @Transactional
  public TerminationRequestDto submitTermination(long projectId, long actorUserId,
      String reason, String attachmentUri) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    if (project.getPmUserId() == null || !project.getPmUserId().equals(actorUserId)) {
      throw new ApiException(403, "只有项目经理才能发起项目终止申请");
    }

    if (project.getStatus() == Enums.ProjectStatus.TERMINATED) {
      throw new ApiException(409, "项目已终止");
    }
    if (project.getStatus() == Enums.ProjectStatus.CLOSED) {
      throw new ApiException(409, "项目已结项");
    }

    // 检查是否有进行中的终止申请
    terminationRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
        .ifPresent(existing -> {
          if (existing.getStatus() == ProjectTerminationRequestEntity.Status.SUBMITTED
              || existing.getStatus() == ProjectTerminationRequestEntity.Status.EFFICIENCY_APPROVED) {
            throw new ApiException(409, "该项目已有进行中的终止申请");
          }
        });

    ProjectTerminationRequestEntity entity = new ProjectTerminationRequestEntity();
    entity.setProjectId(projectId);
    entity.setRequestedBy(actorUserId);
    entity.setTerminationReason(reason);
    entity.setAttachmentUri(attachmentUri);
    entity.setStatus(ProjectTerminationRequestEntity.Status.SUBMITTED);
    entity.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setCreatedAt(Instant.now());
    entity.setUpdatedAt(Instant.now());
    terminationRepository.save(entity);

    return toDto(entity);
  }

  /**
   * 效率管理部部门负责人审批终止申请
   */
  @Transactional
  public TerminationRequestDto approveByEfficiency(long terminationId, long approverId,
      String decision, String opinion) {
    ProjectTerminationRequestEntity entity = terminationRepository.findById(terminationId)
        .orElseThrow(() -> new ApiException(404, "终止申请不存在"));

    if (entity.getStatus() != ProjectTerminationRequestEntity.Status.SUBMITTED) {
      throw new ApiException(409, "当前状态不允许效率管理部审批: " + entity.getStatus());
    }

    OrgDepartmentEntity effDept = orgDepartmentRepository.findByDeptCode("ROSS_EFF").orElse(null);
    if (effDept == null || !effDept.getHeadUserId().equals(approverId)) {
      throw new ApiException(403, "只有效率管理部部门负责人才能审批");
    }

    if ("APPROVED".equals(decision)) {
      entity.setStatus(ProjectTerminationRequestEntity.Status.EFFICIENCY_APPROVED);
    } else {
      entity.setStatus(ProjectTerminationRequestEntity.Status.EFFICIENCY_REJECTED);
    }
    entity.setEfficiencyApproverId(approverId);
    entity.setEfficiencyOpinion(opinion);
    entity.setEfficiencyDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedAt(Instant.now());
    terminationRepository.save(entity);

    return toDto(entity);
  }

  /**
   * PMC审批终止申请
   */
  @Transactional
  public TerminationRequestDto approveByPmc(long terminationId, long approverId,
      String decision, String opinion) {
    ProjectTerminationRequestEntity entity = terminationRepository.findById(terminationId)
        .orElseThrow(() -> new ApiException(404, "终止申请不存在"));

    if (entity.getStatus() != ProjectTerminationRequestEntity.Status.EFFICIENCY_APPROVED) {
      throw new ApiException(409, "当前状态不允许PMC审批: " + entity.getStatus());
    }

    ProjectEntity project = projectRepository.findById(entity.getProjectId())
        .orElseThrow(() -> new ApiException(500, "项目不存在"));

    if ("APPROVED".equals(decision)) {
      // PMC批准终止
      entity.setStatus(ProjectTerminationRequestEntity.Status.PMC_APPROVED);
      project.setStatus(Enums.ProjectStatus.TERMINATED);
      project.setTerminatedReason(entity.getTerminationReason());
      project.setUpdatedAt(Instant.now());
      projectRepository.save(project);

      // 创建终止后任务清单（资产处置、文档归档、项目总结）
      createTerminationTasks(project.getId());

    } else {
      entity.setStatus(ProjectTerminationRequestEntity.Status.PMC_REJECTED);
    }
    entity.setPmcApproverId(approverId);
    entity.setPmcOpinion(opinion);
    entity.setPmcDecidedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedAt(Instant.now());
    terminationRepository.save(entity);

    return toDto(entity);
  }

  /**
   * PM完成终止任务（上传项目总结报告、确认资产处置、确认归档）
   */
  @Transactional
  public TerminationRequestDto completeTermination(long terminationId, long actorUserId,
      String summaryReportUri, boolean assetDisposalConfirmed, boolean archiveConfirmed) {
    ProjectTerminationRequestEntity entity = terminationRepository.findById(terminationId)
        .orElseThrow(() -> new ApiException(404, "终止申请不存在"));

    if (entity.getStatus() != ProjectTerminationRequestEntity.Status.PMC_APPROVED) {
      throw new ApiException(409, "当前状态不允许完成终止: " + entity.getStatus());
    }

    ProjectEntity project = projectRepository.findById(entity.getProjectId())
        .orElseThrow(() -> new ApiException(500, "项目不存在"));

    if (project.getPmUserId() == null || !project.getPmUserId().equals(actorUserId)) {
      throw new ApiException(403, "只有项目经理才能完成终止操作");
    }

    entity.setSummaryReportUri(summaryReportUri);
    entity.setAssetDisposalConfirmed(assetDisposalConfirmed);
    entity.setArchiveConfirmed(archiveConfirmed);
    entity.setStatus(ProjectTerminationRequestEntity.Status.COMPLETED);
    entity.setFinishedAt(LocalDateTime.now(ZoneOffset.UTC));
    entity.setUpdatedAt(Instant.now());

    // 项目最终关闭
    project.setStatus(Enums.ProjectStatus.CLOSED);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    terminationRepository.save(entity);
    return toDto(entity);
  }

  /**
   * PMC成员执行终止决策（兼容统一审批入口）
   */
  @Transactional
  public TerminationRequestDto executePmcDecision(long terminationId, long approverId,
      String decision, String opinion) {
    ProjectTerminationRequestEntity entity = terminationRepository.findById(terminationId)
        .orElseThrow(() -> new ApiException(404, "终止申请不存在"));

    if (entity.getStatus() == ProjectTerminationRequestEntity.Status.SUBMITTED) {
      return approveByEfficiency(terminationId, approverId, decision, opinion);
    }
    if (entity.getStatus() == ProjectTerminationRequestEntity.Status.EFFICIENCY_APPROVED) {
      return approveByPmc(terminationId, approverId, decision, opinion);
    }
    throw new ApiException(409, "当前状态不允许执行决策: " + entity.getStatus());
  }

  /**
   * 获取项目的终止申请列表
   */
  @Transactional(readOnly = true)
  public List<TerminationRequestDto> getProjectTerminations(long projectId) {
    return terminationRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
        .stream().map(this::toDto).toList();
  }

  /**
   * 获取单个终止申请详情
   */
  @Transactional(readOnly = true)
  public TerminationRequestDto getTermination(long terminationId) {
    return terminationRepository.findById(terminationId)
        .map(this::toDto)
        .orElseThrow(() -> new ApiException(404, "终止申请不存在"));
  }

  /**
   * 获取所有待处理的终止申请
   */
  @Transactional(readOnly = true)
  public List<TerminationRequestDto> getPendingTerminations() {
    return terminationRepository.findAll().stream()
        .filter(e -> e.getStatus() == ProjectTerminationRequestEntity.Status.SUBMITTED
            || e.getStatus() == ProjectTerminationRequestEntity.Status.EFFICIENCY_APPROVED)
        .map(this::toDto).toList();
  }

  private void createTerminationTasks(Long projectId) {
    // 1. 项目总结报告
    ProjectTerminationTaskEntity task1 = new ProjectTerminationTaskEntity();
    task1.setProjectId(projectId);
    task1.setTaskCode("PROJECT_SUMMARY");
    task1.setTaskDescription("项目经理需在一个月内完成项目总结报告，上传至系统。");
    task1.setStatus(Enums.TerminationTaskStatus.OPEN);
    task1.setDueDate(LocalDate.now(ZoneOffset.UTC).plusMonths(1));
    task1.setCreatedAt(Instant.now());
    task1.setUpdatedAt(Instant.now());
    terminationTaskRepository.save(task1);

    // 2. 资产处置
    ProjectTerminationTaskEntity task2 = new ProjectTerminationTaskEntity();
    task2.setProjectId(projectId);
    task2.setTaskCode("ASSET_DISPOSAL");
    task2.setTaskDescription("完成项目相关资产的盘点与处置。");
    task2.setStatus(Enums.TerminationTaskStatus.OPEN);
    task2.setDueDate(LocalDate.now(ZoneOffset.UTC).plusMonths(1));
    task2.setCreatedAt(Instant.now());
    task2.setUpdatedAt(Instant.now());
    terminationTaskRepository.save(task2);

    // 3. 文档归档
    ProjectTerminationTaskEntity task3 = new ProjectTerminationTaskEntity();
    task3.setProjectId(projectId);
    task3.setTaskCode("DOCUMENT_ARCHIVE");
    task3.setTaskDescription("所有项目文件按《研发文档管理制度》完成归档。");
    task3.setStatus(Enums.TerminationTaskStatus.OPEN);
    task3.setDueDate(LocalDate.now(ZoneOffset.UTC).plusMonths(1));
    task3.setCreatedAt(Instant.now());
    task3.setUpdatedAt(Instant.now());
    terminationTaskRepository.save(task3);
  }

  private TerminationRequestDto toDto(ProjectTerminationRequestEntity entity) {
    String requesterName = entity.getRequestedBy() != null
        ? iamUserRepository.findById(entity.getRequestedBy())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String effName = entity.getEfficiencyApproverId() != null
        ? iamUserRepository.findById(entity.getEfficiencyApproverId())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;
    String pmcName = entity.getPmcApproverId() != null
        ? iamUserRepository.findById(entity.getPmcApproverId())
            .map(IamUserEntity::getDisplayName).orElse(null) : null;

    return new TerminationRequestDto(
        entity.getId(), entity.getProjectId(),
        entity.getRequestedBy(), requesterName,
        entity.getTerminationReason(), entity.getAttachmentUri(),
        entity.getStatus().name(),
        entity.getEfficiencyApproverId(), effName,
        entity.getEfficiencyOpinion(), entity.getEfficiencyDecidedAt(),
        entity.getPmcApproverId(), pmcName,
        entity.getPmcOpinion(), entity.getPmcDecidedAt(),
        entity.getSummaryReportUri(),
        entity.getAssetDisposalConfirmed(),
        entity.getArchiveConfirmed(),
        entity.getSubmittedAt(), entity.getFinishedAt());
  }
}