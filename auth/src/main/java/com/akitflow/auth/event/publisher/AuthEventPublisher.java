package com.akitflow.auth.event.publisher;

import com.akitflow.auth.config.AppProperties;
import com.akitflow.auth.config.RabbitMQConfig;
import com.akitflow.auth.domain.InviteToken;
import com.akitflow.auth.domain.User;
import com.akitflow.common.event.DomainEvent;
import com.akitflow.common.event.payload.UserInvitedPayload;
import com.akitflow.common.event.payload.UserJoinedPayload;
import com.akitflow.common.event.payload.UserRegisteredPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private static final String USER_REGISTERED = "user.registered";
    private static final String USER_INVITED = "user.invited";
    private static final String USER_JOINED = "user.joined";

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    public void publishUserRegistered(User user) {
        var payload = new UserRegisteredPayload(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getOrganization().getName()
        );
        publish(USER_REGISTERED, user.getOrganization().getId(), user.getId(), payload);
    }

    public void publishUserInvited(InviteToken invite, User inviter, String rawToken) {
        String inviteLink = appProperties.inviteBaseUrl() + "?token=" + rawToken;
        String invitedByName = inviter.getFirstName() + " " + inviter.getLastName();

        var payload = new UserInvitedPayload(
                invite.getEmail(),
                invite.getRole().name(),
                inviteLink,
                invite.getOrganization().getName(),
                invitedByName
        );
        publish(USER_INVITED, invite.getOrganization().getId(), inviter.getId(), payload);
    }

    public void publishUserJoined(User user) {
        var payload = new UserJoinedPayload(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getOrganization().getName(),
                user.getRole().name()
        );
        publish(USER_JOINED, user.getOrganization().getId(), user.getId(), payload);
    }

    private <T> void publish(String routingKey, Long organizationId, Long actorId, T payload) {
        DomainEvent<T> event = DomainEvent.of(routingKey, organizationId, actorId, payload);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Event publish hatası — routingKey={}, eventId={}",
                    routingKey, event.eventId(), e);
        }
    }
}
