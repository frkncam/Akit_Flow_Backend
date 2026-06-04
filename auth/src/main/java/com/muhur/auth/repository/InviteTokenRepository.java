package com.muhur.auth.repository;

import com.muhur.auth.domain.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {

    Optional<InviteToken> findByTokenHash(String tokenHash);

    boolean existsByEmailAndOrganizationIdAndUsedAtIsNull(String email, Long organizationId);
}
