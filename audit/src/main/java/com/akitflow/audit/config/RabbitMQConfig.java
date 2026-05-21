package com.akitflow.audit.config;

import com.akitflow.common.messaging.CommonRabbitConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonRabbitConfig.class)
public class RabbitMQConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String CONTRACT_EXCHANGE = "contract.exchange";
    public static final String SIGNATURE_EXCHANGE = "signature.exchange";
    public static final String WORKFLOW_EXCHANGE = "workflow.exchange";

    public static final String Q_AUDIT_EVENTS = "audit.events.queue";
    public static final String DLX = "audit.dlx";
    public static final String Q_DLQ = "audit.dlq";
    public static final String DLQ_ROUTING_KEY = "dlq";

    @Bean
    public TopicExchange auditAuthExchange() {
        return ExchangeBuilder.topicExchange(AUTH_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange auditContractExchange() {
        return ExchangeBuilder.topicExchange(CONTRACT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange auditSignatureExchange() {
        return ExchangeBuilder.topicExchange(SIGNATURE_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange auditWorkflowExchange() {
        return ExchangeBuilder.topicExchange(WORKFLOW_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange auditDlx() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue auditEventsQueue() {
        return QueueBuilder.durable(Q_AUDIT_EVENTS)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(Q_DLQ).build();
    }

    @Bean
    public Binding bindAuditAuth(Queue auditEventsQueue, TopicExchange auditAuthExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(auditAuthExchange).with("#");
    }

    @Bean
    public Binding bindAuditContract(Queue auditEventsQueue, TopicExchange auditContractExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(auditContractExchange).with("#");
    }

    @Bean
    public Binding bindAuditSignature(Queue auditEventsQueue, TopicExchange auditSignatureExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(auditSignatureExchange).with("#");
    }

    @Bean
    public Binding bindAuditWorkflow(Queue auditEventsQueue, TopicExchange auditWorkflowExchange) {
        return BindingBuilder.bind(auditEventsQueue).to(auditWorkflowExchange).with("#");
    }

    @Bean
    public Binding bindAuditDlq(Queue auditDlq, DirectExchange auditDlx) {
        return BindingBuilder.bind(auditDlq).to(auditDlx).with(DLQ_ROUTING_KEY);
    }
}
