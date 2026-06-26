package com.kbd.pms.service;

import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

  private final DocumentRepository documentRepository;
  private final FileStorageService fileStorageService;
  private final AuditLogService auditLogService;
  private final SecurityHelper securityHelper;
  private final UserRepository userRepository;
  private final ProjectRepository projectRepository;
  private final IamUserRepository iamUserRepository;
  private final OrgDepartmentRepository orgDepartmentRepository;
  private final GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository;

  public DocumentService(
      DocumentRepository documentRepository,
      FileStorageService fileStorageService,
      AuditLogService auditLogService,
      SecurityHelper securityHelper,
      UserRepository userRepository,
      ProjectRepository projectRepository,
      IamUserRepository iamUserRepository,
      OrgDepartmentRepository orgDepartmentRepository,
      GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository) {
    this.documentRepository = documentRepository;
    this.fileStorageService = fileStorageService;
    this.auditLogService = auditLogService;
    this.securityHelper = securityHelper;
    this.userRepository = userRepository;
    this.projectRepository = projectRepository;
    this.iamUserRepository = iamUserRepository;
    this.orgDepartmentRepository = orgDepartmentRepository;
    this.governanceCommitteeMemberRepository = governanceCommitteeMemberRepository;
  }

  @Transactional
  public DocumentEntity uploadDocument(MultipartFile file, Long projectId, String projectCode,
      Enums.MilestoneStage milestonePhase, String fileType, Long uploaderId) throws IOException {
    List<DocumentEntity> existing = documentRepository.findByProjectIdAndMilestonePhase(projectId, milestonePhase)
        .stream()
        .filter(d -> fileType.equals(d.getFileType()))
        .toList();
    if (!existing.isEmpty()) {
      throw new ApiException(409, "同一里程碑阶段下同类型的交付物已存在");
    }

    String storagePath = fileStorageService.storeFile(file, projectCode, milestonePhase, uploaderId);

    DocumentEntity doc = new DocumentEntity();
    doc.setFileName(file.getOriginalFilename());
    doc.setStoragePath(storagePath);
    doc.setFileType(fileType);
    doc.setProjectId(projectId);
    doc.setMilestonePhase(milestonePhase);
    doc.setUploader(uploaderId);
    doc.setComplianceStatus(Enums.ComplianceStatus.PENDING);
    doc.setIsLocked(false);
    doc.setUploadedAt(LocalDateTime.now());
    doc.setCreatedAt(Instant.now());

    DocumentEntity saved = documentRepository.save(doc);
    auditLogService.logAction(uploaderId, "UPLOAD", saved.getId(), "Document uploaded: " + file.getOriginalFilename());
    return saved;
  }

  /**
   * 获取项目文档（带角色权限过滤）
   * 
   * 权限规则：
   * - ROLE_ADMIN / ROLE_PM（该项目的PM）/ ROLE_PMC：查看项目所有文档
   * - ROLE_DEPT_HEAD（部门负责人）：查看项目所有文档
   * - ROLE_DEPT_EXECUTOR（部门执行人）：仅查看自己上传的文档
   * - 其他人：无法查看
   */
  @Transactional(readOnly = true)
  public List<DocumentEntity> getDocumentsByProject(Long projectId) {
    long currentUserId = securityHelper.getCurrentUserId();
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));

    ProjectEntity project = projectRepository.findById(projectId)
        .orElseThrow(() -> new ApiException(404, "项目不存在"));

    List<String> roles = currentUser.getRoles().stream()
        .map(Role::getName).toList();

    // Admin: 全量访问
    if (roles.contains("ROLE_ADMIN")) {
      return documentRepository.findByProjectId(projectId);
    }

    // 该项目的PM: 全量访问
    if (roles.contains("ROLE_PM") && project.getPmUserId() != null
        && project.getPmUserId().equals(currentUserId)) {
      return documentRepository.findByProjectId(projectId);
    }

    // PMC成员: 全量访问
    if (roles.contains("ROLE_PMC") && project.getPmcCommitteeId() != null) {
      boolean isPmcMember = governanceCommitteeMemberRepository
          .isActiveCommitteeMember(project.getPmcCommitteeId(), currentUserId,
              java.time.LocalDate.now(java.time.ZoneOffset.UTC));
      if (isPmcMember) {
        return documentRepository.findByProjectId(projectId);
      }
    }

    // 部门负责人: 全量访问
    IamUserEntity iamUser = iamUserRepository.findById(currentUserId).orElse(null);
    if (iamUser != null && iamUser.getDeptId() != null) {
      OrgDepartmentEntity dept = orgDepartmentRepository.findById(iamUser.getDeptId()).orElse(null);
      if (dept != null && dept.getHeadUserId() != null
          && dept.getHeadUserId().equals(currentUserId)) {
        return documentRepository.findByProjectId(projectId);
      }
    }

    // 部门执行人: 仅查看自己上传的文档
    if (roles.contains("ROLE_DEPT_EXECUTOR")) {
      return documentRepository.findByProjectIdAndUploader(projectId, currentUserId);
    }

    // 其他人：无权查看
    return List.of();
  }

  /**
   * 获取项目指定阶段的文档（带权限过滤）
   */
  @Transactional(readOnly = true)
  public List<DocumentEntity> getDocumentsByProjectAndPhase(Long projectId, Enums.MilestoneStage milestonePhase) {
    List<DocumentEntity> allDocs = getDocumentsByProject(projectId);
    return allDocs.stream()
        .filter(d -> d.getMilestonePhase() == milestonePhase)
        .collect(Collectors.toList());
  }

  /**
   * 获取待合规审核的文档列表（仅合规部可查看）
   */
  @Transactional(readOnly = true)
  public List<DocumentEntity> getPendingReviews() {
    long currentUserId = securityHelper.getCurrentUserId();
    User currentUser = userRepository.findById(currentUserId)
        .orElseThrow(() -> new ApiException(401, "用户不存在"));

    List<String> roles = currentUser.getRoles().stream().map(Role::getName).toList();
    if (!roles.contains("ROLE_COMPLIANCE") && !roles.contains("ROLE_ADMIN")) {
      throw new ApiException(403, "只有药政合规部或管理员才能查看待审核文档");
    }

    return documentRepository.findByComplianceStatus(Enums.ComplianceStatus.PENDING);
  }

  @Transactional
  public void reviewDocument(long documentId, Enums.ComplianceStatus status, long reviewerId) {
    DocumentEntity doc = documentRepository.findById(documentId)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));

    if (doc.getIsLocked()) {
      throw new ApiException(409, "文档已锁定，无法审核");
    }

    doc.setComplianceStatus(status);
    documentRepository.save(doc);

    auditLogService.logAction(reviewerId, "REVIEW", documentId, "Compliance status set to: " + status);
  }

  public DocumentEntity getDocumentById(long id) {
    return documentRepository.findById(id)
        .orElseThrow(() -> new ApiException(404, "文档不存在"));
  }
}