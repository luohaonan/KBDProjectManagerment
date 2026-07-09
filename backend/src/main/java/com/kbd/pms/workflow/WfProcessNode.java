package com.kbd.pms.workflow;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wf_process_node")
public class WfProcessNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_definition_id", nullable = false)
    private WfProcessDefinition processDefinition;

    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;

    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;

    @Column(name = "node_type", nullable = false, length = 32)
    private String nodeType;

    @Column(name = "approver_rule", length = 64)
    private String approverRule;

    @Column(name = "approver_value", length = 128)
    private String approverValue;

    @Column(name = "decision_type", length = 32)
    private String decisionType;

    @Column(name = "is_uploader", nullable = false)
    private Boolean isUploader = Boolean.FALSE;

    @Column(name = "deliverable_slot_code", length = 64)
    private String deliverableSlotCode;

    @Column(name = "position_x", nullable = false)
    private Integer positionX = 0;

    @Column(name = "position_y", nullable = false)
    private Integer positionY = 0;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WfProcessNode() {}

    public Long getId() { return id; }
    public WfProcessDefinition getProcessDefinition() { return processDefinition; }
    public void setProcessDefinition(WfProcessDefinition processDefinition) { this.processDefinition = processDefinition; }
    public String getNodeCode() { return nodeCode; }
    public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public String getApproverRule() { return approverRule; }
    public void setApproverRule(String approverRule) { this.approverRule = approverRule; }
    public String getApproverValue() { return approverValue; }
    public void setApproverValue(String approverValue) { this.approverValue = approverValue; }
    public String getDecisionType() { return decisionType; }
    public void setDecisionType(String decisionType) { this.decisionType = decisionType; }
    public Boolean getIsUploader() { return isUploader; }
    public void setIsUploader(Boolean isUploader) { this.isUploader = isUploader; }
    public String getDeliverableSlotCode() { return deliverableSlotCode; }
    public void setDeliverableSlotCode(String deliverableSlotCode) { this.deliverableSlotCode = deliverableSlotCode; }
    public Integer getPositionX() { return positionX; }
    public void setPositionX(Integer positionX) { this.positionX = positionX; }
    public Integer getPositionY() { return positionY; }
    public void setPositionY(Integer positionY) { this.positionY = positionY; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}