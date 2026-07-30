package com.kbd.pms.workflow;

import com.kbd.pms.web.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/workflow")
public class WfProcessController {

    private final WfProcessService processService;

    public WfProcessController(WfProcessService processService) {
        this.processService = processService;
    }

    /** 获取所有流程 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<WfProcessService.WfProcessDefinitionResponse>> listAll() {
        return Result.ok(processService.listAllProcesses().stream()
                .map(processService::toDto).toList());
    }

    /** 按类型获取流程 */
    @GetMapping("/by-type/{processType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Result<List<WfProcessService.WfProcessDefinitionResponse>> listByType(
            @PathVariable String processType) {
        List<WfProcessService.WfProcessDefinitionResponse> dtos = processService.listProcessesByType(processType).stream()
                .map(processService::toDto).toList();
        System.out.println("========== [WfProcessController.listByType] ==========");
        System.out.println("[CTRL] 查询 processType=" + processType + ", 返回 " + dtos.size() + " 个流程");
        for (var dto : dtos) {
            System.out.println("[CTRL]   流程 id=" + dto.id() + ", milestoneCode=" + dto.milestoneCode() + ", nodes=" + dto.nodes().size() + ", edges=" + dto.edges().size());
            for (var nd : dto.nodes()) {
                System.out.println("[CTRL]     node: id=" + nd.id() + ", code=" + nd.nodeCode() + ", name=" + nd.nodeName());
            }
            for (var ed : dto.edges()) {
                System.out.println("[CTRL]     edge: id=" + ed.id() + ", " + ed.fromNodeCode() + "(" + ed.fromNodeId() + ") -> " + ed.toNodeCode() + "(" + ed.toNodeId() + ")");
            }
        }
        return Result.ok(dtos);
    }

    /** 获取单个流程 */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<WfProcessService.WfProcessDefinitionResponse> getById(@PathVariable Long id) {
        return Result.ok(processService.toDto(processService.getProcessDefinition(id)));
    }

    /** 更新流程 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Result<WfProcessService.WfProcessDefinitionResponse> update(
            @PathVariable Long id, @RequestBody WfProcessUpdateRequest request) {
        // ====== DEBUG LOG: 打印请求体 ======
        System.out.println("========== [WfProcessController.update] START ==========");
        System.out.println("[CTRL] 请求 processId=" + id);
        System.out.println("[CTRL] 请求 description=" + request.description() + ", isActive=" + request.isActive());
        System.out.println("[CTRL] 请求 budgetWarningThreshold=" + request.budgetWarningThreshold());
        if (request.nodes() != null) {
            System.out.println("[CTRL] 请求 nodes 数量=" + request.nodes().size());
            for (var nr : request.nodes()) {
                System.out.println("[CTRL]   node: code=" + nr.nodeCode() + ", name=" + nr.nodeName() + ", type=" + nr.nodeType() + ", approverRule=" + nr.approverRule() + ", approverValue=" + nr.approverValue() + ", isUploader=" + nr.isUploader() + ", pos=(" + nr.positionX() + "," + nr.positionY() + ")");
            }
        } else {
            System.out.println("[CTRL] 请求 nodes=null");
        }
        if (request.edges() != null) {
            System.out.println("[CTRL] 请求 edges 数量=" + request.edges().size());
            for (var er : request.edges()) {
                System.out.println("[CTRL]   edge: " + er.fromNodeCode() + " -> " + er.toNodeCode());
            }
        } else {
            System.out.println("[CTRL] 请求 edges=null");
        }

        WfProcessDefinition updated = new WfProcessDefinition();
        updated.setDescription(request.description());
        updated.setBudgetWarningThreshold(request.budgetWarningThreshold());
        updated.setIsActive(request.isActive() != null ? request.isActive() : true);

        if (request.nodes() != null) {
            List<WfProcessNode> nodes = new ArrayList<>();
            Map<String, WfProcessNode> nodeMap = new HashMap<>();
            for (WfProcessUpdateRequest.NodeRequest nr : request.nodes()) {
                WfProcessNode node = new WfProcessNode();
                node.setNodeCode(nr.nodeCode());
                node.setNodeName(nr.nodeName());
                node.setNodeType(nr.nodeType());
                node.setApproverRule(nr.approverRule());
                node.setApproverValue(nr.approverValue());
                node.setDecisionType(nr.decisionType());
                node.setIsUploader(nr.isUploader() != null ? nr.isUploader() : false);
                node.setDeliverableSlotCode(nr.deliverableSlotCode());
                node.setPositionX(nr.positionX() != null ? nr.positionX() : 0);
                node.setPositionY(nr.positionY() != null ? nr.positionY() : 0);
                node.setSortOrder(nr.sortOrder() != null ? nr.sortOrder() : 0);
                nodes.add(node);
                nodeMap.put(nr.nodeCode(), node);
            }
            updated.setNodes(nodes);

            if (request.edges() != null) {
                Set<WfProcessEdge> edges = new HashSet<>();
                for (WfProcessUpdateRequest.EdgeRequest er : request.edges()) {
                    WfProcessEdge edge = new WfProcessEdge();
                    WfProcessNode fromNode = nodeMap.get(er.fromNodeCode());
                    WfProcessNode toNode = nodeMap.get(er.toNodeCode());
                    if (fromNode != null && toNode != null) {
                        edge.setFromNode(fromNode);
                        edge.setToNode(toNode);
                        edges.add(edge);
                    } else {
                        System.out.println("[CTRL] ⚠ 跳过无效边: " + er.fromNodeCode() + "->" + er.toNodeCode() + " (fromNode=" + (fromNode != null) + ", toNode=" + (toNode != null) + ")");
                    }
                }
                updated.setEdges(edges);
                System.out.println("[CTRL] 构建后 edges 数量=" + edges.size());
            }
        }

        WfProcessDefinition result = processService.updateProcess(id, updated);

        // ====== DEBUG LOG: 打印返回结果 ======
        System.out.println("[CTRL] updateProcess 返回后:");
        System.out.println("[CTRL]   result nodes 数量=" + (result.getNodes() != null ? result.getNodes().size() : 0));
        if (result.getNodes() != null) {
            for (var n : result.getNodes()) {
                System.out.println("[CTRL]   result node: id=" + n.getId() + ", code=" + n.getNodeCode() + ", name=" + n.getNodeName());
            }
        }
        System.out.println("[CTRL]   result edges 数量=" + (result.getEdges() != null ? result.getEdges().size() : 0));
        if (result.getEdges() != null) {
            for (var e : result.getEdges()) {
                System.out.println("[CTRL]   result edge: id=" + e.getId() + ", " + e.getFromNode().getNodeCode() + "(" + e.getFromNode().getId() + ") -> " + e.getToNode().getNodeCode() + "(" + e.getToNode().getId() + ")");
            }
        }
        System.out.println("========== [WfProcessController.update] END ==========");

        return Result.ok(processService.toDto(result));
    }

    public record WfProcessUpdateRequest(
            String description,
            java.math.BigDecimal budgetWarningThreshold,
            Boolean isActive,
            List<NodeRequest> nodes,
            List<EdgeRequest> edges) {
        public record NodeRequest(
                String nodeCode, String nodeName, String nodeType,
                String approverRule, String approverValue, String decisionType,
                Boolean isUploader, String deliverableSlotCode,
                Integer positionX, Integer positionY, Integer sortOrder) {}
        public record EdgeRequest(String fromNodeCode, String toNodeCode) {}
    }
}