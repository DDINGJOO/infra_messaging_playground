package deser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.inframessaging.playground.messaging.api.Envelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Envelope 역직렬화 유틸리티
 * - JSON 문자열에서 Envelope<T>로 역직렬화합니다.
 * - EventPayloadRegistry에 (type, version) 매핑이 있으면 해당 T로, 없으면 Map으로 payload를 파싱합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class EnvelopeDeserializer {

    private final ObjectMapper objectMapper;
    private final EventPayloadRegistry registry;

    /** JSON → Envelope<T or Map> */
    public Envelope<?> deserialize(String json) {
        try {
            // 먼저 type/version만 빠르게 파싱하기보다는, JavaType을 동적으로 생성하여 바로 시도합니다.
            // 레지스트리에 매핑이 없는 경우를 대비해 Map payload 타입으로 fallback합니다.
            // 1) Map payload 타입으로 파싱해서 type/version을 확인
            Envelope<Map<String, Object>> prelim = objectMapper.readValue(json, new TypeReference<Envelope<Map<String, Object>>>() {});
            String type = prelim.getType();
            int version = prelim.getVersion();
            Class<?> payloadClass = registry.find(type, version);
            if (payloadClass == null) {
                return prelim; // 등록이 없으면 Map payload 유지
            }
            // 2) 매핑된 payloadClass로 재파싱
            TypeFactory tf = objectMapper.getTypeFactory();
            var javaType = tf.constructParametricType(Envelope.class, payloadClass);
            return objectMapper.readValue(json, javaType);
        } catch (Exception e) {
            log.warn("Envelope deserialize failed, fallback to raw string payload. error={}", e.getMessage());
            // 최후의 수단: payload를 문자열로 유지
            try {
                TypeFactory tf = objectMapper.getTypeFactory();
                var javaType = tf.constructParametricType(Envelope.class, String.class);
                return objectMapper.readValue(json, javaType);
            } catch (Exception ignored) {
                // 정말 불가하면 null 반환(컨슈머에서 별도 처리)
                return null;
            }
        }
    }
}
