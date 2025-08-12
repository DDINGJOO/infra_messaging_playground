package com.inframessaging.playground.services.servicea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ServiceA (Producer)
 * - 기존 루트 모듈의 컴포넌트(Outbox, DemoController 등)를 스캔하여 사용합니다.
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.inframessaging.playground")
public class ServiceAApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceAApplication.class, args);
    }
}
