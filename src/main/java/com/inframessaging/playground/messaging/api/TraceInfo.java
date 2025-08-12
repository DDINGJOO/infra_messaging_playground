package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TraceInfo {
    String traceId;
    String correlationId;
}
