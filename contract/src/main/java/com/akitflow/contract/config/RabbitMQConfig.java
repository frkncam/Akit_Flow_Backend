package com.akitflow.contract.config;

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

    public static final String CONTRACT_EXCHANGE = "contract.exchange";

    public static final String RK_CONTRACT_CREATED         = "contract.created";
    public static final String RK_CONTRACT_STATUS_CHANGED  = "contract.status.changed";
    public static final String RK_CONTRACT_EXPIRING_SOON        = "contract.expiring.soon";
    public static final String RK_CONTRACT_SIGNATURE_REQUESTED  = "contract.signature.requested";
    public static final String RK_CONTRACT_SIGNED               = "contract.signed";
    public static final String RK_CONTRACT_SIGNATURE_REJECTED   = "contract.signature.rejected";

    @Bean
    public TopicExchange contractExchange() {
        return ExchangeBuilder.topicExchange(CONTRACT_EXCHANGE).durable(true).build();
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
