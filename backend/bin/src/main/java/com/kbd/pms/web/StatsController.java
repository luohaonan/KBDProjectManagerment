package com.kbd.pms.web;

import com.kbd.pms.dto.DashboardStats;
import com.kbd.pms.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ProjectService projectService;

    public StatsController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/dashboard")
    public Result<DashboardStats> getDashboardStats() {
        return Result.ok(projectService.getDashboardStats());
    }
}