package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import java.util.Map;

public interface EventProducer {
    BrokerType brokerType();

    void sendKafka(String topic, String key, String bodyJson, Map<String, String> headers);
    void sendRabbit(String exchange, String routingKey, String bodyJson, Map<String, String> headers);
}
