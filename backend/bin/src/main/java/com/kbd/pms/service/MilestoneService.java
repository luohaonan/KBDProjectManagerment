package com.kbd.pms.service;

import com.kbd.pms.dto.MilestoneApproveRequest;
import com.kbd.pms.dto.MilestoneResponse;
import com.kbd.pms.dto.MilestoneRescheduleRequest;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.entity.MilestoneHistoryEntity;
import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;
import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.GovernanceCommitteeMemberRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import com.kbd.pms.repository.MilestoneHistoryRepository;
import com.kbd.pms.repository.DocumentRepository;
import com.kbd.pms.repository.ProjectMilestoneRepository;
import com.kbd.pms.repository.ProjectDocumentRepository;
import com.kbd.pms.repository.ProjectRepository;
import com.kbd.pms.repository.ProjectTeamMemberRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kbd.pms.event.MilestoneGoEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("null")
public class MilestoneService {

  private static final Map<String, List<String>> REQUIRED_DELIVERABLES_BY_STAGE =
      Map.of(
          // 示例：制度表里“核心交付物”字段 + 评审附件清单（附件三）可继续扩展
          "G0", List.of("PROJECT_INITIATION_REPORT"),
          "G3", List.of("PCC_NOMINATION_REPORT"),
          "G4", List.of("GLP_TOX_REPORT", "EFFICACY_SUMMARY_REPORT", "CMC_PRELIM_REPORT", "FTO_REPORT"),
          "G5", List.of("IND_DOSSIER", "ACCEPTANCE_NOTICE", "IND_APPROVAL_FILE"),
          "G9", List.of("NDA_DOSSIER", "REG_CERTIFICATE"));

  private final ProjectRepository projectRepository;
  private final ProjectMilestoneRepository projectMilestoneRepository;
  private final MilestoneDefRepository milestoneDefRepository;
  private final ProjectDocumentRepository projectDocumentRepository;
  private final DocumentRepository documentRepository;
  private final ProjectTeamMemberRepository projectTeamMemberRepository;
  private final GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository;
  private final MilestoneHistoryRepository milestoneHistoryRepository;
  private final ApplicationEventPublisher eventPublisher;

  public MilestoneService(
      ProjectRepository projectRepository,
      ProjectMilestoneRepository projectMilestoneRepository,
      MilestoneDefRepository milestoneDefRepository,
      ProjectDocumentRepository projectDocumentRepository,
      DocumentRepository documentRepository,
      ProjectTeamMemberRepository projectTeamMemberRepository,
      GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository,
      MilestoneHistoryRepository milestoneHistoryRepository,
      ApplicationEventPublisher eventPublisher) {
    this.projectRepository = projectRepository;
    this.projectMilestoneRepository = projectMilestoneRepository;
    this.milestoneDefRepository = milestoneDefRepository;
    this.projectDocumentRepository = projectDocumentRepository;
    this.documentRepository = documentRepository;
    this.projectTeamMemberRepository = projectTeamMemberRepository;
    this.governanceCommitteeMemberRepository = governanceCommitteeMemberRepository;
    this.milestoneHistoryRepository = milestoneHistoryRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * 提交评审申请（制度：里程碑申请由 PM 发起；提交前需确认核心交付物已上传）。
   */
  @Transactional
  @PreAuthorize("hasRole('ROLE_PM')")
  public void submitReview(long projectMilestoneId, long actorUserId) {
    ProjectMilestoneEntity pm =
        projectMilestoneRepository
            .findById(projectMilestoneId)
            .orElseThrow(() -> new ApiException(404, "里程碑记录不存在: id=" + projectMilestoneId));

    Long pid = pm.getProjectId();
    ProjectEntity project =
        projectRepository
            .findById(pid)
            .orElseThrow(() -> new ApiException(500, "项目数据缺失: id=" + pid));

    ensureActorIsPm(project, actorUserId);

    // 只能对“当前里程碑”发起评审（保持连续性）
    if (!Objects.equals(project.getCurrentMilestoneId(), pm.getMilestoneId())) {
      throw new ApiException(409, "只能对当前里程碑发起评审申请");
    }

    if (pm.getStatus() != Enums.ProjectMilestoneStatus.IN_PROGRESS) {
      throw new ApiException(409, "当前里程碑状态不允许提交评审: " + pm.getStatus());
    }

    Long mid = pm.getMilestoneId();
    MilestoneDefEntity def =
        milestoneDefRepository
            .findById(mid)
            .orElseThrow(() -> new ApiException(500, "里程碑字典缺失: milestone_id=" + mid));

    assertCoreDeliverablesUploaded(project.getId(), def);

    var from = pm.getStatus().name();
    pm.setStatus(Enums.ProjectMilestoneStatus.SUBMITTED);
    pm.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(pm);

    writeHistory(
        project.getId(),
        pm.getId(),
        MilestoneHistoryEntity.Action.SUBMIT_REVIEW,
        actorUserId,
        from,
        pm.getStatus().name(),
        "提交评审申请",
        "{\"milestoneCode\":\"" + def.getMilestoneCode() + "\"}");
  }

  @Transactional
  @PreAuthorize("hasRole('ROLE_PM')")
  public void rescheduleMilestone(long projectId, long milestoneId, MilestoneRescheduleRequest request) {
    ProjectMilestoneEntity milestone = projectMilestoneRepository
        .findById(milestoneId)
        .orElseThrow(() -> new ApiException(404, "里程碑记录不存在: id=" + milestoneId));

    if (!milestone.getProjectId().equals(projectId)) {
      throw new ApiException(409, "里程碑不属于指定项目");
    }

    if (milestone.getPlannedDate() != null) {
      long delayMonths = milestone.getPlannedDate().until(request.newPlannedDate()).toTotalMonths();
      if (delayMonths > 3) {
        throw new ApiException(409, "延期超过3个月的里程碑变更必须通过变更申请来处理。");
      }
    }

    milestone.setPlannedDate(request.newPlannedDate());
    milestone.setUpdatedAt(Instant.now());
    projectMilestoneRepository.save(milestone);
  }

  /**
   * 检查该阶段所有核心交付物是否已通过药政合规部的审核。
   */
  private void checkComplianceForGo(long projectId, MilestoneDefEntity def) {
    List<String> requiredDeliverables = REQUIRED_DELIVERABLES_BY_STAGE.get(def.getMilestoneCode());
    if (requiredDeliverables == null || requiredDeliverables.isEmpty()) {
      return; // 无需检查
    }

    Enums.MilestoneStage stage = Enums.MilestoneStage.valueOf(def.getMilestoneCode());

    for (String deliverable : requiredDeliverables) {
      List<DocumentEntity> docs = documentRepository.findByProjectIdAndMilestonePhase(projectId, stage)
          .stream()
          .filter(d -> deliverable.equals(d.getFileType()) && d.getComplianceStatus() != Enums.ComplianceStatus.APPROVED)
          .toList();
      if (!docs.isEmpty()) {
        throw new ApiException(409, "核心交付物 '" + deliverable + "' 未通过合规审核，无法执行 GO 决策");
      }
    }
  }
  @Transactional
  @PreAuthorize("hasRole('ROLE_PMC')")
  public void executeDecision(long projectMilestoneId, MilestoneApproveRequest req) {
    ProjectMilestoneEntity pm =
        projectMilestoneRepository
            .findById(projectMilestoneId)
            .orElseThrow(() -> new ApiException(404, "里程碑记录不存在: id=" + projectMilestoneId));

    Long pid = pm.getProjectId();
    ProjectEntity project =
        projectRepository
            .findById(pid)
            .orElseThrow(() -> new ApiException(500, "项目数据缺失: id=" + pid));

    ensureActorIsPmcMember(project, req.actorUserId());

    if (pm.getStatus() != Enums.ProjectMilestoneStatus.SUBMITTED) {
      throw new ApiException(409, "仅允许对 SUBMITTED 状态的里程碑执行决策");
    }

    Long mid = pm.getMilestoneId();
    MilestoneDefEntity currentDef =
        milestoneDefRepository
            .findById(mid)
            .orElseThrow(() -> new ApiException(500, "里程碑字典缺失: milestone_id=" + mid));

    String from = pm.getStatus().name();
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    Instant nowInstant = Instant.now();

    // Java 21：switch + 枚举分支（可继续扩展成 pattern matching）
    switch (req.decision()) {
      case GO -> {
        // 合规性前置校验
        checkComplianceForGo(project.getId(), currentDef);

        pm.setStatus(Enums.ProjectMilestoneStatus.APPROVED);
        pm.setDecisionResult(Enums.MilestoneDecisionResult.GO);
        pm.setConditionalDeadline(null);
        pm.setDecisionNotes(req.reviewNotes());
        pm.setDecisionAt(now);
        pm.setDecidedBy(req.actorUserId());
        pm.setUpdatedAt(nowInstant);
        projectMilestoneRepository.save(pm);

        // 打开下一个里程碑
        MilestoneDefEntity nextDef = findNextMilestone(currentDef);
        if (nextDef != null) {
          ProjectMilestoneEntity next =
              projectMilestoneRepository.findByProjectIdAndMilestoneId(project.getId(), nextDef.getId());
          if (next.getStatus() == Enums.ProjectMilestoneStatus.NOT_STARTED) {
            next.setStatus(Enums.ProjectMilestoneStatus.IN_PROGRESS);
            next.setUpdatedAt(nowInstant);
            projectMilestoneRepository.save(next);
          }
          project.setCurrentMilestoneId(nextDef.getId());
        }
        project.setUpdatedAt(nowInstant);
        projectRepository.save(project);

        // 发布归档锁定事件
        eventPublisher.publishEvent(new MilestoneGoEvent(project, pm));

        writeHistory(
            project.getId(),
            pm.getId(),
            MilestoneHistoryEntity.Action.DECISION,
            req.actorUserId(),
            from,
            pm.getStatus().name(),
            req.reviewNotes(),
            "{\"decision\":\"GO\",\"milestoneCode\":\"" + currentDef.getMilestoneCode() + "\"}");
      }
      case CONDITIONAL_GO -> {
        pm.setStatus(Enums.ProjectMilestoneStatus.CONDITIONAL_APPROVED);
        pm.setDecisionResult(Enums.MilestoneDecisionResult.CONDITIONAL_GO);
        pm.setConditionalDeadline(now.plusMonths(3));
        pm.setDecisionNotes(
            (req.reviewNotes() == null ? "" : req.reviewNotes())
                + (req.conditionalNotes() == null ? "" : ("\n条件：" + req.conditionalNotes())));
        pm.setDecisionAt(now);
        pm.setDecidedBy(req.actorUserId());
        pm.setUpdatedAt(nowInstant);
        projectMilestoneRepository.save(pm);

        project.setUpdatedAt(nowInstant);
        projectRepository.save(project);

        writeHistory(
            project.getId(),
            pm.getId(),
            MilestoneHistoryEntity.Action.DECISION,
            req.actorUserId(),
            from,
            pm.getStatus().name(),
            pm.getDecisionNotes(),
            "{\"decision\":\"CONDITIONAL_GO\",\"deadline\":\"" + pm.getConditionalDeadline() + "\"}");
      }
      case NO_GO -> {
        pm.setStatus(Enums.ProjectMilestoneStatus.REJECTED);
        pm.setDecisionResult(Enums.MilestoneDecisionResult.NO_GO);
        pm.setConditionalDeadline(null);
        pm.setDecisionNotes(req.reviewNotes());
        pm.setDecisionAt(now);
        pm.setDecidedBy(req.actorUserId());
        pm.setUpdatedAt(nowInstant);
        projectMilestoneRepository.save(pm);

        project.setStatus(Enums.ProjectStatus.TERMINATED);
        project.setTerminatedReason(
            req.terminationReason() != null && !req.terminationReason().isBlank()
                ? req.terminationReason()
                : (req.reviewNotes() == null ? "No-Go" : req.reviewNotes()));
        project.setUpdatedAt(nowInstant);
        projectRepository.save(project);

        writeHistory(
            project.getId(),
            pm.getId(),
            MilestoneHistoryEntity.Action.DECISION,
            req.actorUserId(),
            from,
            pm.getStatus().name(),
            project.getTerminatedReason(),
            "{\"decision\":\"NO_GO\",\"reason\":\"" + escapeJson(project.getTerminatedReason()) + "\"}");
      }
    }
  }

  /**
   * Conditional Go 超期预警：观察期（<=3个月）截止仍未转为 GO 时返回待告警清单。
   */
  @Transactional(readOnly = true)
  public List<ProjectMilestoneEntity> checkExpiredConditionalGo() {
    return projectMilestoneRepository.findExpiredConditionalApproved(LocalDateTime.now(ZoneOffset.UTC));
  }

  private void ensureActorIsPm(ProjectEntity project, long actorUserId) {
    if (project.getPmUserId() != null && project.getPmUserId() == actorUserId) {
      return;
    }
    boolean ok =
        projectTeamMemberRepository.isActiveMemberWithRole(
            project.getId(), actorUserId, Enums.ProjectTeamRole.PM, LocalDate.now(ZoneOffset.UTC));
    if (!ok) {
      throw new ApiException(403, "仅允许项目经理（PM）发起里程碑评审申请");
    }
  }

  private void ensureActorIsPmcMember(ProjectEntity project, long actorUserId) {
    if (project.getPmcCommitteeId() == null) {
      throw new ApiException(409, "项目未配置 PMC 委员会，无法执行决策");
    }
    boolean ok =
        governanceCommitteeMemberRepository.isActiveCommitteeMember(
            project.getPmcCommitteeId(), actorUserId, LocalDate.now(ZoneOffset.UTC));
    if (!ok) {
      throw new ApiException(403, "仅允许 PMC 成员执行里程碑决策审批");
    }
  }

  private void assertCoreDeliverablesUploaded(long projectId, MilestoneDefEntity def) {
    List<String> required = REQUIRED_DELIVERABLES_BY_STAGE.get(def.getMilestoneCode());
    if (required == null || required.isEmpty()) {
      return; // 未配置则不拦截（可按附件三继续补齐）
    }
    for (String docType : required) {
      boolean exists =
          projectDocumentRepository.existsByProjectIdAndMilestoneIdAndDocType(
              projectId, def.getId(), docType);
      if (!exists) {
        throw new ApiException(
            409,
            "缺少核心交付物，无法提交评审。阶段="
                + def.getMilestoneCode()
                + "，必需交付物类型="
                + docType);
      }
    }
  }

  private MilestoneDefEntity findNextMilestone(MilestoneDefEntity current) {
    Integer sort = current.getSortNo();
    if (sort == null) return null;
    // 字典表 sort_no = 0..9
    List<MilestoneDefEntity> defs = milestoneDefRepository.findAllByIsActiveTrueOrderBySortNoAsc();
    for (MilestoneDefEntity d : defs) {
      if (Objects.equals(d.getSortNo(), sort + 1)) {
        return d;
      }
    }
    return null;
  }

  private void writeHistory(
      long projectId,
      long projectMilestoneId,
      MilestoneHistoryEntity.Action action,
      long actorUserId,
      String fromStatus,
      String toStatus,
      String notes,
      String payloadJson) {
    MilestoneHistoryEntity h = new MilestoneHistoryEntity();
    h.setProjectId(projectId);
    h.setProjectMilestoneId(projectMilestoneId);
    h.setAction(action);
    h.setActorUserId(actorUserId);
    h.setFromStatus(fromStatus);
    h.setToStatus(toStatus);
    h.setNotes(notes);
    h.setPayloadJson(payloadJson);
    h.setActionAt(LocalDateTime.now(ZoneOffset.UTC));
    h.setCreatedAt(Instant.now());
    milestoneHistoryRepository.save(h);
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  public List<MilestoneResponse> getProjectMilestones(long projectId) {
    List<ProjectMilestoneEntity> milestones = projectMilestoneRepository.findByProjectIdOrderByIdAsc(projectId);

    // 获取项目实体，用于读取项目级别的计划日期
    ProjectEntity project = projectRepository.findById(projectId).orElse(null);

    return milestones.stream().map(m -> {
      Long mid = m.getMilestoneId();
      MilestoneDefEntity def = milestoneDefRepository.findById(mid).orElse(null);
      if (def == null) return null;

      // 优先使用里程碑级别的计划日期，如果为空则从项目级别获取对应阶段的计划日期
      LocalDate plannedDate = m.getPlannedDate();
      if (plannedDate == null && project != null) {
        String milestoneCode = def.getMilestoneCode();
        plannedDate = switch (milestoneCode) {
          case "G0" -> project.getPlannedPccDate();
          case "G5" -> project.getPlannedIndDate();
          case "G9" -> project.getPlannedNdaDate();
          default -> null;
        };
      }

      LocalDate actualDate = m.getActualDate() != null
          ? m.getActualDate() : null;

      String decisionResult = m.getDecisionResult() != null
          ? m.getDecisionResult().name() : null;

      return new MilestoneResponse(
          def.getMilestoneCode(),
          def.getMilestoneName(),
          m.getStatus().name(),
          plannedDate,
          actualDate,
          def.getLeadDeptText(),
          def.getSortNo(),
          decisionResult,
          m.getDecisionAt(),
          m.getDecisionNotes()
      );
    }).filter(Objects::nonNull).toList();
  }
}

