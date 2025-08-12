package com.inframessaging.playground.web;

import com.inframessaging.playground.messaging.api.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DomainEventPublisher publisher;

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody DemoRequest req) {
        DemoEvent event = new DemoEvent(req.getBroker(), req.getTopic(), req.getVersion(), req.getUserId());
        RoutingOptions opts = RoutingOptions.builder()
                .kafkaKey(req.getUserId())
                .routingKey("user.profile.updated")
                .build();
        publisher.publish(event, opts);
        return ResponseEntity.accepted().build();
    }

    @Data
    public static class DemoRequest {
        private BrokerType broker = BrokerType.KAFKA;
        private String topic = "user.profile.updated.v1"; // kafka topic or rabbit exchange
        private int version = 1;
        private String userId = "user-1";
    }

    @Data
    public static class DemoEvent implements CustomEvent {
        private final BrokerType brokerType;
        private final String topic;
        private final int version;
        private final String userId;

        @Override
        public BrokerType brokerType() { return brokerType; }
        @Override
        public String topic() { return topic; }
        @Override
        public int version() { return version; }
    }
}
