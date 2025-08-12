package com.inframessaging.playground.web;

import com.inframessaging.playground.messaging.api.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데모 REST 컨트롤러 (ServiceA)
 * - POST /api/demo/publish 로 샘플 이벤트를 Outbox에 적재 → 프로세서가 전송합니다.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DomainEventPublisher publisher;

    /**
     * 샘플 이벤트 발행 엔드포인트
     * - body로 브로커/토픽(익스체인지)/버전/userId를 받습니다.
     * - Kafka 키로 userId를 사용하고, Rabbit 라우팅키는 예시로 고정값을 넣습니다.
     */
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

    /**
     * 두 브로커(KAFKA, RABBIT)로 동시에 발행하는 샘플
     */
    @PostMapping("/publish-both")
    public ResponseEntity<?> publishBoth(@RequestBody DemoRequest req) {
        // KAFKA
        DemoEvent kafkaEvent = new DemoEvent(BrokerType.KAFKA, req.getTopic(), req.getVersion(), req.getUserId());
        RoutingOptions kafkaOpts = RoutingOptions.builder()
                .kafkaKey(req.getUserId())
                .build();
        publisher.publish(kafkaEvent, kafkaOpts);
        // RABBIT
        DemoEvent rabbitEvent = new DemoEvent(BrokerType.RABBIT, req.getRabbitExchange(), req.getVersion(), req.getUserId());
        RoutingOptions rabbitOpts = RoutingOptions.builder()
                .routingKey(req.getRabbitRoutingKey())
                .build();
        publisher.publish(rabbitEvent, rabbitOpts);
        return ResponseEntity.accepted().build();
    }

    /**
     * 여러 토픽/라우팅키로 다건 발행하는 샘플
     */
    @PostMapping("/publish-multi")
    public ResponseEntity<?> publishMulti(@RequestBody DemoRequest req) {
        // 여러 Kafka 토픽으로 발행
        if (req.getKafkaTopics() != null && !req.getKafkaTopics().isEmpty()) {
            for (String kt : req.getKafkaTopics()) {
                DemoEvent ev = new DemoEvent(BrokerType.KAFKA, kt, req.getVersion(), req.getUserId());
                publisher.publish(ev, RoutingOptions.builder().kafkaKey(req.getUserId()).build());
            }
        }
        // 여러 Rabbit 라우팅키로 발행(같은 exchange)
        if (req.getRabbitRoutingKeys() != null && !req.getRabbitRoutingKeys().isEmpty()) {
            for (String rk : req.getRabbitRoutingKeys()) {
                DemoEvent ev = new DemoEvent(BrokerType.RABBIT, req.getRabbitExchange(), req.getVersion(), req.getUserId());
                publisher.publish(ev, RoutingOptions.builder().routingKey(rk).build());
            }
        }
        return ResponseEntity.accepted().build();
    }

    @Data
    public static class DemoRequest {
        /** 사용 브로커(KAFKA/RABBIT) */
        private BrokerType broker = BrokerType.KAFKA;
        /** Kafka 토픽 */
        private String topic = "user.profile.updated.v1"; // kafka topic
        /** Rabbit 익스체인지 */
        private String rabbitExchange = "user.events";
        /** Rabbit 라우팅키 */
        private String rabbitRoutingKey = "user.profile.updated";
        /** 여러 Kafka 토픽(다건 발행용) */
        private java.util.List<String> kafkaTopics = java.util.List.of("user.profile.updated.v1", "user.activity.logged.v1");
        /** 여러 Rabbit 라우팅키(다건 발행용) */
        private java.util.List<String> rabbitRoutingKeys = java.util.List.of("user.profile.updated", "user.activity.logged");
        /** 이벤트 버전 */
        private int version = 1;
        /** 사용자 ID(샘플 페이로드이자 Kafka 파티션 키로 사용) */
        private String userId = "user-1";
    }

    /**
     * 샘플 도메인 이벤트 구현체
     */
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
