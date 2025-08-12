package com.inframessaging.playground.infra_messaging_playground;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.inframessaging.playground")
@EnableJpaRepositories(basePackages = "com.inframessaging.playground.messaging.outbox")
@EntityScan(basePackages = "com.inframessaging.playground") // 엔티티가 있는 루트 패키지 지정
public class InfraMessagingPlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraMessagingPlaygroundApplication.class, args);
    }

}
