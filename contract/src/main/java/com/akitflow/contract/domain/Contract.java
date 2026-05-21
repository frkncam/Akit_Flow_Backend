package com.akitflow.contract.domain;

import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.domain.enums.ContractType;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(schema = "contract_schema", name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 32)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContractStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Party> parties;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(precision = 19, scale = 2)
    private BigDecimal value;

    @Column(length = 3)
    private String currency;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "creator_email", length = 255)
    private String creatorEmail;

    @Column(name = "notified_30d_at")
    private Instant notified30dAt;

    @Column(name = "notified_7d_at")
    private Instant notified7dAt;

    @Column(name = "notified_1d_at")
    private Instant notified1dAt;

    @Column(name = "workflow_state", length = 32)
    private String workflowState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void transitionTo(ContractStatus target) {
        if (status == target) {
            return;
        }
        if (!status.canTransitionTo(target)) {
            throw new InvalidContractStateTransitionException(status, target);
        }
        status = target;
    }
}
