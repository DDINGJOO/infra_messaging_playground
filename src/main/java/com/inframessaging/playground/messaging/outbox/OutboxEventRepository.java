package com.inframessaging.playground.messaging.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query("select e from OutboxEventEntity e where e.status in (com.inframessaging.playground.messaging.outbox.OutboxEventStatus.PENDING, com.inframessaging.playground.messaging.outbox.OutboxEventStatus.FAILED) and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) order by e.createdAt asc")
    List<OutboxEventEntity> findProcessable(@Param("now") Instant now, Pageable pageable);
}
