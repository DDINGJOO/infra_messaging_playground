package com.inframessaging.playground.messaging.deser;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 이벤트 payload 역직렬화를 위한 타입/버전 → 클래스 매핑 레지스트리
 * - 서비스가 부팅 시 자신의 이벤트를 등록하여 수신 측에서 구체 타입으로 역직렬화할 수 있게 합니다.
 * - 등록이 없으면 컨슈머는 Map/JsonNode 등으로 처리할 수 있습니다.
 */
public class EventPayloadRegistry {

    private final ConcurrentMap<Key, Class<?>> registry = new ConcurrentHashMap<>();

    public void register(String type, int version, Class<?> payloadClass) {
        if (type == null || payloadClass == null) return;
        registry.put(new Key(type, version), payloadClass);
    }

    public Class<?> find(String type, int version) {
        if (type == null) return null;
        return registry.get(new Key(type, version));
    }

    private record Key(String type, int version) {
        Key {
            Objects.requireNonNull(type);
        }
    }
}
