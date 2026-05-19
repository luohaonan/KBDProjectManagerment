package com.kbd.pms.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建项目（立项初期）请求体 — 不暴露数据库结构，仅承载业务字段。
 */
public record ProjectCreateRequest(
    @NotBlank(message = "项目名称不能为空") String projectName,
    /** 项目分级代号，如 H-L、G-L（对应 project_level.level_code） */
    @NotBlank(message = "项目分级代号不能为空") String levelCode,
    /** 拟定适应症 */
    @NotBlank(message = "拟定适应症不能为空") String indication,
    String targetPathway,
    String tppSummary,
    Long pmUserId,
    Long createdByUserId
) {}
