package com.inframessaging.playground.messaging.api;

public interface DomainEventPublisher {
    void publish(CustomEvent event);
    void publish(CustomEvent event, RoutingOptions opts);
}
