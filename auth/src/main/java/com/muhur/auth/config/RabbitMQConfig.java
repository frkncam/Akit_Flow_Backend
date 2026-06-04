package com.muhur.auth.config;

import com.muhur.common.messaging.CommonRabbitConfig;
import com.muhur.common.messaging.TransactionAwareEventPublisher;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
@Import({CommonRabbitConfig.class, TransactionAwareEventPublisher.class})
public class RabbitMQConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";

    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange(AUTH_EXCHANGE).durable(true).build();
    }
}
