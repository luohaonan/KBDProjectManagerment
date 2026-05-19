package com.kbd.pms.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wf_template_node")
public class WfTemplateNodeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Column(name = "node_code", nullable = false, length = 64)
  private String nodeCode;

  @Column(name = "node_name", nullable = false, length = 128)
  private String nodeName;

  @Enumerated(EnumType.STRING)
  @Column(name = "node_type", nullable = false, length = 16)
  private Enums.WfNodeType nodeType;

  @Column(name = "sort_no", nullable = false)
  private Integer sortNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "approver_mode", length = 16)
  private Enums.ApproverMode approverMode;

  @Column(name = "approver_ref", length = 128)
  private String approverRef;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WfTemplateNodeEntity() {}

  public Long getId() { return id; }
  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public String getNodeCode() { return nodeCode; }
  public void setNodeCode(String nodeCode) { this.nodeCode = nodeCode; }
  public String getNodeName() { return nodeName; }
  public void setNodeName(String nodeName) { this.nodeName = nodeName; }
  public Enums.WfNodeType getNodeType() { return nodeType; }
  public void setNodeType(Enums.WfNodeType nodeType) { this.nodeType = nodeType; }
  public Integer getSortNo() { return sortNo; }
  public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
  public Enums.ApproverMode getApproverMode() { return approverMode; }
  public void setApproverMode(Enums.ApproverMode approverMode) { this.approverMode = approverMode; }
  public String getApproverRef() { return approverRef; }
  public void setApproverRef(String approverRef) { this.approverRef = approverRef; }
  public Instant getCreatedAt() { return createdAt; }
}

