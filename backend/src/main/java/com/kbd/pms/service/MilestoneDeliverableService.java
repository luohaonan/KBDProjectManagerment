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
      WfProcessRepository wfProcessRepository) {
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
    List<DocumentEntity> uploadedDocs = documentRepository.findByProjectIdAndMilestonePhase(projectId, stage);

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

    // 权限校验：PM / 部门负责人 / 部门执行人 可上传
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));
    List<String> roles = currentUser.getRoles().stream().map(Role::getName).toList();

    // 权限校验：ADMIN / 项目PM 可上传所有里程碑交付物；
    // 部门负责人/执行人需要属于当前里程碑的执行部门
    boolean canUpload = roles.contains("ROLE_ADMIN")
        || (roles.contains("ROLE_PM") && project.getPmUserId() != null
            && project.getPmUserId().equals(currentUserId));

    if (!canUpload && (roles.contains("ROLE_DEPT_HEAD") || roles.contains("ROLE_DEPT_EXECUTOR"))) {
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
        request.projectId(), request.milestoneCode(), "PROJECT_COMPLETION");
    auditLogService.logAction(currentUserId, "UPLOAD_DELIVERABLE", saved.getId(),
        "Milestone " + request.milestoneCode() + " deliverable uploaded: " + def.getSlotName());
    return saved;
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

    // 权限：ADMIN / 项目PM / 上传者本人 / 执行部门的部门负责人/执行人 可删除
    boolean canDelete = roles.contains("ROLE_ADMIN")
        || (roles.contains("ROLE_PM") && project.getPmUserId() != null
            && project.getPmUserId().equals(currentUserId))
        || doc.getUploader().equals(currentUserId);

    if (!canDelete && (roles.contains("ROLE_DEPT_HEAD") || roles.contains("ROLE_DEPT_EXECUTOR"))) {
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
    return documentRepository.findById(documentId)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));
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
}