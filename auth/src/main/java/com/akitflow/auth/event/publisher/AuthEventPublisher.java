package com.akitflow.auth.event.publisher;

import com.akitflow.auth.config.AppProperties;
import com.akitflow.auth.config.RabbitMQConfig;
import com.akitflow.auth.domain.InviteToken;
import com.akitflow.auth.domain.User;
import com.akitflow.auth.event.AuthEvent;
import com.akitflow.auth.event.payload.UserInvitedPayload;
import com.akitflow.auth.event.payload.UserJoinedPayload;
import com.akitflow.auth.event.payload.UserRegisteredPayload;
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
        publish(USER_REGISTERED, AuthEvent.of(
                USER_REGISTERED,
                user.getOrganization().getId(),
                user.getId(),
                payload
        ));
    }

    public void publishUserInvited(InviteToken invite, User inviter, String rawToken) {
        String inviteLink = appProperties.inviteBaseUrl() + "?token=" + rawToken;
        String invitedByName = inviter.getFirstName() + " " + inviter.getLastName();

        var payload = new UserInvitedPayload(
                invite.getEmail(),
                invite.getRole(),
                inviteLink,
                invite.getOrganization().getName(),
                invitedByName
        );
        publish(USER_INVITED, AuthEvent.of(
                USER_INVITED,
                invite.getOrganization().getId(),
                inviter.getId(),
                payload
        ));
    }

    public void publishUserJoined(User user) {
        var payload = new UserJoinedPayload(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getOrganization().getName(),
                user.getRole()
        );
        publish(USER_JOINED, AuthEvent.of(
                USER_JOINED,
                user.getOrganization().getId(),
                user.getId(),
                payload
        ));
    }

    private void publish(String routingKey, AuthEvent<?> event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.AUTH_EXCHANGE, routingKey, event);
        } catch (Exception e) {
            // Event publish başarısız olsa bile ana iş akışını bozma
            log.error("Event publish hatası — routingKey={}, eventId={}",
                    routingKey, event.eventId(), e);
        }
    }
}
