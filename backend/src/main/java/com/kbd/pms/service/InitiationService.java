package com.kbd.pms.service;

import com.kbd.pms.dto.InitiationApprovalDto;
import com.kbd.pms.dto.InitiationApprovalTaskDto;
import com.kbd.pms.dto.InitiationDecisionRequest;
import com.kbd.pms.dto.InitiationSubmitRequest;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.InitiationApprovalEntity;
import com.kbd.pms.entity.InitiationApprovalTaskEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.User;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.InitiationApprovalRepository;
import com.kbd.pms.repository.InitiationApprovalTaskRepository;
import com.kbd.pms.repository.ProjectRepository;
import com.kbd.pms.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitiationService {

  private final ProjectRepository projectRepository;
  private final InitiationApprovalRepository initiationApprovalRepository;
  private final InitiationApprovalTaskRepository initiationApprovalTaskRepository;
  private final UserRepository userRepository;
  private static final String PERMISSION_APPROVE_INITIATION = "PERMISSION_APPROVE_INITIATION";

  public InitiationService(
      ProjectRepository projectRepository,
      InitiationApprovalRepository initiationApprovalRepository,
      InitiationApprovalTaskRepository initiationApprovalTaskRepository,
      UserRepository userRepository) {
    this.projectRepository = projectRepository;
    this.initiationApprovalRepository = initiationApprovalRepository;
    this.initiationApprovalTaskRepository = initiationApprovalTaskRepository;
    this.userRepository = userRepository;
  }

  /**
   * 项目经理提交立项申请
   */
  @Transactional
  public InitiationApprovalDto submitInitiation(long projectId, InitiationSubmitRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    // 检查是否已有进行中的立项申请
    initiationApprovalRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
        .ifPresent(existing -> {
          if (existing.getStatus() == InitiationApprovalEntity.Status.SUBMITTED) {
            throw new ApiException(409, "该项目已有进行中的立项申请，请等待审批完成");
          }
        });

    // 创建立项申请记录
    InitiationApprovalEntity approval = new InitiationApprovalEntity();
    approval.setProjectId(projectId);
    approval.setSubmitterUserId(request.actorUserId());
    approval.setApplicationContent(request.applicationContent());
    approval.setStatus(InitiationApprovalEntity.Status.SUBMITTED);
    approval.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    approval.setCreatedAt(Instant.now());
    approval.setUpdatedAt(Instant.now());
    initiationApprovalRepository.save(approval);

    // 更新项目立项状态
    project.setInitiationStatus("SUBMITTED");
    project.setInitiationSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
    project.setInitiationApplication(request.applicationContent());
    // 项目状态变为 ACTIVE（G0，进行中）
    project.setStatus(Enums.ProjectStatus.ACTIVE);
    project.setUpdatedAt(Instant.now());
    projectRepository.save(project);

    // 自动创建PMC成员审批任务
    createApproverTasks(approval);

    return toApprovalDto(approval);
  }

  /**
   * PMC成员执行审批决策
   */
  @Transactional
  public InitiationApprovalDto executeDecision(long projectId, long taskId, InitiationDecisionRequest request) {
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + projectId));

    InitiationApprovalTaskEntity task = initiationApprovalTaskRepository.findById(taskId)
        .orElseThrow(() -> new ApiException(404, "审批任务不存在: id=" + taskId));

    if (task.getApproverUserId() != request.actorUserId()) {
      throw new ApiException(403, "您不是此审批任务的审批人");
    }

    if (task.getStatus() != InitiationApprovalTaskEntity.Status.PENDING) {
      throw new ApiException(409, "审批任务已处理，无法重复审批");
    }

    InitiationApprovalEntity approval = initiationApprovalRepository.findById(task.getInitiationApprovalId())
        .orElseThrow(() -> new ApiException(500, "审批记录不存在"));

    if (approval.getStatus() != InitiationApprovalEntity.Status.SUBMITTED) {
      throw new ApiException(409, "审批记录状态不允许审批: " + approval.getStatus());
    }

    String decision = request.decision();
    if (!List.of("APPROVED", "REJECTED").contains(decision)) {
      throw new ApiException(400, "无效的决策类型: " + decision + "，有效值为: APPROVED, REJECTED");
    }

    // 更新任务状态
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    boolean isApproved = "APPROVED".equals(decision);
    task.setStatus(isApproved
        ? InitiationApprovalTaskEntity.Status.APPROVED
        : InitiationApprovalTaskEntity.Status.REJECTED);
    task.setDecision(decision);
    task.setOpinion(request.opinion());
    task.setDecidedAt(now);
    task.setUpdatedAt(Instant.now());
    initiationApprovalTaskRepository.save(task);

    // 检查所有审批人状态
    List<InitiationApprovalTaskEntity> allTasks =
        initiationApprovalTaskRepository.findByInitiationApprovalIdOrderBySortOrderAsc(approval.getId());
    boolean allApproved = allTasks.stream()
        .allMatch(t -> t.getStatus() == InitiationApprovalTaskEntity.Status.APPROVED);
    boolean anyRejected = allTasks.stream()
        .anyMatch(t -> t.getStatus() == InitiationApprovalTaskEntity.Status.REJECTED);

    if (anyRejected) {
      // 任一审批人拒绝 → 整体驳回
      approval.setStatus(InitiationApprovalEntity.Status.REJECTED);
      approval.setFinishedAt(now);
      approval.setUpdatedAt(Instant.now());
      initiationApprovalRepository.save(approval);

      // 更新项目立项状态为驳回
      project.setInitiationStatus("REJECTED");
      project.setUpdatedAt(Instant.now());
      projectRepository.save(project);

    } else if (allApproved) {
      // 所有PMC成员都同意 → 立项通过
      approval.setStatus(InitiationApprovalEntity.Status.APPROVED);
      approval.setFinishedAt(now);
      approval.setUpdatedAt(Instant.now());
      initiationApprovalRepository.save(approval);

      // 更新项目立项状态为通过
      project.setInitiationStatus("APPROVED");
      project.setUpdatedAt(Instant.now());
      projectRepository.save(project);
    }

    return toApprovalDto(approval);
  }

  /**
   * 获取项目的立项申请审批状态
   */
  @Transactional
  public InitiationApprovalDto getInitiationStatus(long projectId) {
    return initiationApprovalRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId)
        .map(approval -> {
          if (approval.getStatus() == InitiationApprovalEntity.Status.SUBMITTED) {
            ensureApproverTasks(approval);
          }
          return toApprovalDto(approval);
        })
        .orElse(null);
  }

  /**
   * 获取当前用户的待审批任务（作为PMC成员）
   */
  @Transactional(readOnly = true)
  public List<InitiationApprovalTaskDto> getMyApprovalTasks(long userId) {
    List<InitiationApprovalTaskEntity> tasks =
        initiationApprovalTaskRepository.findByApproverUserIdOrderByCreatedAtDesc(userId);
    return tasks.stream()
        .filter(t -> t.getStatus() == InitiationApprovalTaskEntity.Status.PENDING)
        .map(this::toTaskDto)
        .toList();
  }

  // ==================== 私有辅助方法 ====================

  private void createApproverTasks(InitiationApprovalEntity approval) {
    List<User> approvers =
        userRepository.findActiveUsersByPermissionName(PERMISSION_APPROVE_INITIATION);
    if (approvers.isEmpty()) {
      throw new ApiException(500, "未配置拥有「审批立项申请」权限的审批人，无法发起立项审批");
    }

    int sortOrder = 1;
    for (User approver : approvers) {
      if (approver.getId().equals(approval.getSubmitterUserId())) {
        continue;
      }
      InitiationApprovalTaskEntity task = new InitiationApprovalTaskEntity();
      task.setInitiationApprovalId(approval.getId());
      task.setApproverUserId(approver.getId());
      task.setApproverRole(resolvePrimaryRole(approver));
      task.setSortOrder(sortOrder++);
      task.setStatus(InitiationApprovalTaskEntity.Status.PENDING);
      task.setCreatedAt(Instant.now());
      task.setUpdatedAt(Instant.now());
      initiationApprovalTaskRepository.save(task);
    }

    if (sortOrder == 1) {
      throw new ApiException(500, "除提交人外无可用审批人，请为其他账号分配「审批立项申请」权限");
    }
  }

  private void ensureApproverTasks(InitiationApprovalEntity approval) {
    List<InitiationApprovalTaskEntity> existing =
        initiationApprovalTaskRepository.findByInitiationApprovalIdOrderBySortOrderAsc(approval.getId());
    if (!existing.isEmpty()) {
      return;
    }
    projectRepository.findById(approval.getProjectId())
        .orElseThrow(() -> new ApiException(404, "项目不存在: id=" + approval.getProjectId()));
    createApproverTasks(approval);
  }

  private String resolvePrimaryRole(User user) {
    return user.getRoles().stream()
        .map(role -> role.getName())
        .findFirst()
        .orElse("APPROVER");
  }

  private InitiationApprovalDto toApprovalDto(InitiationApprovalEntity entity) {
    List<InitiationApprovalTaskEntity> tasks =
        initiationApprovalTaskRepository.findByInitiationApprovalIdOrderBySortOrderAsc(entity.getId());

    String submitterName = entity.getSubmitterUserId() != null
        ? userRepository.findById(entity.getSubmitterUserId())
            .map(User::getUsername).orElse(null)
        : null;

    return new InitiationApprovalDto(
        entity.getId(),
        entity.getProjectId(),
        entity.getSubmitterUserId(),
        submitterName,
        entity.getApplicationContent(),
        entity.getStatus().name(),
        entity.getSubmittedAt(),
        entity.getFinishedAt(),
        tasks.stream().map(this::toTaskDto).toList()
    );
  }

  private InitiationApprovalTaskDto toTaskDto(InitiationApprovalTaskEntity entity) {
    String approverName = userRepository.findById(entity.getApproverUserId())
        .map(User::getUsername).orElse("未知用户");

    return new InitiationApprovalTaskDto(
        entity.getId(),
        entity.getInitiationApprovalId(),
        entity.getApproverUserId(),
        approverName,
        entity.getApproverRole(),
        entity.getSortOrder(),
        entity.getStatus().name(),
        entity.getDecision(),
        entity.getOpinion(),
        entity.getDecidedAt()
    );
  }
}
