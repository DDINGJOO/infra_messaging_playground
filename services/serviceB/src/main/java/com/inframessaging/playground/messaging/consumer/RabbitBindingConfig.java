package com.inframessaging.playground.messaging.consumer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 바인딩 설정(데모용)
 * - messaging.type=RABBIT 일 때만 빈을 등록합니다.
 * - demo.rabbit.exchange / demo.rabbit.queue / demo.rabbit.routing-key 값을 사용합니다.
 */
@Configuration
@ConditionalOnProperty(prefix = "messaging", name = "type", havingValue = "RABBIT")
public class RabbitBindingConfig {

    @Bean
    public TopicExchange demoExchange(RabbitDemoProps props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public Queue demoQueue(RabbitDemoProps props) {
        return new Queue(props.getQueue(), true);
    }

    @Bean
    public Binding demoBinding(TopicExchange demoExchange, Queue demoQueue, RabbitDemoProps props) {
        return BindingBuilder.bind(demoQueue).to(demoExchange).with(props.getRoutingKey());
    }

    @Bean
    public RabbitDemoProps rabbitDemoProps() {
        return new RabbitDemoProps();
    }

    public static class RabbitDemoProps {
        private String exchange = System.getProperty("demo.rabbit.exchange", System.getenv().getOrDefault("DEMO_RABBIT_EXCHANGE", "user.events"));
        private String queue = System.getProperty("demo.rabbit.queue", System.getenv().getOrDefault("DEMO_RABBIT_QUEUE", "user.profile.updated.queue"));
        private String routingKey = System.getProperty("demo.rabbit.routing-key", System.getenv().getOrDefault("DEMO_RABBIT_ROUTING_KEY", "user.profile.updated"));

        public String getExchange() { return exchange; }
        public String getQueue() { return queue; }
        public String getRoutingKey() { return routingKey; }

        public void setExchange(String exchange) { this.exchange = exchange; }
        public void setQueue(String queue) { this.queue = queue; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
    }
}
