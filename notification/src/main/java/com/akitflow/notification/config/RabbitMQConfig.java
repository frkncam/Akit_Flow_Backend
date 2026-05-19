package com.akitflow.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String CONTRACT_EXCHANGE = "contract.exchange";
    public static final String SIGNATURE_EXCHANGE = "signature.exchange";
    public static final String DLX = "notification.dlx";

    public static final String Q_USER_REGISTERED = "notification.user.registered";
    public static final String Q_USER_INVITED = "notification.user.invited";
    public static final String Q_USER_JOINED = "notification.user.joined";
    public static final String Q_CONTRACT_CREATED = "notification.contract.created";
    public static final String Q_CONTRACT_STATUS_CHANGED = "notification.contract.status.changed";
    public static final String Q_CONTRACT_EXPIRING_SOON = "notification.contract.expiring.soon";
    public static final String Q_CONTRACT_SIGNATURE_REQUESTED = "notification.contract.signature.requested";
    public static final String Q_CONTRACT_SIGNED = "notification.contract.signed";
    public static final String Q_CONTRACT_SIGNATURE_REJECTED = "notification.contract.signature.rejected";
    public static final String Q_SIGNATURE_REQUESTED = "notification.signature.requested";
    public static final String Q_SIGNATURE_BATCH_COMPLETED = "notification.signature.batch.completed";
    public static final String Q_SIGNATURE_BATCH_REJECTED = "notification.signature.batch.rejected";
    public static final String Q_DLQ = "notification.dlq";

    private static final String DLQ_ROUTING_KEY = "dlq";

    @Bean
    public TopicExchange authExchange() {
        return ExchangeBuilder.topicExchange(AUTH_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange contractExchange() {
        return ExchangeBuilder.topicExchange(CONTRACT_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange signatureExchange() {
        return ExchangeBuilder.topicExchange(SIGNATURE_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable(Q_USER_REGISTERED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userInvitedQueue() {
        return QueueBuilder.durable(Q_USER_INVITED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userJoinedQueue() {
        return QueueBuilder.durable(Q_USER_JOINED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractCreatedQueue() {
        return QueueBuilder.durable(Q_CONTRACT_CREATED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractStatusChangedQueue() {
        return QueueBuilder.durable(Q_CONTRACT_STATUS_CHANGED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractExpiringSoonQueue() {
        return QueueBuilder.durable(Q_CONTRACT_EXPIRING_SOON)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractSignatureRequestedQueue() {
        return QueueBuilder.durable(Q_CONTRACT_SIGNATURE_REQUESTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractSignedQueue() {
        return QueueBuilder.durable(Q_CONTRACT_SIGNED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contractSignatureRejectedQueue() {
        return QueueBuilder.durable(Q_CONTRACT_SIGNATURE_REJECTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue signatureRequestedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_REQUESTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue signatureBatchCompletedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_BATCH_COMPLETED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue signatureBatchRejectedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_BATCH_REJECTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(Q_DLQ).build();
    }

    @Bean
    public Binding bindRegistered(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with("user.registered");
    }

    @Bean
    public Binding bindInvited(Queue userInvitedQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userInvitedQueue).to(authExchange).with("user.invited");
    }

    @Bean
    public Binding bindJoined(Queue userJoinedQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userJoinedQueue).to(authExchange).with("user.joined");
    }

    @Bean
    public Binding bindContractCreated(Queue contractCreatedQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractCreatedQueue).to(contractExchange).with("contract.created");
    }

    @Bean
    public Binding bindContractStatusChanged(Queue contractStatusChangedQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractStatusChangedQueue).to(contractExchange).with("contract.status.changed");
    }

    @Bean
    public Binding bindContractExpiringSoon(Queue contractExpiringSoonQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractExpiringSoonQueue).to(contractExchange).with("contract.expiring.soon");
    }

    @Bean
    public Binding bindContractSignatureRequested(Queue contractSignatureRequestedQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractSignatureRequestedQueue).to(contractExchange).with("contract.signature.requested");
    }

    @Bean
    public Binding bindContractSigned(Queue contractSignedQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractSignedQueue).to(contractExchange).with("contract.signed");
    }

    @Bean
    public Binding bindContractSignatureRejected(Queue contractSignatureRejectedQueue, TopicExchange contractExchange) {
        return BindingBuilder.bind(contractSignatureRejectedQueue).to(contractExchange).with("contract.signature.rejected");
    }

    @Bean
    public Binding bindSignatureRequested(Queue signatureRequestedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureRequestedQueue).to(signatureExchange).with("signature.requested");
    }

    @Bean
    public Binding bindSignatureBatchCompleted(Queue signatureBatchCompletedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureBatchCompletedQueue).to(signatureExchange).with("signature.batch.completed");
    }

    @Bean
    public Binding bindSignatureBatchRejected(Queue signatureBatchRejectedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureBatchRejectedQueue).to(signatureExchange).with("signature.batch.rejected");
    }

    @Bean
    public Binding bindDlq(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter conv) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(conv);
        return template;
    }
}
