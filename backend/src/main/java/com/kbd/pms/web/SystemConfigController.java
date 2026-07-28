package com.kbd.pms.web;

import com.kbd.pms.service.EmailService;
import com.kbd.pms.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system/config")
public class SystemConfigController {

  private final SystemConfigService configService;
  private final EmailService emailService;

  public SystemConfigController(SystemConfigService configService, EmailService emailService) {
    this.configService = configService;
    this.emailService = emailService;
  }

  /** 获取邮件配置（密码脱敏） */
  @GetMapping("/mail")
  public Result<Map<String, String>> getMailConfig() {
    return Result.ok(configService.getMailConfig());
  }

  /** 更新邮件配置 */
  @PutMapping("/mail")
  public Result<Void> updateMailConfig(@RequestBody Map<String, String> configs) {
    configService.saveMailConfig(configs);
    return Result.ok(null);
  }

  /** 发送测试邮件 */
  @PostMapping("/mail/test")
  public Result<String> sendTestEmail(@RequestBody Map<String, String> body) {
    String to = body.get("to");
    if (to == null || to.isEmpty()) {
      return Result.fail(400, "请输入测试邮箱地址");
    }
    String error = emailService.sendTestEmail(to);
    if (error == null) {
      return Result.ok("测试邮件发送成功");
    }
    return Result.fail(500, error);
  }
}