package com.inframessaging.playground.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Outbox 이벤트를 조회/저장하는 JPA 리포지토리.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * 처리 가능한(전송 대상) 이벤트 배치를 조회합니다.
     * - 대상 상태: PENDING(미전송) + FAILED(재시도 대상)
     * - 스케줄 기준: nextAttemptAt가 null(즉시) 이거나 now 이하(기한 도래)
     * - 정렬: createdAt ASC (오래된 것부터 처리)
     * - Pageable의 size로 배치 크기를 제한합니다.
     */
    @Query("select e from OutboxEventEntity e where e.status in (com.inframessaging.playground.messaging.outbox.OutboxEventStatus.PENDING, com.inframessaging.playground.messaging.outbox.OutboxEventStatus.FAILED) and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) order by e.createdAt asc")
    List<OutboxEventEntity> findProcessable(@Param("now") Instant now, Pageable pageable);
}
