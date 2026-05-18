package com.akitflow.contract.domain;

import com.akitflow.contract.domain.enums.SignatureStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "contract_schema", name = "contract_signatures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private ContractFile file;

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

    /**
     * Groups signatures created by a single send-for-signature call.
     * The "all signed" / "any rejected" decision in {@code accept} / {@code reject}
     * is scoped to this batch so historical rounds (e.g. a previous rejection)
     * do not block the current round's completion.
     */
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "signed_file_storage_key", length = 512)
    private String signedFileStorageKey;

    @Column(name = "signature_metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String signatureMetadata;
}
