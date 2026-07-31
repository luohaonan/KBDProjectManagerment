package com.kbd.pms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import java.util.Properties;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);
  private static final String DEFAULT_INTERNAL_URL = "http://192.168.39.233:18080/";
  private static final String DEFAULT_EXTERNAL_URL = "http://343t787f48.wicp.vip/";

  private final SystemConfigService configService;
  private final TemplateEngine templateEngine;

  public EmailService(SystemConfigService configService, TemplateEngine templateEngine) {
    this.configService = configService;
    this.templateEngine = templateEngine;
  }

  /**
   * 发送通知邮件（异步，不阻塞主业务）
   * 使用 Thymeleaf 模板渲染 HTML 内容
   */
  @Async
  public void sendNotificationEmail(String to, String subject, String content) {
    // 使用模板渲染
    Context context = new Context(Locale.getDefault());
    context.setVariable("subject", subject);
    context.setVariable("content", content);
    String internalUrl = firstNonBlank(configService.getConfig("app.internal.url"), DEFAULT_INTERNAL_URL);
    String externalUrl = firstNonBlank(
        configService.getConfig("app.external.url"),
        configService.getConfig("app.base.url"),
        DEFAULT_EXTERNAL_URL);
    context.setVariable("internalLoginUrl", internalUrl);
    context.setVariable("externalLoginUrl", externalUrl);
    // Keep the legacy variable available for compatibility with older templates.
    context.setVariable("loginUrl", externalUrl);
    String htmlContent = templateEngine.process("email/notification", context);

    sendRawEmail(to, subject, htmlContent);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.trim().isEmpty()) {
        return value.trim();
      }
    }
    return "";
  }

  /**
   * 发送原始 HTML 邮件（内部方法）
   */
  private void sendRawEmail(String to, String subject, String htmlContent) {
    // 检查是否启用邮件发送
    String enabled = configService.getConfig("mail.enabled");
    if (!"true".equals(enabled)) {
      log.debug("邮件发送已关闭，跳过: to={}, subject={}", to, subject);
      return;
    }

    String host = configService.getConfig("mail.smtp.host");
    String port = configService.getConfig("mail.smtp.port");
    String username = configService.getConfig("mail.smtp.username");
    String password = configService.getConfig("mail.smtp.password");
    String from = configService.getConfig("mail.from.address");

    if (host == null || host.isEmpty() || username == null || username.isEmpty() || password == null || password.isEmpty()) {
      log.warn("邮件配置不完整，跳过邮件发送");
      return;
    }

    try {
      JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
      mailSender.setHost(host);
      mailSender.setPort(Integer.parseInt(port != null ? port : "465"));
      mailSender.setUsername(username);
      mailSender.setPassword(password);

      Properties props = configService.buildMailProperties();
      mailSender.setJavaMailProperties(props);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from != null && !from.isEmpty() ? from : username);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      mailSender.send(message);
      log.info("邮件发送成功: to={}, subject={}", to, subject);
    } catch (Exception e) {
      log.error("发送邮件失败: to={}, subject={}", to, subject, e);
    }
  }

  /**
   * 发送测试邮件，用于验证配置是否正确
   * @return null 表示成功，否则返回错误消息
   */
  public String sendTestEmail(String to) {
    String enabled = configService.getConfig("mail.enabled");
    if (!"true".equals(enabled)) {
      return "邮件发送功能未启用，请先启用后再测试";
    }

    String host = configService.getConfig("mail.smtp.host");
    String port = configService.getConfig("mail.smtp.port");
    String username = configService.getConfig("mail.smtp.username");
    String password = configService.getConfig("mail.smtp.password");
    String from = configService.getConfig("mail.from.address");

    if (host == null || host.isEmpty()) return "SMTP 服务器地址未配置";
    if (username == null || username.isEmpty()) return "发信邮箱账号未配置";
    if (password == null || password.isEmpty()) return "发信邮箱密码/授权码未配置";

    try {
      JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
      mailSender.setHost(host);
      mailSender.setPort(Integer.parseInt(port != null ? port : "465"));
      mailSender.setUsername(username);
      mailSender.setPassword(password);

      Properties props = configService.buildMailProperties();
      mailSender.setJavaMailProperties(props);

      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from != null && !from.isEmpty() ? from : username);
      helper.setTo(to);
      helper.setSubject("[PMS 测试邮件] 邮件配置验证");
      helper.setText("<h3>PMS 项目管理系统 - 测试邮件</h3><p>如果您收到此邮件，说明邮件配置正确！</p>", true);

      mailSender.send(message);
      return null; // 成功
    } catch (Exception e) {
      log.error("测试邮件发送失败", e);
      return e.getMessage();
    }
  }
}