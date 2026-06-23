package com.kbd.pms.web;

import com.kbd.pms.dto.MilestoneApproveRequest;
import com.kbd.pms.dto.MilestoneResponse;
import com.kbd.pms.dto.MilestoneSubmitRequest;
import com.kbd.pms.service.MilestoneService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable("id") long id, @Valid @RequestBody MilestoneSubmitRequest req) {
        milestoneService.submitReview(id, req.actorUserId());
        return Result.ok(null);
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") long id, @Valid @RequestBody MilestoneApproveRequest req) {
        milestoneService.executeDecision(id, req);
        return Result.ok(null);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<MilestoneResponse>> getProjectMilestones(@PathVariable("projectId") long projectId) {
        return Result.ok(milestoneService.getProjectMilestones(projectId));
    }
}
