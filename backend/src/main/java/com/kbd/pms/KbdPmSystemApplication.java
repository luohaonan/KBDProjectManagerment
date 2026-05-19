package com.kbd.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.kbd.pms.entity")
public class KbdPmSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(KbdPmSystemApplication.class, args);
  }
}

