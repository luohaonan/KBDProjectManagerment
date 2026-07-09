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
    public Result<List<WfProcessService.WfProcessDefinitionResponse>> listByType(
            @PathVariable String processType) {
        return Result.ok(processService.listProcessesByType(processType).stream()
                .map(processService::toDto).toList());
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
        WfProcessDefinition updated = new WfProcessDefinition();
        updated.setDescription(request.description());
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
                    }
                }
                updated.setEdges(edges);
            }
        }

        WfProcessDefinition result = processService.updateProcess(id, updated);
        return Result.ok(processService.toDto(result));
    }

    public record WfProcessUpdateRequest(
            String description,
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