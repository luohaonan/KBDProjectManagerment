package com.kbd.pms.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WfProcessEdgeRepository extends JpaRepository<WfProcessEdge, Long> {
    void deleteByProcessDefinitionId(Long processDefinitionId);
}