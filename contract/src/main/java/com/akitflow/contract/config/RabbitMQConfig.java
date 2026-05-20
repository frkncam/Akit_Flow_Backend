package com.akitflow.contract.config;

import com.akitflow.common.messaging.CommonRabbitConfig;
import com.akitflow.common.messaging.TransactionAwareEventPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonRabbitConfig.class, TransactionAwareEventPublisher.class})
public class RabbitMQConfig {

    public static final String CONTRACT_EXCHANGE = "contract.exchange";
    public static final String SIGNATURE_EXCHANGE = "signature.exchange";

    public static final String RK_CONTRACT_CREATED         = "contract.created";
    public static final String RK_CONTRACT_STATUS_CHANGED  = "contract.status.changed";
    public static final String RK_CONTRACT_EXPIRING_SOON   = "contract.expiring.soon";

    public static final String Q_SIGNATURE_BATCH_COMPLETED = "contract.signature.batch.completed";
    public static final String Q_SIGNATURE_BATCH_REJECTED = "contract.signature.batch.rejected";

    @Bean
    public TopicExchange contractExchange() {
        return ExchangeBuilder.topicExchange(CONTRACT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange signatureExchange() {
        return ExchangeBuilder.topicExchange(SIGNATURE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue signatureBatchCompletedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_BATCH_COMPLETED).build();
    }

    @Bean
    public Queue signatureBatchRejectedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_BATCH_REJECTED).build();
    }

    @Bean
    public Binding bindSignatureBatchCompleted(Queue signatureBatchCompletedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureBatchCompletedQueue).to(signatureExchange).with("signature.batch.completed");
    }

    @Bean
    public Binding bindSignatureBatchRejected(Queue signatureBatchRejectedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureBatchRejectedQueue).to(signatureExchange).with("signature.batch.rejected");
    }
}
