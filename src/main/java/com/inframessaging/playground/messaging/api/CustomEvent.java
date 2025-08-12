package com.inframessaging.playground.messaging.api;

/**
 * 서비스에서 정의하는 도메인 이벤트 계약(PoC 최소 형태)
 * - 어떤 브로커를 사용할지, 어디로 보낼지(토픽/익스체인지), 이벤트 버전을 노출합니다.
 */
public interface CustomEvent {
    /** 사용 브로커 타입 */
    BrokerType brokerType();

    /**
     * Kafka 토픽 또는 Rabbit 익스체인지명
     * - brokerType에 따라 의미가 달라집니다.
     */
    String topic();

    /**
     * 이벤트 버전(파괴적 변경 시 증가)
     */
    int version();
}
