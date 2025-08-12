package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 실제 브로커 전송 대신 로그로 출력하는 PoC용 Producer 구현체
 */
@Slf4j
public class LoggingEventProducer implements EventProducer {

    private final BrokerType selected = BrokerType.KAFKA; // default; 실제 선택은 빈 주입/설정에 따름

    @Override
    public BrokerType brokerType() {
        return selected;
    }

    @Override
    public void sendKafka(String topic, String key, String bodyJson, Map<String, String> headers) {
        log.info("[LoggingProducer][KAFKA] topic={} key={} headers={} body={}", topic, key, headers, bodyJson);
    }

    @Override
    public void sendRabbit(String exchange, String routingKey, String bodyJson, Map<String, String> headers) {
        log.info("[LoggingProducer][RABBIT] exchange={} routingKey={} headers={} body={}", exchange, routingKey, headers, bodyJson);
    }
}
