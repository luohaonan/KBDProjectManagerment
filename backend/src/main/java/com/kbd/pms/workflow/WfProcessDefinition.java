package com.kbd.pms.workflow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "wf_process_definition")
public class WfProcessDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_type", nullable = false, length = 32)
    private String processType;

    @Column(name = "milestone_code", length = 4)
    private String milestoneCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    @OneToMany(mappedBy = "processDefinition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<WfProcessNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "processDefinition", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WfProcessEdge> edges = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WfProcessDefinition() {}

    public Long getId() { return id; }
    public String getProcessType() { return processType; }
    public void setProcessType(String processType) { this.processType = processType; }
    public String getMilestoneCode() { return milestoneCode; }
    public void setMilestoneCode(String milestoneCode) { this.milestoneCode = milestoneCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<WfProcessNode> getNodes() { return nodes; }
    public void setNodes(List<WfProcessNode> nodes) { this.nodes = nodes; }
    public Set<WfProcessEdge> getEdges() { return edges; }
    public void setEdges(Set<WfProcessEdge> edges) { this.edges = edges; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}