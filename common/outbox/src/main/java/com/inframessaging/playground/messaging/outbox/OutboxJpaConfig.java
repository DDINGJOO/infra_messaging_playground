package com.inframessaging.playground.messaging.outbox;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 공통 JPA 스캔 설정
 * - 어떤 메인 애플리케이션 클래스를 실행하더라도, 메시징 Outbox 모듈의
 *   Repository/Entity가 항상 탐색되도록 보장합니다.
 * - 동일 루트 패키지(com.inframessaging.playground) 하위의 엔티티 전체를 스캔하고,
 *   outbox 리포지토리 패키지를 활성화합니다.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.inframessaging.playground.messaging.outbox")
@EntityScan(basePackages = "com.inframessaging.playground")
public class OutboxJpaConfig {
}
