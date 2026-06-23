package com.kbd.pms.event;

import com.kbd.pms.entity.ProjectEntity;
import com.kbd.pms.entity.ProjectMilestoneEntity;

public class MilestoneGoEvent {
  private final ProjectEntity project;
  private final ProjectMilestoneEntity milestone;

  public MilestoneGoEvent(ProjectEntity project, ProjectMilestoneEntity milestone) {
    this.project = project;
    this.milestone = milestone;
  }

  public ProjectEntity getProject() { return project; }
  public ProjectMilestoneEntity getMilestone() { return milestone; }
}