package com.kbd.pms.workflow;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WfProcessRepository extends JpaRepository<WfProcessDefinition, Long> {

    @EntityGraph(attributePaths = {"nodes", "edges"})
    Optional<WfProcessDefinition> findById(Long id);

    @EntityGraph(attributePaths = {"nodes", "edges"})
    List<WfProcessDefinition> findAll();

    @EntityGraph(attributePaths = {"nodes", "edges"})
    Optional<WfProcessDefinition> findByProcessTypeAndMilestoneCodeAndIsActiveTrue(
            String processType, String milestoneCode);

    @EntityGraph(attributePaths = {"nodes", "edges"})
    Optional<WfProcessDefinition> findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue(
            String processType);

    @EntityGraph(attributePaths = {"nodes", "edges"})
    List<WfProcessDefinition> findByProcessTypeAndIsActiveTrue(String processType);

    @EntityGraph(attributePaths = {"nodes", "edges"})
    List<WfProcessDefinition> findByProcessType(String processType);
}