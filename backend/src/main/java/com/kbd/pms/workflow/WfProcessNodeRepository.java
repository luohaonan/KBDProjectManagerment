package com.kbd.pms.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WfProcessNodeRepository extends JpaRepository<WfProcessNode, Long> {
    void deleteByProcessDefinitionId(Long processDefinitionId);
}