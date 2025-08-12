package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class LoggingEventProducer implements EventProducer {

    private final BrokerType selected = BrokerType.KAFKA; // default; real selection is via bean exposure

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
