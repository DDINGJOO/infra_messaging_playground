package com.inframessaging.playground.messaging.api;

/**
 * Minimal CustomEvent contract used for the PoC.
 */
public interface CustomEvent {
    BrokerType brokerType();

    /**
     * Kafka topic or Rabbit exchange name.
     */
    String topic();

    /**
     * Event version (increase on breaking changes)
     */
    int version();
}
