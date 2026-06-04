package com.akitflow.notification.config;

import com.akitflow.common.messaging.CommonRabbitConfig;
import org.springframework.amqp.core.*;
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
    public static final String DLX = "notification.dlx";

    public static final String Q_USER_REGISTERED = "notification.user.registered";
    public static final String Q_USER_INVITED = "notification.user.invited";
    public static final String Q_USER_JOINED = "notification.user.joined";
    public static final String Q_CONTRACT_CREATED = "notification.contract.created";
    public static final String Q_CONTRACT_STATUS_CHANGED = "notification.contract.status.changed";
    public static final String Q_CONTRACT_EXPIRING_SOON = "notification.contract.expiring.soon";
    public static final String Q_SIGNATURE_REQUESTED = "notification.signature.requested";
    public static final String Q_SIGNATURE_BATCH_COMPLETED = "notification.signature.batch.completed";
    public static final String Q_SIGNATURE_BATCH_REJECTED = "notification.signature.batch.rejected";
    public static final String Q_SIGNATURE_OTP_REQUESTED = "notification.signature.otp.requested";
    public static final String Q_WORKFLOW_APPROVAL_REQUESTED = "notification.workflow.approval.requested";
    public static final String Q_WORKFLOW_APPROVAL_REJECTED = "notification.workflow.approval.rejected";
    public static final String Q_WORKFLOW_APPROVED = "notification.workflow.approved";
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
    public TopicExchange workflowExchange() {
        return ExchangeBuilder.topicExchange(WORKFLOW_EXCHANGE).durable(true).build();
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
    public Queue signatureOtpRequestedQueue() {
        return QueueBuilder.durable(Q_SIGNATURE_OTP_REQUESTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue workflowApprovalRequestedQueue() {
        return QueueBuilder.durable(Q_WORKFLOW_APPROVAL_REQUESTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue workflowApprovalRejectedQueue() {
        return QueueBuilder.durable(Q_WORKFLOW_APPROVAL_REJECTED)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue workflowApprovedQueue() {
        return QueueBuilder.durable(Q_WORKFLOW_APPROVED)
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
    public Binding bindSignatureOtpRequested(Queue signatureOtpRequestedQueue, TopicExchange signatureExchange) {
        return BindingBuilder.bind(signatureOtpRequestedQueue).to(signatureExchange).with("signature.otp.requested");
    }

    @Bean
    public Binding bindWorkflowApprovalRequested(Queue workflowApprovalRequestedQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(workflowApprovalRequestedQueue).to(workflowExchange).with("workflow.approval.requested");
    }

    @Bean
    public Binding bindWorkflowApprovalRejected(Queue workflowApprovalRejectedQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(workflowApprovalRejectedQueue).to(workflowExchange).with("workflow.approval.rejected");
    }

    @Bean
    public Binding bindWorkflowApproved(Queue workflowApprovedQueue, TopicExchange workflowExchange) {
        return BindingBuilder.bind(workflowApprovedQueue).to(workflowExchange).with("workflow.approved");
    }

    @Bean
    public Binding bindDlq(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
    }

}
