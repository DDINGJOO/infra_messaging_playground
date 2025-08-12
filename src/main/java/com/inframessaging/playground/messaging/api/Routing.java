package com.inframessaging.playground.messaging.api;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Routing {
    BrokerType broker;
    Kafka kafka;
    Rabbit rabbit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kafka {
        String topic;
        String key;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbit {
        String exchange;
        String routingKey;
    }
}
