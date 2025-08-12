package com.inframessaging.playground.messaging.outbox;

public enum OutboxEventStatus {
    PENDING, SENT, FAILED, DEAD
}
