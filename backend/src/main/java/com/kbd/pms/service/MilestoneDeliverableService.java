package com.kbd.pms.service;

import com.kbd.pms.dto.DeliverableUploadRequest;
import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import com.kbd.pms.workflow.WfProcessDefinition;
import com.kbd.pms.workflow.WfProcessNode;
import com.kbd.pms.workflow.WfProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 里程碑交付物服务
 *
 * <p>整合交付物定义（milestone_deliverable_def）与文档实体（document），
 * 为里程碑控制台提供"按槽位上传/查看交付物"的能力。</p>
 */
@Service
public class MilestoneDeliverableService {

  private final MilestoneDeliverableDefRepository deliverableDefRepository;
  private final DocumentRepository documentRepository;
  private final FileStorageService fileStorageService;
  private final ProjectRepository projectRepository;
  private final SecurityHelper securityHelper;
  private final UserRepository userRepository;
  private final AuditLogService auditLogService;
  private final NotificationService notificationService;
  private final MilestoneDefRepository milestoneDefRepository;
  private final MilestoneDeptRoleRepository milestoneDeptRoleRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final WfProcessRepository wfProcessRepository;
  private final ReviewApprovalRepository reviewApprovalRepository;
  private final ReviewApprovalTaskRepository reviewApprovalTaskRepository;
  private final ProjectMilestoneRepository projectMilestoneRepository;
  private final ProjectTeamMemberRepository projectTeamMemberRepository;

  public MilestoneDeliverableService(
      MilestoneDeliverableDefRepository deliverableDefRepository,
      DocumentRepository documentRepository,
      FileStorageService fileStorageService,
      ProjectRepository projectRepository,
      SecurityHelper securityHelper,
      UserRepository userRepository,
      AuditLogService auditLogService,
      NotificationService notificationService,
      MilestoneDefRepository milestoneDefRepository,
      MilestoneDeptRoleRepository milestoneDeptRoleRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      WfProcessRepository wfProcessRepository,
      ReviewApprovalRepository reviewApprovalRepository,
      ReviewApprovalTaskRepository reviewApprovalTaskRepository,
      ProjectMilestoneRepository projectMilestoneRepository,
      ProjectTeamMemberRepository projectTeamMemberRepository) {
    this.deliverableDefRepository = deliverableDefRepository;
    this.documentRepository = documentRepository;
    this.fileStorageService = fileStorageService;
    this.projectRepository = projectRepository;
    this.securityHelper = securityHelper;
    this.userRepository = userRepository;
    this.auditLogService = auditLogService;
    this.notificationService = notificationService;
    this.milestoneDefRepository = milestoneDefRepository;
    this.milestoneDeptRoleRepository = milestoneDeptRoleRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.wfProcessRepository = wfProcessRepository;
    this.reviewApprovalRepository = reviewApprovalRepository;
    this.reviewApprovalTaskRepository = reviewApprovalTaskRepository;
    this.projectMilestoneRepository = projectMilestoneRepository;
    this.projectTeamMemberRepository = projectTeamMemberRepository;
  }

  /**
   * 获取指定里程碑阶段的交付物清单（含已上传状态）
   *
   * @param projectId     项目ID
   * @param milestoneCode 里程碑代码（如 "G0"）
   * @return 交付物槽位列表，每个槽位包含定义信息和已上传的文档
   */
  @Transactional(readOnly = true)
  public List<DeliverableSlotVO> getDeliverableSlots(Long projectId, String milestoneCode) {
    // 校验项目存在
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    // 获取该阶段的交付物定义
    List<MilestoneDeliverableDefEntity> defs = deliverableDefRepository
        .findByMilestoneCodeAndIsActiveTrueOrderBySortNoAsc(milestoneCode);

    // 获取该项目该阶段已上传的文档
    Enums.MilestoneStage stage = Enums.MilestoneStage.valueOf(milestoneCode);
    List<DocumentEntity> uploadedDocs = getVisibleDocuments(projectId).stream()
        .filter(doc -> doc.getMilestonePhase() == stage)
        .toList();

    // 按 slotCode 分组
    Map<String, List<DocumentEntity>> docsBySlot = uploadedDocs.stream()
        .filter(d -> d.getDeliverableSlotCode() != null)
        .collect(Collectors.groupingBy(DocumentEntity::getDeliverableSlotCode));

    // 组装 VO
    return defs.stream().map(def -> {
      DeliverableSlotVO vo = new DeliverableSlotVO();
      vo.setSlotCode(def.getSlotCode());
      vo.setSlotName(def.getSlotName());
      vo.setIsRequired(def.getIsRequired());
      vo.setDescription(def.getDescription());
      vo.setAllowedFileTypes(def.getAllowedFileTypes());
      vo.setDocuments(docsBySlot.getOrDefault(def.getSlotCode(), List.of()));
      return vo;
    }).collect(Collectors.toList());
  }

  /** 项目详情交付物管理：跨 G0-G9 返回全部固定槽位，空槽位也返回用于占位展示/定点上传。 */
  @Transactional(readOnly = true)
  public List<MilestoneDeliverableGroupVO> getProjectDeliverableSlotGroups(Long projectId) {
    projectRepository.findById(projectId).orElseThrow(() -> new ApiException(404, "项目不存在"));
    List<MilestoneDefEntity> milestones = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    Map<String, MilestoneDefEntity> milestoneByCode = milestones.stream()
        .collect(Collectors.toMap(MilestoneDefEntity::getMilestoneCode, m -> m, (a, b) -> a, LinkedHashMap::new));
    List<DocumentEntity> visibleDocs = getVisibleDocuments(projectId);

    return Arrays.stream(Enums.MilestoneStage.values()).map(stage -> {
      String milestoneCode = stage.name();
      MilestoneDefEntity milestone = milestoneByCode.get(milestoneCode);
      List<MilestoneDeliverableDefEntity> defs = deliverableDefRepository
          .findByMilestoneCodeAndIsActiveTrueOrderBySortNoAsc(milestoneCode);
      Map<String, List<DocumentEntity>> docsBySlot = visibleDocs.stream()
          .filter(doc -> doc.getMilestonePhase() == stage)
          .filter(doc -> doc.getDeliverableSlotCode() != null)
          .collect(Collectors.groupingBy(DocumentEntity::getDeliverableSlotCode));
      List<DeliverableSlotVO> slots = defs.stream().map(def -> toSlotVO(def,
          docsBySlot.getOrDefault(def.getSlotCode(), List.of()))).toList();
      MilestoneDeliverableGroupVO group = new MilestoneDeliverableGroupVO();
      group.setMilestoneCode(milestoneCode);
      group.setMilestoneName(milestone == null ? milestoneCode : milestone.getMilestoneName());
      group.setPhaseLabel(milestone == null ? milestoneCode : milestoneCode + "-" + milestone.getMilestoneName());
      group.setSlots(slots);
      group.setUploadedCount((int) slots.stream().filter(slot -> slot.getDocuments() != null && !slot.getDocuments().isEmpty()).count());
      group.setTotalCount(slots.size());
      return group;
    }).toList();
  }

  private DeliverableSlotVO toSlotVO(MilestoneDeliverableDefEntity def, List<DocumentEntity> documents) {
    DeliverableSlotVO vo = new DeliverableSlotVO();
    vo.setSlotCode(def.getSlotCode());
    vo.setSlotName(def.getSlotName());
    vo.setIsRequired(def.getIsRequired());
    vo.setDescription(def.getDescription());
    vo.setAllowedFileTypes(def.getAllowedFileTypes());
    vo.setDocuments(documents);
    return vo;
  }

  /**
   * 上传交付物到指定槽位
   */
  @Transactional
  public DocumentEntity uploadDeliverable(MultipartFile file, DeliverableUploadRequest request) throws IOException {
    long currentUserId = securityHelper.getCurrentUserId();

    // 校验项目
    ProjectEntity project = projectRepository.findById(request.projectId())
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    // 校验交付物槽位定义
    MilestoneDeliverableDefEntity def = deliverableDefRepository
        .findByMilestoneCodeAndSlotCode(request.milestoneCode(), request.deliverableSlotCode())
        .orElseThrow(() -> new ApiException(400, "交付物槽位不存在: " + request.deliverableSlotCode()));

    // 权限校验：PM / 部门执行人 可上传
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));
    List<String> roles = currentUser.getRoles().stream().map(Role::getName).toList();

    // 权限校验：ADMIN / 项目PM 可上传所有里程碑交付物；
    // 部门执行人需要属于当前里程碑的执行部门
    boolean canUpload = roles.contains("ROLE_ADMIN")
        || (roles.contains("ROLE_PM") && project.getPmUserId() != null
            && project.getPmUserId().equals(currentUserId));

    if (!canUpload && roles.contains("ROLE_DEPT_EXECUTOR")) {
      // 检查用户所在部门是否在里程碑的执行部门列表中
      List<Long> executorDeptIds = getExecutorDeptIdsByMilestoneCode(request.milestoneCode());
      if (!executorDeptIds.isEmpty()) {
        Set<OrgDepartmentEntity> userDepts = currentUser.getDepartments();
        if (userDepts != null) {
          for (OrgDepartmentEntity userDept : userDepts) {
            if (executorDeptIds.contains(userDept.getId())) {
              canUpload = true;
              break;
            }
          }
        }
      }
    }

    if (!canUpload) {
      String deptNames = getExecutorDeptNamesByMilestoneCode(request.milestoneCode());
      if (deptNames != null && !deptNames.isEmpty()) {
        throw new ApiException(403, "仅「" + deptNames + "」的部门执行人可上传交付物");
      }
      throw new ApiException(403, "无权限上传交付物");
    }

    // 同一槽位只允许一个文件（如果已存在则拒绝，前端可先删除再上传）
    Enums.MilestoneStage stage = Enums.MilestoneStage.valueOf(request.milestoneCode());
    List<DocumentEntity> existing = documentRepository
        .findByProjectIdAndMilestonePhaseAndDeliverableSlotCode(
            request.projectId(), stage, request.deliverableSlotCode());
    if (!existing.isEmpty()) {
      throw new ApiException(409, "该交付物槽位已存在文件，请先删除后再上传");
    }

    // 存储文件
    String storagePath = fileStorageService.storeFile(
        file, project.getProjectCode(), stage, currentUserId);

    // 创建文档记录
    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(file.getOriginalFilename());
    doc.setStoragePath(storagePath);
    doc.setFileType(def.getSlotName());
    doc.setProjectId(request.projectId());
    doc.setMilestonePhase(stage);
    doc.setDeliverableSlotCode(request.deliverableSlotCode());
    doc.setUploader(currentUserId);
    doc.setComplianceStatus(Enums.ComplianceStatus.PENDING);
    doc.setIsLocked(false);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setCreatedAt(Instant.now());

    DocumentEntity saved = documentRepository.save(doc);
    notificationService.completeTodoByProjectAndMilestone(
        request.projectId(), request.milestoneCode(), "DELIVERABLE");
    auditLogService.logAction(currentUserId, "UPLOAD_DELIVERABLE", saved.getId(),
        "Milestone " + request.milestoneCode() + " deliverable uploaded: " + def.getSlotName());
    return saved;
  }

  /** 管理员历史项目导入：必须明确阶段和交付物槽位，避免文件落入不可追溯的默认目录。 */
  @Transactional
  public DocumentEntity importDeliverable(MultipartFile file, Long projectId, String milestoneCode,
      String slotCode, String fileType) throws IOException {
    long currentUserId = securityHelper.getCurrentUserId();
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));
    List<String> roles = currentUser.getRoles().stream().map(Role::getName).toList();
    if (!hasDeliverablePermission(currentUser, "PERMISSION_DELIVERABLE_IMPORT")
        && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_PROJECT_ADMIN")) {
      throw new ApiException(403, "仅系统管理员和项目管理员可以导入历史交付物");
    }
    if (file == null || file.isEmpty()) throw new ApiException(400, "请选择要导入的文件");
    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));
    MilestoneDeliverableDefEntity def = deliverableDefRepository
        .findByMilestoneCodeAndSlotCode(milestoneCode, slotCode)
        .orElseThrow(() -> new ApiException(400, "交付物槽位不存在: " + slotCode));
    Enums.MilestoneStage stage;
    try {
      stage = Enums.MilestoneStage.valueOf(milestoneCode);
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "里程碑阶段必须为 G0-G9");
    }
    if (!documentRepository.findByProjectIdAndMilestonePhaseAndDeliverableSlotCode(projectId, stage, slotCode).isEmpty()) {
      throw new ApiException(409, "该交付物槽位已存在文件，请先删除后再导入");
    }
    String storagePath = fileStorageService.storeFile(file, project.getProjectCode(), stage, currentUserId);
    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(file.getOriginalFilename());
    doc.setStoragePath(storagePath);
    doc.setFileType(fileType == null || fileType.isBlank() ? def.getSlotName() : fileType);
    doc.setProjectId(projectId);
    doc.setMilestonePhase(stage);
    doc.setDeliverableSlotCode(slotCode);
    doc.setUploader(currentUserId);
    // 管理员/项目管理员导入历史交付物是不经过评审的补录动作，导入即视为已确认，避免页面显示“待审核”。
    doc.setComplianceStatus(Enums.ComplianceStatus.APPROVED);
    doc.setIsLocked(true);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setCreatedAt(Instant.now());
    DocumentEntity saved = documentRepository.save(doc);
    synchronizeMilestoneProgressAfterImport(project, stage);
    auditLogService.logAction(currentUserId, "IMPORT_DELIVERABLE", saved.getId(),
        "Imported historical deliverable into " + milestoneCode + "/" + slotCode);
    return saved;
  }

  /**
   * 管理员直传历史交付物后，同步项目里程碑状态：
   * - 连续已填满的阶段视为 APPROVED；
   * - 首个未填满且不早于本次上传阶段的阶段置为 IN_PROGRESS；
   * - 当前里程碑指向该 IN_PROGRESS 阶段，里程碑控制台从此阶段继续。
   */
  private void synchronizeMilestoneProgressAfterImport(ProjectEntity project, Enums.MilestoneStage importedStage) {
    List<MilestoneDefEntity> milestones = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    Map<String, MilestoneDefEntity> milestoneByCode = milestones.stream()
        .collect(Collectors.toMap(MilestoneDefEntity::getMilestoneCode, m -> m));
    List<ProjectMilestoneEntity> projectMilestones = projectMilestoneRepository.findByProjectIdOrderByIdAsc(project.getId());
    Map<Long, ProjectMilestoneEntity> pmByMilestoneId = projectMilestones.stream()
        .collect(Collectors.toMap(ProjectMilestoneEntity::getMilestoneId, pm -> pm));
    Instant now = Instant.now();
    int currentIndex = isMilestoneFilled(project.getId(), importedStage)
        ? Math.min(importedStage.ordinal() + 1, Enums.MilestoneStage.G9.ordinal())
        : importedStage.ordinal();
    Enums.MilestoneStage currentStage = Enums.MilestoneStage.values()[currentIndex];
    for (Enums.MilestoneStage stage : Enums.MilestoneStage.values()) {
      MilestoneDefEntity milestone = milestoneByCode.get(stage.name());
      if (milestone == null) continue;
      ProjectMilestoneEntity pm = pmByMilestoneId.get(milestone.getId());
      if (pm == null) continue;
      if (stage.ordinal() < currentStage.ordinal()) {
        pm.setStatus(Enums.ProjectMilestoneStatus.APPROVED);
        pm.setDecisionResult(Enums.MilestoneDecisionResult.GO);
        if (pm.getDecisionAt() == null) pm.setDecisionAt(LocalDateTime.now());
      } else if (stage == currentStage) {
        pm.setStatus(Enums.ProjectMilestoneStatus.IN_PROGRESS);
        pm.setDecisionResult(null);
      } else if (pm.getStatus() != Enums.ProjectMilestoneStatus.APPROVED
          && pm.getStatus() != Enums.ProjectMilestoneStatus.CONDITIONAL_APPROVED) {
        pm.setStatus(Enums.ProjectMilestoneStatus.NOT_STARTED);
        pm.setDecisionResult(null);
      }
      pm.setUpdatedAt(now);
      projectMilestoneRepository.save(pm);
    }
    MilestoneDefEntity currentMilestone = milestoneByCode.get(currentStage.name());
    if (currentMilestone != null) {
      project.setCurrentMilestoneId(currentMilestone.getId());
      project.setStatus(Enums.ProjectStatus.ACTIVE);
      project.setReviewStatus(null);
      project.setUpdatedAt(now);
      projectRepository.save(project);
      notificationService.completeTodoByProjectAndMilestone(project.getId(), currentStage.name(), "DELIVERABLE");
      notifyExecutorsForMilestone(project, currentMilestone);
    }
  }

  private boolean isMilestoneFilled(Long projectId, Enums.MilestoneStage stage) {
    List<MilestoneDeliverableDefEntity> defs = deliverableDefRepository
        .findByMilestoneCodeAndIsActiveTrueOrderBySortNoAsc(stage.name());
    if (defs.isEmpty()) return false;
    return defs.stream().allMatch(def -> !documentRepository
        .findByProjectIdAndMilestonePhaseAndDeliverableSlotCode(projectId, stage, def.getSlotCode()).isEmpty());
  }

  private void notifyExecutorsForMilestone(ProjectEntity project, MilestoneDefEntity milestone) {
    try {
      for (Long deptId : getExecutorDeptIdsByMilestoneCode(milestone.getMilestoneCode())) {
        notificationService.sendNotificationToDeptExecutors(deptId, "DELIVERABLE",
            "请上传" + milestone.getMilestoneCode() + "阶段交付物",
            "项目 [" + project.getProjectName() + "] 已进入 " + milestone.getMilestoneCode() + "-" + milestone.getMilestoneName()
                + "，请上传本阶段交付物并提交评审。",
            project.getId(), milestone.getMilestoneCode(), null, true);
      }
    } catch (Exception ignored) {
      // 通知失败不影响历史交付物导入主流程
    }
  }

  /**
   * 删除交付物
   */
  @Transactional
  public void deleteDeliverable(Long documentId) throws IOException {
    long currentUserId = securityHelper.getCurrentUserId();

    DocumentEntity doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));

    if (doc.getIsLocked()) {
      throw new ApiException(409, "文档已锁定，无法删除");
    }

    // 权限：上传者本人 / PM / 管理员 可删除
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));
    List<String> roles = currentUser.getRoles().stream().map(Role::getName).toList();

    ProjectEntity project = projectRepository.findById(doc.getProjectId())
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    // 权限：ADMIN / 项目PM / 上传者本人 / 执行部门的部门执行人 可删除
    boolean canDelete = roles.contains("ROLE_ADMIN")
        || (roles.contains("ROLE_PM") && project.getPmUserId() != null
            && project.getPmUserId().equals(currentUserId))
        || doc.getUploader().equals(currentUserId);

    if (!canDelete && roles.contains("ROLE_DEPT_EXECUTOR")) {
      // 检查用户所在部门是否在里程碑的执行部门列表中
      String milestoneCode = doc.getMilestonePhase().name();
      List<Long> executorDeptIds = getExecutorDeptIdsByMilestoneCode(milestoneCode);
      if (!executorDeptIds.isEmpty()) {
        Set<OrgDepartmentEntity> userDepts = currentUser.getDepartments();
        if (userDepts != null) {
          for (OrgDepartmentEntity userDept : userDepts) {
            if (executorDeptIds.contains(userDept.getId())) {
              canDelete = true;
              break;
            }
          }
        }
      }
    }

    if (!canDelete) {
      throw new ApiException(403, "无权限删除该交付物");
    }

    // 删除物理文件
    fileStorageService.deleteFile(doc.getStoragePath(), currentUserId);

    // 删除数据库记录
    documentRepository.delete(doc);

    auditLogService.logAction(currentUserId, "DELETE_DELIVERABLE", documentId,
        "Deliverable deleted: " + doc.getFileName());
  }

  /**
   * 根据ID获取文档（用于下载）
   */
  @Transactional(readOnly = true)
  public DocumentEntity getDocumentById(Long documentId) {
    DocumentEntity document = documentRepository.findById(documentId)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));
    assertCanView(document);
    return document;
  }

  /** 获取项目详情中的全部交付物，固定按 G0-G9、槽位、上传时间排序。 */
  @Transactional(readOnly = true)
  public List<DocumentEntity> getVisibleDocuments(Long projectId) {
    ProjectEntity project = projectRepository.findById(projectId).orElseThrow(() -> new ApiException(404, "项目不存在"));
    long userId = securityHelper.getCurrentUserId();
    User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(401, "用户不存在"));
    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
    if (hasGlobalDeliverableAccess(user, roles) || isProjectRelatedUser(project, userId)) {
      return documentRepository.findByProjectIdOrderByMilestonePhaseAscDeliverableSlotCodeAscUploadedAtAsc(projectId);
    }

    List<ReviewApprovalEntity> approvals = reviewApprovalRepository.findByProjectIdAndStatusIn(
        projectId, List.of(ReviewApprovalEntity.Status.SUBMITTED, ReviewApprovalEntity.Status.APPROVED,
            ReviewApprovalEntity.Status.REJECTED));
    List<Long> approvalIds = approvals.stream().map(ReviewApprovalEntity::getId).toList();
    Set<String> reviewedSlots = approvalIds.isEmpty() ? Set.of() : reviewApprovalTaskRepository
        .findByReviewApprovalIdInAndApproverUserIdAndStatusIn(
            approvalIds, userId, List.of(ReviewApprovalTaskEntity.Status.APPROVED,
                ReviewApprovalTaskEntity.Status.REJECTED))
        .stream().map(ReviewApprovalTaskEntity::getDeliverableSlotCode)
        .filter(Objects::nonNull).collect(Collectors.toSet());
    return documentRepository.findByProjectIdOrderByMilestonePhaseAscDeliverableSlotCodeAscUploadedAtAsc(projectId)
        .stream().filter(doc -> Objects.equals(userId, doc.getUploader())
            || (doc.getDeliverableSlotCode() != null && reviewedSlots.contains(doc.getDeliverableSlotCode())))
        .toList();
  }

  /** 项目关联用户（项目经理、项目成员/执行人、所有里程碑评审人/曾评审人、发起人）可查看项目交付物。 */
  private boolean isProjectRelatedUser(ProjectEntity project, Long userId) {
    if (userId == null) return false;
    if (Objects.equals(project.getPmUserId(), userId) || Objects.equals(project.getInitiatorUserId(), userId)) {
      return true;
    }
    if (projectTeamMemberRepository.isActiveMember(project.getId(), userId, LocalDate.now())) {
      return true;
    }
    List<ReviewApprovalEntity> approvals = reviewApprovalRepository.findByProjectIdAndStatusIn(
        project.getId(), List.of(ReviewApprovalEntity.Status.SUBMITTED, ReviewApprovalEntity.Status.APPROVED,
            ReviewApprovalEntity.Status.REJECTED));
    List<Long> approvalIds = approvals.stream().map(ReviewApprovalEntity::getId).toList();
    if (!approvalIds.isEmpty() && !reviewApprovalTaskRepository
        .findByReviewApprovalIdInAndApproverUserIdAndStatusIn(
            approvalIds, userId, List.of(ReviewApprovalTaskEntity.Status.PENDING,
                ReviewApprovalTaskEntity.Status.APPROVED, ReviewApprovalTaskEntity.Status.REJECTED))
        .isEmpty()) {
      return true;
    }
    return false;
  }

  /** 下载、预览和列表使用同一套访问控制，避免只保护页面不保护文件接口。 */
  @Transactional(readOnly = true)
  public void assertCanView(DocumentEntity document) {
    if (getVisibleDocuments(document.getProjectId()).stream()
        .noneMatch(candidate -> candidate.getId().equals(document.getId()))) {
      throw new ApiException(403, "无权访问该交付物");
    }
  }

  private boolean hasGlobalDeliverableAccess(User user, List<String> roles) {
    Set<String> permissions = user.getRoles().stream()
        .filter(Objects::nonNull)
        .flatMap(role -> role.getPermissions() == null ? java.util.stream.Stream.empty() : role.getPermissions().stream())
        .map(Permission::getName)
        .collect(Collectors.toSet());
    if (permissions.contains("PERMISSION_DELIVERABLE_VIEW_ALL")) return true;
    // 兼容迁移前的角色配置，避免已具备全量交付物管理能力的管理员短暂失去访问。
    if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_PROJECT_ADMIN") || roles.contains("ROLE_PMC")) return true;
    return user.getDepartments() != null && user.getDepartments().stream()
        .anyMatch(dept -> dept.getDeptName() != null &&
            (dept.getDeptName().contains("效率管理") || dept.getDeptName().contains("效率管理部")));
  }

  private boolean hasDeliverablePermission(User user, String permissionName) {
    return user.getRoles().stream()
        .filter(Objects::nonNull)
        .flatMap(role -> role.getPermissions() == null ? java.util.stream.Stream.empty() : role.getPermissions().stream())
        .map(Permission::getName)
        .anyMatch(permissionName::equals);
  }

  /**
   * 根据里程碑代码获取执行部门ID列表（从流程引擎上传节点配置中获取）
   */
  private List<Long> getExecutorDeptIdsByMilestoneCode(String milestoneCode) {
    WfProcessDefinition def = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
        .orElse(null);
    if (def == null) {
      return Collections.emptyList();
    }
    List<Long> deptIds = new ArrayList<>();
    for (WfProcessNode node : def.getNodes()) {
      if (Boolean.TRUE.equals(node.getIsUploader()) && node.getApproverValue() != null) {
        Arrays.stream(node.getApproverValue().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(deptIdStr -> {
              try {
                deptIds.add(Long.parseLong(deptIdStr));
              } catch (NumberFormatException e) {
                // 忽略无效的部门ID
              }
            });
      }
    }
    return deptIds;
  }

  /**
   * 根据里程碑代码获取执行部门名称列表（从流程引擎上传节点配置中获取）
   */
  private String getExecutorDeptNamesByMilestoneCode(String milestoneCode) {
    WfProcessDefinition def = wfProcessRepository
        .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
        .orElse(null);
    if (def == null) {
      return "";
    }
    List<String> deptNames = new ArrayList<>();
    for (WfProcessNode node : def.getNodes()) {
      if (Boolean.TRUE.equals(node.getIsUploader()) && node.getApproverValue() != null) {
        Arrays.stream(node.getApproverValue().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .forEach(deptIdStr -> {
              try {
                Long deptId = Long.parseLong(deptIdStr);
                orgDepartmentRepository.findById(deptId)
                    .map(OrgDepartmentEntity::getDeptName)
                    .ifPresent(deptNames::add);
              } catch (NumberFormatException e) {
                // 忽略无效的部门ID
              }
            });
      }
    }
    return String.join("、", deptNames);
  }

  /**
   * 交付物槽位 VO
   */
  public static class DeliverableSlotVO {
    private String slotCode;
    private String slotName;
    private Boolean isRequired;
    private String description;
    private String allowedFileTypes;
    private List<DocumentEntity> documents;

    public String getSlotCode() { return slotCode; }
    public void setSlotCode(String slotCode) { this.slotCode = slotCode; }

    public String getSlotName() { return slotName; }
    public void setSlotName(String slotName) { this.slotName = slotName; }

    public Boolean getIsRequired() { return isRequired; }
    public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAllowedFileTypes() { return allowedFileTypes; }
    public void setAllowedFileTypes(String allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }

    public List<DocumentEntity> getDocuments() { return documents; }
    public void setDocuments(List<DocumentEntity> documents) { this.documents = documents; }
  }

  public static class MilestoneDeliverableGroupVO {
    private String milestoneCode;
    private String milestoneName;
    private String phaseLabel;
    private Integer uploadedCount;
    private Integer totalCount;
    private List<DeliverableSlotVO> slots;

    public String getMilestoneCode() { return milestoneCode; }
    public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }
    public String getMilestoneName() { return milestoneName; }
    public void setMilestoneName(String milestoneName) { this.milestoneName = milestoneName; }
    public String getPhaseLabel() { return phaseLabel; }
    public void setPhaseLabel(String phaseLabel) { this.phaseLabel = phaseLabel; }
    public Integer getUploadedCount() { return uploadedCount; }
    public void setUploadedCount(Integer uploadedCount) { this.uploadedCount = uploadedCount; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public List<DeliverableSlotVO> getSlots() { return slots; }
    public void setSlots(List<DeliverableSlotVO> slots) { this.slots = slots; }
  }
}