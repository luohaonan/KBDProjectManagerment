package com.kbd.pms.workflow;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WfProcessRepository extends JpaRepository<WfProcessDefinition, Long> {

    // 单个实体查询：使用 EntityGraph 预先加载（笛卡尔积影响可控）
    @EntityGraph(attributePaths = {"nodes", "edges"})
    Optional<WfProcessDefinition> findById(Long id);

    // 列表查询：只加载单个集合的 EntityGraph，另一集合由 @BatchSize 分批加载
    // 避免两个集合同时 LEFT JOIN 导致笛卡尔积
    @EntityGraph(attributePaths = {"nodes"})
    List<WfProcessDefinition> findAll();

    @EntityGraph(attributePaths = {"nodes"})
    Optional<WfProcessDefinition> findByProcessTypeAndMilestoneCodeAndIsActiveTrue(
            String processType, String milestoneCode);

    @EntityGraph(attributePaths = {"nodes"})
    Optional<WfProcessDefinition> findByProcessTypeAndMilestoneCodeIsNullAndIsActiveTrue(
            String processType);

    @EntityGraph(attributePaths = {"nodes"})
    List<WfProcessDefinition> findByProcessTypeAndIsActiveTrue(String processType);

    @EntityGraph(attributePaths = {"nodes"})
    List<WfProcessDefinition> findByProcessType(String processType, Sort sort);
}
}