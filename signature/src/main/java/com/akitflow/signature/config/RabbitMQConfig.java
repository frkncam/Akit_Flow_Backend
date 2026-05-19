package com.akitflow.signature.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SIGNATURE_EXCHANGE = "signature.exchange";

    public static final String RK_SIGNATURE_REQUESTED = "signature.requested";
    public static final String RK_SIGNATURE_BATCH_COMPLETED = "signature.batch.completed";
    public static final String RK_SIGNATURE_BATCH_REJECTED = "signature.batch.rejected";

    @Bean
    public TopicExchange signatureExchange() {
        return ExchangeBuilder.topicExchange(SIGNATURE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }
}
