package com.inframessaging.playground.services.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ServiceB (Consumer)
 * - 루트 모듈의 컨슈머/상태 컨트롤러를 스캔하여 사용합니다.
 */
@SpringBootApplication(scanBasePackages = "com.inframessaging.playground")
public class ServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }
}
