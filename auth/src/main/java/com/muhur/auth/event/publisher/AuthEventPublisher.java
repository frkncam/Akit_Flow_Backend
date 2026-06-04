package com.muhur.auth.event.publisher;

import com.muhur.auth.config.AppProperties;
import com.muhur.auth.config.RabbitMQConfig;
import com.muhur.auth.domain.InviteToken;
import com.muhur.auth.domain.User;
import com.muhur.common.event.DomainEvent;
import com.muhur.common.event.payload.UserInvitedPayload;
import com.muhur.common.event.payload.UserJoinedPayload;
import com.muhur.common.event.payload.UserRegisteredPayload;
import com.muhur.common.messaging.TransactionAwareEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {

    private static final String USER_REGISTERED = "user.registered";
    private static final String USER_INVITED = "user.invited";
    private static final String USER_JOINED = "user.joined";

    private final TransactionAwareEventPublisher txPublisher;
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
        txPublisher.publish(RabbitMQConfig.AUTH_EXCHANGE, routingKey, event);
    }
}
