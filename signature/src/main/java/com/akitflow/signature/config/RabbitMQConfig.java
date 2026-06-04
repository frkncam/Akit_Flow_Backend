package com.akitflow.signature.config;

import com.akitflow.common.messaging.CommonRabbitConfig;
import com.akitflow.common.messaging.TransactionAwareEventPublisher;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonRabbitConfig.class, TransactionAwareEventPublisher.class})
public class RabbitMQConfig {

    public static final String SIGNATURE_EXCHANGE = "signature.exchange";

    public static final String RK_SIGNATURE_REQUESTED = "signature.requested";
    public static final String RK_SIGNATURE_BATCH_COMPLETED = "signature.batch.completed";
    public static final String RK_SIGNATURE_BATCH_REJECTED = "signature.batch.rejected";
    public static final String RK_SIGNATURE_OTP_REQUESTED = "signature.otp.requested";

    @Bean
    public TopicExchange signatureExchange() {
        return ExchangeBuilder.topicExchange(SIGNATURE_EXCHANGE).durable(true).build();
    }
}
