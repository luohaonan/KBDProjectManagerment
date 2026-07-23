package com.kbd.pms.workflow;

import com.kbd.pms.entity.*;
import com.kbd.pms.exception.ApiException;
import com.kbd.pms.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class WfProcessService {

    private final WfProcessRepository processRepository;
    private final UserRepository userRepository;
    private final OrgDepartmentRepository orgDepartmentRepository;
    private final IamUserRepository iamUserRepository;
    private final GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository;
    private final ProjectRepository projectRepository;
    private final RoleRepository roleRepository;
    private final EntityManager entityManager;

    public WfProcessService(
            WfProcessRepository processRepository,
            UserRepository userRepository,
            OrgDepartmentRepository orgDepartmentRepository,
            IamUserRepository iamUserRepository,
            GovernanceCommitteeMemberRepository governanceCommitteeMemberRepository,
            ProjectRepository projectRepository,
            RoleRepository roleRepository,
            EntityManager entityManager) {
        this.processRepository = processRepository;
        this.userRepository = userRepository;
        this.orgDepartmentRepository = orgDepartmentRepository;
        this.iamUserRepository = iamUserRepository;
        this.governanceCommitteeMemberRepository = governanceCommitteeMemberRepository;
        this.projectRepository = projectRepository;
        this.roleRepository = roleRepository;
        this.entityManager = entityManager;
    }

    // ==================== 流程定义查询 ====================

    @Transactional(readOnly = true)
    public List<WfProcessDefinition> listAllProcesses() {
        return processRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WfProcessDefinition> listProcessesByType(String processType) {
        return processRepository.findByProcessType(processType, Sort.by(Sort.Direction.ASC, "milestoneCode"));
    }

    @Transactional(readOnly = true)
    public WfProcessDefinition getProcessDefinition(Long id) {
        return processRepository.findById(id)
                .orElseThrow(() -> new ApiException(404, "流程定义不存在: id=" + id));
    }

    @Transactional(readOnly = true)
    public WfProcessDefinition getActiveMilestoneProcess(String milestoneCode) {
        return processRepository
                .findByProcessTypeAndMilestoneCodeAndIsActiveTrue("MILESTONE", milestoneCode)
                .orElseThrow(() -> new ApiException(404, "未找到里程碑审批流程: " + milestoneCode));
    }

    @Transactional(readOnly = true)
    public WfProcessDefinition getActiveProcessByType(String processType) {
        return processRepository
                .findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue(processType)
                .orElseThrow(() -> new ApiException(404, "未找到审批流程: " + processType));
    }

    // ==================== 流程定义CRUD ====================

    @Transactional
    public WfProcessDefinition createProcess(WfProcessDefinition def) {
        def.setCreatedAt(Instant.now());
        def.setUpdatedAt(Instant.now());
        if (def.getNodes() != null) {
            for (WfProcessNode node : def.getNodes()) {
                node.setProcessDefinition(def);
                node.setCreatedAt(Instant.now());
                node.setUpdatedAt(Instant.now());
            }
        }
        if (def.getEdges() != null) {
            for (WfProcessEdge edge : def.getEdges()) {
                edge.setProcessDefinition(def);
                edge.setCreatedAt(Instant.now());
            }
        }
        return processRepository.save(def);
    }

    @Transactional
    public WfProcessDefinition updateProcess(Long id, WfProcessDefinition updated) {
        // ================================================================
        // 策略：使用 JPA 集合清理（orphanRemoval + clear），
        // 避免原生 SQL 删除可能产生的缓存不一致问题
        // ================================================================

        System.out.println("========== [WfProcessService.updateProcess] START ==========");
        System.out.println("[SVC] 传入 processId=" + id);
        System.out.println("[SVC] 传入 updated.nodes 数量=" + (updated.getNodes() != null ? updated.getNodes().size() : 0));
        System.out.println("[SVC] 传入 updated.edges 数量=" + (updated.getEdges() != null ? updated.getEdges().size() : 0));

        WfProcessDefinition existing = getProcessDefinition(id);

        // 打印清理前的状态
        System.out.println("[SVC] 清理前 existing.nodes 数量=" + existing.getNodes().size());
        for (var n : existing.getNodes()) {
            System.out.println("[SVC]   旧 node: id=" + n.getId() + ", code=" + n.getNodeCode() + ", name=" + n.getNodeName());
        }
        System.out.println("[SVC] 清理前 existing.edges 数量=" + existing.getEdges().size());
        for (var e : existing.getEdges()) {
            System.out.println("[SVC]   旧 edge: id=" + e.getId() + ", " + e.getFromNode().getNodeCode() + "(" + e.getFromNode().getId() + ") -> " + e.getToNode().getNodeCode() + "(" + e.getToNode().getId() + ")");
        }

        existing.setDescription(updated.getDescription());
        existing.setIsActive(updated.getIsActive());
        existing.setUpdatedAt(Instant.now());

        // 1) 先清理旧 edges（FK 指向 wf_process_node，必须先删）
        existing.getEdges().clear();
        System.out.println("[SVC] 步骤1: edges.clear() 后 edges 数量=" + existing.getEdges().size());

        // 2) 再清理旧 nodes（orphanRemoval=true → DELETE 旧行）
        existing.getNodes().clear();
        System.out.println("[SVC] 步骤2: nodes.clear() 后 nodes 数量=" + existing.getNodes().size());

        // 3) flush 确保 DELETE 已发送到 DB
        entityManager.flush();
        System.out.println("[SVC] 步骤3: flush() 完成");

        // 4) 插入新节点（id=null → INSERT），flush 获取自增 ID
        if (updated.getNodes() != null) {
            for (WfProcessNode node : updated.getNodes()) {
                node.setId(null);
                node.setProcessDefinition(existing);
                node.setCreatedAt(Instant.now());
                node.setUpdatedAt(Instant.now());
                existing.getNodes().add(node);
            }
            System.out.println("[SVC] 步骤4: 已添加 " + updated.getNodes().size() + " 个新节点到 existing");
            entityManager.flush();
            System.out.println("[SVC] 步骤4: flush() 后，existing.nodes 数量=" + existing.getNodes().size());
            for (var n : existing.getNodes()) {
                System.out.println("[SVC]   新 node: id=" + n.getId() + ", code=" + n.getNodeCode() + ", name=" + n.getNodeName());
            }
        }

        // 5) 插入新连线（关联步骤4生成的 DB ID）
        if (updated.getEdges() != null && !existing.getNodes().isEmpty()) {
            Map<String, WfProcessNode> nodeByCode = new LinkedHashMap<>();
            for (WfProcessNode node : existing.getNodes()) {
                if (node.getNodeCode() != null) {
                    nodeByCode.put(node.getNodeCode(), node);
                }
            }
            System.out.println("[SVC] 步骤5: nodeByCode 包含 " + nodeByCode.size() + " 个映射");
            for (var entry : nodeByCode.entrySet()) {
                System.out.println("[SVC]   " + entry.getKey() + " -> node.id=" + entry.getValue().getId());
            }

            int edgeAdded = 0;
            int edgeSkipped = 0;
            for (WfProcessEdge edge : updated.getEdges()) {
                WfProcessNode fromNode = edge.getFromNode() != null
                        ? nodeByCode.get(edge.getFromNode().getNodeCode()) : null;
                WfProcessNode toNode = edge.getToNode() != null
                        ? nodeByCode.get(edge.getToNode().getNodeCode()) : null;
                if (fromNode != null && toNode != null) {
                    edge.setId(null);
                    edge.setFromNode(fromNode);
                    edge.setToNode(toNode);
                    edge.setProcessDefinition(existing);
                    edge.setCreatedAt(Instant.now());
                    existing.getEdges().add(edge);
                    edgeAdded++;
                } else {
                    edgeSkipped++;
                    System.out.println("[SVC] ⚠ 跳过边: fromCode=" + (edge.getFromNode() != null ? edge.getFromNode().getNodeCode() : "null") + ", toCode=" + (edge.getToNode() != null ? edge.getToNode().getNodeCode() : "null") + " (fromNode=" + (fromNode != null) + ", toNode=" + (toNode != null) + ")");
                }
            }
            System.out.println("[SVC] 步骤5: 添加了 " + edgeAdded + " 条边, 跳过了 " + edgeSkipped + " 条边");
        }

        WfProcessDefinition saved = processRepository.save(existing);
        System.out.println("[SVC] save() 后 saved.nodes 数量=" + (saved.getNodes() != null ? saved.getNodes().size() : 0));
        System.out.println("[SVC] save() 后 saved.edges 数量=" + (saved.getEdges() != null ? saved.getEdges().size() : 0));
        System.out.println("========== [WfProcessService.updateProcess] END ==========");

        return saved;
    }

    @Transactional
    public void deleteProcess(Long id) {
        if (!processRepository.existsById(id)) throw new ApiException(404, "流程定义不存在");
        processRepository.deleteById(id);
    }

    // ==================== 流程执行引擎 ====================

    public Map<Long, List<WfProcessNode>> buildAdjacencyMap(WfProcessDefinition def) {
        Map<Long, List<WfProcessNode>> adj = new LinkedHashMap<>();
        Map<Long, WfProcessNode> nodeMap = def.getNodes().stream()
                .collect(Collectors.toMap(WfProcessNode::getId, n -> n));
        for (WfProcessEdge edge : def.getEdges()) {
            adj.computeIfAbsent(edge.getFromNode().getId(), k -> new ArrayList<>())
                    .add(nodeMap.get(edge.getToNode().getId()));
        }
        return adj;
    }

    public List<WfProcessNode> findStartNodes(WfProcessDefinition def) {
        Set<Long> targetNodeIds = def.getEdges().stream().map(e -> e.getToNode().getId()).collect(Collectors.toSet());
        return def.getNodes().stream()
                .filter(n -> !targetNodeIds.contains(n.getId()))
                .sorted(Comparator.comparing(WfProcessNode::getSortOrder))
                .collect(Collectors.toList());
    }

    public List<WfProcessNode> getApprovalNodesInOrder(WfProcessDefinition def) {
        Set<Long> visited = new HashSet<>();
        List<WfProcessNode> result = new ArrayList<>();
        Map<Long, List<WfProcessNode>> adj = buildAdjacencyMap(def);
        List<WfProcessNode> startNodes = findStartNodes(def);

        Deque<WfProcessNode> queue = new ArrayDeque<>(startNodes);
        while (!queue.isEmpty()) {
            WfProcessNode node = queue.poll();
            if (!visited.add(node.getId())) continue;
            if (!Boolean.TRUE.equals(node.getIsUploader())) {
                result.add(node);
            }
            List<WfProcessNode> nextNodes = adj.getOrDefault(node.getId(), Collections.emptyList());
            for (WfProcessNode next : nextNodes) {
                if (!visited.contains(next.getId())) queue.add(next);
            }
        }
        return result;
    }

    public List<ReviewApprovalTaskEntity> createMilestoneApprovalTasks(
            WfProcessDefinition def, Long approvalId, ProjectEntity project) {

        List<ReviewApprovalTaskEntity> tasks = new ArrayList<>();
        List<WfProcessNode> approvalNodes = getApprovalNodesInOrder(def);

        int sortOrder = 0;
        for (WfProcessNode node : approvalNodes) {
            List<Long> approverIds = resolveApprovers(node, project);
            String stepCode = deriveStepCode(node);
            for (Long approverId : approverIds) {
                ReviewApprovalTaskEntity task = new ReviewApprovalTaskEntity();
                task.setReviewApprovalId(approvalId);
                task.setApproverUserId(approverId);
                task.setApproverRole(node.getApproverRule() != null ? node.getApproverRule() : "ROLE_PMC");
                task.setStepCode(stepCode);
                task.setSortOrder(sortOrder++);
                task.setStatus(ReviewApprovalTaskEntity.Status.PENDING);
                task.setCreatedAt(Instant.now());
                task.setUpdatedAt(Instant.now());
                tasks.add(task);
            }
        }
        return tasks;
    }

    private String deriveStepCode(WfProcessNode node) {
        if (node.getNodeType() == null) return "UNKNOWN";
        return switch (node.getNodeType()) {
            case "DEPT_HEAD_APPROVE" -> "DEPT_HEAD_APPROVE";
            case "ROLE_APPROVE" -> {
                if ("ROLE_PM".equals(node.getApproverRule())) yield "PM_TECH_REVIEW";
                if ("ROLE_COMPLIANCE".equals(node.getApproverRule())) yield "COMPLIANCE_OPINION";
                yield "ROLE_APPROVE";
            }
            case "DECISION" -> {
                if ("ROLE_PMC".equals(node.getApproverRule())) yield "PMC_DECISION";
                if ("ROLE_PM".equals(node.getApproverRule())) yield "PM_INTERNAL_REVIEW";
                yield "DECISION";
            }
            default -> node.getNodeType();
        };
    }

    public List<Long> resolveApprovers(WfProcessNode node, ProjectEntity project) {
        if (node.getApproverRule() == null) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();

        switch (node.getApproverRule()) {
            case "ROLE_PM":
                if (project != null && project.getPmUserId() != null) ids.add(project.getPmUserId());
                break;
            case "ROLE_PMC":
                if (project != null && project.getPmcCommitteeId() != null) {
                    List<Long> pmcIds = governanceCommitteeMemberRepository
                            .findActiveMemberIds(project.getPmcCommitteeId(), LocalDate.now(ZoneOffset.UTC));
                    ids.addAll(pmcIds);
                }
                break;
            case "ROLE_COMPLIANCE":
                List<User> cu = userRepository.findActiveUsersByPermissionName("ROLE_COMPLIANCE");
                if (cu.isEmpty()) {
                    OrgDepartmentEntity d = orgDepartmentRepository.findById(7L).orElse(null);
                    if (d != null && d.getHeadUserId() != null) {
                        ids.add(d.getHeadUserId());
                    }
                } else {
                    for (User u : cu) {
                        ids.add(u.getId());
                    }
                }
                break;
            case "DEPT_HEAD":
                if (node.getApproverValue() != null) {
                    Arrays.stream(node.getApproverValue().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .forEach(s -> {
                                try {
                                    Long deptId = Long.parseLong(s);
                                    orgDepartmentRepository.findById(deptId).ifPresent(d -> {
                                        if (d.getHeadUserId() != null) ids.add(d.getHeadUserId());
                                    });
                                } catch (NumberFormatException e) {}
                            });
                }
                break;
            case "SPECIFIC_USER":
                if (node.getApproverValue() != null) {
                    Arrays.stream(node.getApproverValue().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .forEach(s -> {
                                try { ids.add(Long.parseLong(s)); } catch (NumberFormatException e) {}
                            });
                }
                break;
        }
        return ids;
    }

    // ==================== DTO ====================

    public WfProcessDefinitionResponse toDto(WfProcessDefinition def) {
        List<WfProcessNodeResponse> nodeDtos = def.getNodes().stream()
                .map(n -> new WfProcessNodeResponse(n.getId(), def.getId(), n.getNodeCode(), n.getNodeName(),
                        n.getNodeType(), n.getApproverRule(), n.getApproverValue(),
                        n.getDecisionType(), n.getIsUploader(), n.getDeliverableSlotCode(),
                        n.getPositionX(), n.getPositionY(), n.getSortOrder()))
                .collect(Collectors.toList());
        List<WfProcessEdgeResponse> edgeDtos = def.getEdges().stream()
                .map(e -> new WfProcessEdgeResponse(e.getId(), def.getId(),
                        e.getFromNode().getId(), e.getFromNode().getNodeCode(),
                        e.getToNode().getId(), e.getToNode().getNodeCode()))
                .collect(Collectors.toList());
        return new WfProcessDefinitionResponse(def.getId(), def.getProcessType(), def.getMilestoneCode(),
                def.getDescription(), def.getIsActive(), nodeDtos, edgeDtos);
    }

    public record WfProcessDefinitionResponse(
            Long id, String processType, String milestoneCode, String description,
            Boolean isActive, List<WfProcessNodeResponse> nodes, List<WfProcessEdgeResponse> edges) {}
    public record WfProcessNodeResponse(
            Long id, Long processDefinitionId, String nodeCode, String nodeName,
            String nodeType, String approverRule, String approverValue,
            String decisionType, Boolean isUploader, String deliverableSlotCode,
            Integer positionX, Integer positionY, Integer sortOrder) {}
    public record WfProcessEdgeResponse(
            Long id, Long processDefinitionId,
            Long fromNodeId, String fromNodeCode, Long toNodeId, String toNodeCode) {}
}