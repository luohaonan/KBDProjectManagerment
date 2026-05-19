package com.kbd.pms.listener;

import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.entity.MilestoneDefEntity;
import com.kbd.pms.event.MilestoneGoEvent;
import com.kbd.pms.repository.DocumentRepository;
import com.kbd.pms.repository.MilestoneDefRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MilestoneGoListener {

  private final DocumentRepository documentRepository;
  private final MilestoneDefRepository milestoneDefRepository;

  public MilestoneGoListener(DocumentRepository documentRepository, MilestoneDefRepository milestoneDefRepository) {
    this.documentRepository = documentRepository;
    this.milestoneDefRepository = milestoneDefRepository;
  }

  @EventListener
  @Transactional
  public void handleMilestoneGo(MilestoneGoEvent event) {
    MilestoneDefEntity def = milestoneDefRepository.findById(event.getMilestone().getMilestoneId())
        .orElseThrow(() -> new RuntimeException("Milestone definition not found"));

    Enums.MilestoneStage stage = Enums.MilestoneStage.valueOf(def.getMilestoneCode());

    // 锁定对应阶段的所有文档
    documentRepository.lockDocumentsByProjectAndPhase(event.getProject().getId(), stage);
  }
}