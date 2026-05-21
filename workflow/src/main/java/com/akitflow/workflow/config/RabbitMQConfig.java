package com.akitflow.workflow.config;

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

    public static final String WORKFLOW_EXCHANGE = "workflow.exchange";

    public static final String RK_WORKFLOW_TRANSITIONED = "workflow.transitioned";
    public static final String RK_WORKFLOW_APPROVAL_REQUESTED = "workflow.approval.requested";
    public static final String RK_WORKFLOW_APPROVAL_REJECTED = "workflow.approval.rejected";
    public static final String RK_WORKFLOW_APPROVED = "workflow.approved";

    @Bean
    public TopicExchange workflowExchange() {
        return ExchangeBuilder.topicExchange(WORKFLOW_EXCHANGE).durable(true).build();
    }
}
