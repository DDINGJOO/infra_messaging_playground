package com.inframessaging.playground.messaging.config;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {
    private BrokerType type = BrokerType.KAFKA; // kafka | rabbit

    private Tracing tracing = new Tracing();

    @Data
    public static class Tracing {
        private boolean propagation = true;
    }
}
