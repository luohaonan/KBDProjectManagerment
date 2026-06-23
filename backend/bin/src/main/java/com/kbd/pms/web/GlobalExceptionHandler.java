package com.kbd.pms.web;

import com.kbd.pms.exception.ApiException;
import com.kbd.pms.exception.BudgetExceededException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Result<Void>> handleApi(ApiException ex) {
    int code = ex.getCode();
    if (code < 100 || code > 599) {
      code = 400;
    }
    return ResponseEntity.status(code).body(Result.fail(code, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
    String msg =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(Result.fail(400, msg.isEmpty() ? "参数校验失败" : msg));
  }

  @ExceptionHandler(BudgetExceededException.class)
  public ResponseEntity<Result<Void>> handleBudgetExceeded(BudgetExceededException ex) {
    return ResponseEntity.status(423).body(Result.fail(423, ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException ex) {
    log.warn("登录失败：用户名或密码错误 - {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Result.fail(401, "用户名或密码错误"));
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<Result<Void>> handleDisabled(DisabledException ex) {
    log.warn("登录失败：账号已被禁用");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Result.fail(401, "账号已被禁用，请联系管理员"));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException ex) {
    log.warn("登录失败：认证异常 - {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Result.fail(401, "用户名或密码错误"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Result<Void>> handleAny(Exception ex) {
    // 只打印异常摘要和业务相关堆栈（前 5 行），避免框架内部堆栈刷屏
    log.error("Unhandled error: {} (cause: {})", ex.getMessage(),
        ex.getCause() != null ? ex.getCause().getMessage() : "N/A");
    // 打印前 5 行堆栈，聚焦业务代码
    StackTraceElement[] trace = ex.getStackTrace();
    int limit = Math.min(trace.length, 5);
    for (int i = 0; i < limit; i++) {
      log.error("  at {}.{}({}:{})", trace[i].getClassName(), trace[i].getMethodName(),
          trace[i].getFileName(), trace[i].getLineNumber());
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Result.fail(500, "服务器内部错误，请稍后重试"));
  }
}
