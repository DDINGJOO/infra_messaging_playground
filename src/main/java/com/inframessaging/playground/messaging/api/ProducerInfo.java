package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProducerInfo {
    String service;
    String host;
    String env;
}
