package com.kbd.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = {"com.kbd.pms.entity", "com.kbd.pms.workflow"})
public class KbdPmSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(KbdPmSystemApplication.class, args);
  }
}

