package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoutingOptions {
    /** Kafka partitioning key */
    String kafkaKey;
    /** Rabbit routing key */
    String routingKey;
}
