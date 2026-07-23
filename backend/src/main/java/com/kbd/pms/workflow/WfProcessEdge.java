package com.kbd.pms.workflow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "wf_process_edge")
public class WfProcessEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_definition_id", nullable = false)
    private WfProcessDefinition processDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", nullable = false)
    private WfProcessNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", nullable = false)
    private WfProcessNode toNode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WfProcessEdge() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WfProcessDefinition getProcessDefinition() { return processDefinition; }
    public void setProcessDefinition(WfProcessDefinition processDefinition) { this.processDefinition = processDefinition; }
    public WfProcessNode getFromNode() { return fromNode; }
    public void setFromNode(WfProcessNode fromNode) { this.fromNode = fromNode; }
    public WfProcessNode getToNode() { return toNode; }
    public void setToNode(WfProcessNode toNode) { this.toNode = toNode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WfProcessEdge that)) return false;
        if (id != null && that.id != null) return Objects.equals(id, that.id);
        return Objects.equals(fromNode, that.fromNode) && Objects.equals(toNode, that.toNode);
    }

    @Override
    public int hashCode() {
        if (id != null) return Objects.hash(id);
        return Objects.hash(fromNode, toNode);
    }
}