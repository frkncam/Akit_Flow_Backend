package com.akitflow.signature.domain;

import com.akitflow.common.tenant.TenantEntityListener;
import com.akitflow.common.tenant.TenantScoped;
import com.akitflow.signature.domain.enums.SignatureStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "signature_schema", name = "signatures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter")
@EntityListeners(TenantEntityListener.class)
public class Signature implements TenantScoped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "contract_title", nullable = false, length = 255)
    private String contractTitle;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "file_storage_key", nullable = false, length = 512)
    private String fileStorageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "signer_name", nullable = false, length = 255)
    private String signerName;

    @Column(name = "signer_email", nullable = false, length = 255)
    private String signerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SignatureStatus status;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "provider_name", nullable = false, length = 32)
    private String providerName;

    @Column(name = "external_ref", length = 255)
    private String externalRef;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "signed_file_storage_key", length = 512)
    private String signedFileStorageKey;

    @Column(name = "signature_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String signatureMetadata;

    @Column(name = "document_hash", length = 64)
    private String documentHash;

    @Column(name = "otp_hash", length = 128)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    @Column(name = "otp_attempts")
    private Integer otpAttempts;

    @Column(name = "otp_verified_at")
    private Instant otpVerifiedAt;

    @Column(name = "signer_ip", length = 64)
    private String signerIp;

    @Column(name = "signer_user_agent", length = 512)
    private String signerUserAgent;

    @Column(name = "consent_accepted_at")
    private Instant consentAcceptedAt;

    @Column(name = "tsa_time")
    private Instant tsaTime;
}
