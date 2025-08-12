package com.inframessaging.playground.messaging.api;

/**
 * 도메인 이벤트 발행 추상화.
 * - 서비스 코드는 구현체에 의존하지 않고 이 인터페이스만 사용합니다.
 * - 구현체는 Outbox 패턴을 통해 안전하게 전송을 보장합니다.
 */
public interface DomainEventPublisher {
    /** 기본 라우팅 옵션으로 발행 */
    void publish(CustomEvent event);
    /** 라우팅 옵션(키/라우팅키 오버라이드)과 함께 발행 */
    void publish(CustomEvent event, RoutingOptions opts);
}
