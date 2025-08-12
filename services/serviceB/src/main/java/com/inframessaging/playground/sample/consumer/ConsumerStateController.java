package com.inframessaging.playground.sample.consumer;

import com.inframessaging.playground.messaging.consumer.ConsumerState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ServiceB(Consumer) 상태 확인 컨트롤러
 * - 각 브로커에서 수신한 메시지 수를 반환합니다.
 */
@RestController
@RequestMapping("/api/consumer/state")
@RequiredArgsConstructor
public class ConsumerStateController {

    private final ConsumerState state;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getState() {
        return ResponseEntity.ok(Map.of(
                "kafkaReceived", state.getKafkaReceived().get(),
                "rabbitReceived", state.getRabbitReceived().get()
        ));
    }
}
