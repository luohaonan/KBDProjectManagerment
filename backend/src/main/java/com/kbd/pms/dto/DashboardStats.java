package com.kbd.pms.dto;

public record DashboardStats(
    int inProgressProjects,
    int pendingMilestoneReviews,
    int pendingInitiationReviews,
    int budgetAlerts
) {}