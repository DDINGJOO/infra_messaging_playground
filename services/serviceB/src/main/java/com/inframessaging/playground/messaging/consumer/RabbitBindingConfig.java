package com.inframessaging.playground.messaging.consumer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 바인딩 설정(데모용)
 * - demo.rabbit.exchange / demo.rabbit.queue / demo.rabbit.routing-key 값을 사용합니다.
 */
@Configuration
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
    public Queue demoQueue2(RabbitDemoProps props) {
        return new Queue(props.getQueue2(), true);
    }

    @Bean
    public Binding demoBinding(TopicExchange demoExchange, Queue demoQueue, RabbitDemoProps props) {
        return BindingBuilder.bind(demoQueue).to(demoExchange).with(props.getRoutingKey());
    }

    @Bean
    public Binding demoBinding2(TopicExchange demoExchange, Queue demoQueue2, RabbitDemoProps props) {
        return BindingBuilder.bind(demoQueue2).to(demoExchange).with(props.getRoutingKey2());
    }

    @Bean
    public RabbitDemoProps rabbitDemoProps() {
        return new RabbitDemoProps();
    }

    public static class RabbitDemoProps {
        private String exchange = System.getProperty("demo.rabbit.exchange", System.getenv().getOrDefault("DEMO_RABBIT_EXCHANGE", "user.events"));
        private String queue = System.getProperty("demo.rabbit.queue", System.getenv().getOrDefault("DEMO_RABBIT_QUEUE", "user.profile.updated.queue"));
        private String routingKey = System.getProperty("demo.rabbit.routing-key", System.getenv().getOrDefault("DEMO_RABBIT_ROUTING_KEY", "user.profile.updated"));
        private String queue2 = System.getProperty("demo.rabbit.queue2", System.getenv().getOrDefault("DEMO_RABBIT_QUEUE2", "user.activity.logged.queue"));
        private String routingKey2 = System.getProperty("demo.rabbit.routing-key2", System.getenv().getOrDefault("DEMO_RABBIT_ROUTING_KEY2", "user.activity.logged"));

        public String getExchange() { return exchange; }
        public String getQueue() { return queue; }
        public String getRoutingKey() { return routingKey; }
        public String getQueue2() { return queue2; }
        public String getRoutingKey2() { return routingKey2; }

        public void setExchange(String exchange) { this.exchange = exchange; }
        public void setQueue(String queue) { this.queue = queue; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        public void setQueue2(String queue2) { this.queue2 = queue2; }
        public void setRoutingKey2(String routingKey2) { this.routingKey2 = routingKey2; }
    }
}
