// Java
package com.inframessaging.playground.web;

import com.inframessaging.playground.messaging.api.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 데모 REST 컨트롤러 (ServiceA)
 * - 서비스 코드에서는 DomainEventPublisher만 사용. Envelope 생성/직렬화/Outbox 적재는 스타터(구현) 책임.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final DomainEventPublisher publisher;

    /**
     * 단건 샘플 이벤트 발행
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody DemoRequest req) {
        DemoEvent event = new DemoEvent(req.getBroker(), req.getTopic(), req.getVersion(), req.getUserId(), req.getPayload());
        // 명시적으로 kafkaKey와 rabbit routingKey를 옵션에 담아 전달
        RoutingOptions opts = RoutingOptions.builder()
                .kafkaKey(req.getUserId())                       // Kafka 파티션/순서 보장 키
                .routingKey("user.profile.updated")              // Rabbit routingKey 예시(오버라이드 가능)
                .build();
        publisher.publish(event, opts);
        return ResponseEntity.accepted().build();
    }

    /**
     * 두 브로커(KAFKA, RABBIT)로 동시에 발행하는 샘플
     */
    @PostMapping("/publish-both")
    public ResponseEntity<?> publishBoth(@RequestBody DemoRequest req) {
        // KAFKA 발행: kafkaKey 포함
        DemoEvent kafkaEvent = new DemoEvent(BrokerType.KAFKA, req.getTopic(), req.getVersion(), req.getUserId(), req.getPayload());
        RoutingOptions kafkaOpts = RoutingOptions.builder()
                .kafkaKey(req.getUserId())
                .build();
        publisher.publish(kafkaEvent, kafkaOpts);

        // RABBIT 발행: exchange + routingKey 명시
        DemoEvent rabbitEvent = new DemoEvent(BrokerType.RABBIT, req.getRabbitExchange(), req.getVersion(), req.getUserId(), req.getPayload());
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
        // 여러 Kafka 토픽으로 발행 (각 토픽마다 kafkaKey 전달)
        List<String> kafkaTopics = req.getKafkaTopics();
        if (kafkaTopics != null) {
            for (String kt : kafkaTopics) {
                DemoEvent ev = new DemoEvent(BrokerType.KAFKA, kt, req.getVersion(), req.getUserId(), req.getPayload());
                publisher.publish(ev, RoutingOptions.builder().kafkaKey(req.getUserId()).build());
            }
        }

        // 여러 Rabbit 라우팅키로 발행(같은 exchange)
        List<String> rabbitRoutingKeys = req.getRabbitRoutingKeys();
        if (rabbitRoutingKeys != null) {
            for (String rk : rabbitRoutingKeys) {
                DemoEvent ev = new DemoEvent(BrokerType.RABBIT, req.getRabbitExchange(), req.getVersion(), req.getUserId(), req.getPayload());
                publisher.publish(ev, RoutingOptions.builder().routingKey(rk).build());
            }
        }
        return ResponseEntity.accepted().build();
    }

    /**
     * - type: UserRegisteredEvent, version: 1
     * - payload: userId, email, userNumber, regCode
     */
    @PostMapping("/publish-user")
    public ResponseEntity<?> publishUser(@RequestBody UserRegisteredRequest req) {
        UserRegisteredEvent event = new UserRegisteredEvent(
                req.getBroker(),
                req.getTopic(),
                1,
                // payload는 Map으로 구성 (Producer 측은 클래스 불필요)
                java.util.Map.of(
                        "userId", req.getUserId(),
                        "email", req.getEmail(),
                        "userNumber", req.getUserNumber(),
                        "regCode", req.getRegCode()
                )
        );
        RoutingOptions opts = RoutingOptions.builder()
                .kafkaKey(req.getUserId())
                .routingKey(req.getRoutingKey() != null ? req.getRoutingKey() : "user.registered")
                .build();
        publisher.publish(event, opts);
        return ResponseEntity.accepted().build();
    }

    @Data
    public static class DemoRequest {
        private BrokerType broker = BrokerType.KAFKA;
        private String topic = "user.profile.updated.v1";
        private String rabbitExchange = "user.events";
        private String rabbitRoutingKey = "user.profile.updated";
        private java.util.List<String> kafkaTopics = java.util.List.of("user.profile.updated.v1", "user.activity.logged.v1");
        private java.util.List<String> rabbitRoutingKeys = java.util.List.of("user.profile.updated", "user.activity.logged");
        private int version = 1;
        private String userId = "user-1";
        private Object payload = java.util.Map.of("userId", "user-1"); // 샘플 페이로드
    }

    @Data
    public static class UserRegisteredRequest {
        private BrokerType broker = BrokerType.KAFKA; // KAFKA | RABBIT
        private String topic = "user.registered.v1"; // Kafka topic 또는 Rabbit exchange
        private String routingKey = "user.registered"; // Rabbit 사용 시
        private String userId;
        private String email;
        private Long userNumber;
        private Long regCode;
    }

    @Data
    public static class DemoEvent implements CustomEvent {
        private final BrokerType brokerType;
        private final String topic; // kafka topic OR rabbit exchange (routing determined by brokerType + RoutingOptions)
        private final int version;
        private final String userId;
        private final Object payload;

        @Override public BrokerType brokerType() { return brokerType; }
        @Override public String topic() { return topic; }
        @Override public int version() { return version; }
    }

    @Data
    public static class UserRegisteredEvent implements CustomEvent {
        private final BrokerType brokerType; // KAFKA or RABBIT
        private final String topic;          // Kafka topic or Rabbit exchange
        private final int version;           // 1
        private final Object payload;        // Map payload

        @Override public BrokerType brokerType() { return brokerType; }
        @Override public String topic() { return topic; }
        @Override public int version() { return version; }
    }
}
