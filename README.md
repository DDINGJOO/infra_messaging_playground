
# HELP.md

본 문서는 infra-messaging / messaging-starter / outbox 관련 PoC 프로젝트(또는 샘플 서비스)를 빠르게 이해하고 운영·검증하는 데 필요한 사용법과 체크리스트를 정리한 안내서입니다.

목차
- 개요
- 빠른 시작 (로컬)
- 주요 개념 요약
- 개발자 API 사용법(요약)
- 권장 application.yml 예시
- Outbox 동작 흐름
- Consumer(수신) 동작 요점
- 관측성(트레이싱/메트릭/로그)
- 테스트 및 검증 절차
- 마이그레이션/적용 가이드(요약)
- 운영 체크리스트
- 문제해결(FAQ)
- 추가 참고자료

---

## 개요
이 프로젝트는 도메인 코드가 브로커(Kafka/Rabbit)에 직접 의존하지 않도록 DomainEventPublisher, EventEnvelope, RoutingOptions 같은 추상 API를 제공하고, 실제 전송·재시도·DLQ·트레이싱·메트릭 등은 messaging-starter(또는 infra 모듈)에서 일괄 처리하도록 설계된 PoC입니다. Outbox 패턴을 사용해 DB에 안전하게 이벤트를 적재한 뒤 프로세서가 브로커로 전송합니다.

---

## 빠른 시작 (로컬)
1. 필요한 로컬 의존 서비스 기동
	- DB (MySQL/Postgres 또는 H2)
	- Kafka 또는 RabbitMQ (Testcontainers 권장)
2. DB 스키마 생성
	- event_outbox 테이블(README의 DDL 참조)
3. application.yml 설정 (아래 예시 참고)
4. 애플리케이션 실행
	- producer 서비스: POST /api/demo/publish 등으로 이벤트 적재 확인
	- OutboxProcessor 로그가 SENT/FAILED 전이를 찍는지 확인
	- consumer 서비스가 있으면 메시지 수신 확인

---

## 주요 개념 요약
- Envelope: id, type, version, occurredAt, producer, trace, routing, payload를 포함하는 표준 래퍼 JSON. Outbox의 `envelope` 컬럼에 저장.
- OutboxEventEntity: event_outbox 테이블의 엔티티. 상태(status), retry_count, next_attempt_at, last_error_message, sent_at 등을 관리.
- DomainEventPublisher: 서비스가 호출하는 추상 발행 API. 실제 Envelope 빌드·직렬화·Outbox 적재는 구현체가 담당.
- RoutingOptions: kafkaKey(파티션/순서 보장용), routingKey(Rabbit) 등 발행 시 오버라이드 옵션.
- OutboxProcessor: 주기적으로 PENDING/재시도 대상 레코드를 조회해 브로커로 전송. 성공(SENT), 실패(FAILED/DEAD) 전이 처리.

---

## 개발자 API 사용법(요약)
- 발행
	- publisher.publish(CustomEvent event);
	- publisher.publish(CustomEvent event, RoutingOptions opts);
	- (옵션) publisher.publish(List<CustomEvent> events, RoutingOptions opts);
- 소비
	- @EventConsumer(topic="...", version=..., broker=BrokerType.KAFKA) 또는 기존 @KafkaListener/@RabbitListener 사용 가능
	- EventHandler<T>: void handle(EventEnvelope<T> envelope)

서비스 개발자는 DomainEventPublisher를 주입받아 호출만 하면 됩니다. Envelope 구성/직렬화/Outbox 적재는 스타터가 처리합니다.

---

## 권장 application.yml 예시
아래는 필수/권장 프로퍼티의 예시입니다. 실제 값은 환경에 맞게 변경하세요.

~~~
# yaml
server:
port: 8081

spring:
application:
name: ${APP_NAME:sample-service}
datasource:
url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:app_db}
username: ${DB_USER:username}
password: ${DB_PASS:password}

messaging:
type: kafka # kafka | rabbit
kafka:
bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
rabbit:
host: ${RABBIT_HOST:localhost}
port: ${RABBIT_PORT:5672}
username: ${RABBIT_USER:guest}
password: ${RABBIT_PASS:guest}
tracing:
propagation: true

outbox:
enabled: true
batch:
size: 200
schedule:
delay-ms: 1000
retry:
max-attempts: 10
backoff:
base-seconds: 5
max-seconds: 300
jitter-rate: 0.1
dead-letter:
enabled: true
kafka-suffix: ".DLQ"
rabbit-suffix: ".dlq"

~~~
---

## Outbox 동작 흐름
1. 서비스 코드가 DomainEventPublisher.publish(event, opts)를 호출.
2. Publisher 구현체가 EventEnvelope를 생성하고 JSON으로 직렬화 시도.
	- 직렬화 성공 → Outbox에 PENDING 레코드로 적재(관련 컬럼: kafka_topic/rabbit_exchange/message_key 등 채움).
	- 직렬화 실패 → Outbox에 상태 = FAILED로 적재하고 lastErrorMessage 기록(운영 가시성 확보).
3. OutboxProcessor가 주기적으로 PENDING/재시도 도래 건을 조회.
4. 각 레코드에 대해 EventProducer를 통해 브로커 전송.
	- 전송 성공 → 상태 = SENT, sentAt 기록
	- 전송 실패 → retry_count 증가, nextAttemptAt = now + backoff, 상태 = FAILED 또는 DEAD(임계치 초과)
5. DEAD 처리 시 설정에 따라 DLQ로 자동 라우팅하거나 수동 운영으로 전환.

---

## Consumer(수신) 동작 요점
- 수신 시 Envelope 우선 파싱. 레거시 포맷은 fallback으로 지원.
- Envelope.trace에서 traceId/correlationId를 MDC에 복원해 로깅/트레이싱을 유지.
- 역직렬화 실패 또는 핸들러 예외는 공통 에러 핸들러로 처리(재시도/Dead-letter).
- Kafka: DefaultErrorHandler + DeadLetterPublishingRecoverer 권장. Rabbit: RepublishMessageRecoverer / DLX 권장.

---

## 관측성(트레이싱/메트릭/로그)
- 트레이싱
	- 요청 처리 시 MDC(traceId, correlationId)를 설정하고 Envelope.trace로 전파.
	- 전송 시 Kafka/Rabbit 헤더에 traceId 주입.
- 메트릭 (Micrometer)
	- 필수 지표: outbox_processed_total, outbox_failed_total, outbox_dead_total, backlog_gauge, processing_latency_histogram
- 로그
	- 구조화 로그에 Envelope의 id/type/traceId/topic/exchange/status 포함 권장.

---

## 테스트 및 검증 절차
1. 단위 테스트
	- Envelope 빌더, DataSerializer(직렬화/역직렬화), RoutingOptions 적용 테스트 작성.
2. 통합(E2E) 테스트
	- Testcontainers로 DB + Kafka/Rabbit 띄워 Outbox 적재→Processor→Broker 전송→Consumer 수신 검증.
3. 실패 시나리오
	- 직렬화 실패: publish 호출 시 Outbox에 FAILED 레코드와 lastErrorMessage가 생성되는지 확인.
	- 브로커 장애: 재시도/nextAttemptAt/DEAD 전이 검사.
	- DLQ: DEAD → DLQ(토픽/교환) 라우팅 동작 확인.
4. 부하 테스트
	- 배치 사이즈 및 동시 처리 성능, 지연 등을 측정해 튜닝.

---

## 마이그레이션 / 적용 가이드(요약)
- 1단계: 데이터베이스 스키마 확장(예: envelope 컬럼 추가, sent_at, message_key 추가).
- 2단계: 서비스에서 DomainEventPublisher로 전환(기존 직접 Producer 호출을 대신).
- 3단계: OutboxProcessor가 Envelope 기반으로 전송하도록 스타터 적용.
- 4단계: Consumer는 Envelope 우선 파싱으로 변경(dual-format 지원 기간 운영).
- 5단계: 모니터링/메트릭/알람 설정 및 운영 검증 후 레거시 필드 정리.

---

## 운영 체크리스트
- Outbox INSERT 후 Processor가 전송하여 SENT로 전이되는가?
- 직렬화 실패 케이스가 Outbox에 FAILED로 남고 lastErrorMessage가 기록되는가?
- retry_count/nextAttemptAt가 정상적으로 증가·계산되는가? (지수 백오프 + 지터)
- DLQ 전송 규칙(예: topic.DLQ, exchange.dlx + routingKey.dlq)이 일관되게 동작하는가?
- Kafka 전송 시 message_key가 전달되어 파티셔닝/순서 보장이 되는가?
- traceId 헤더가 전파되고 Consumer에서 MDC가 복원되는가?
- Micrometer 지표가 수집되고 대시보드/알람이 설정되어있는가?

---

## 문제해결(FAQ)
Q. 이벤트가 소비되지 않아요.  
A. 1) Kafka: topic 이름/partition 설정, consumer group 확인 2) Rabbit: exchange/queue/routingKey 바인딩 확인 3) Envelope 대신 레거시 포맷이 사용되는지 확인

Q. 직렬화 실패로 이벤트가 손실되나요?  
A. 본 설계에서는 직렬화 실패도 Outbox에 FAILED로 남아 운영자가 원인 확인 및 재처리 가능하도록 합니다. lastErrorMessage 로그 확인.

Q. 같은 메시지가 중복 전송됩니다.  
A. Processor 동시 처리 제어(ShedLock + 낙관적 락)와 Kafka idempotence 설정(acks=all, enable.idempotence=true)으로 중복 전송 완화 필요.

Q. DLQ에서 메시지를 어떻게 재처리하나요?  
A. DLQ는 별도 소비자/수동 워크플로로 처리합니다(정정·재발행). 운영 절차를 문서화하세요.

---




