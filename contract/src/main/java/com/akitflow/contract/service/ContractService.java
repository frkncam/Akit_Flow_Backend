package com.akitflow.contract.service;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.Party;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.dto.request.ContractCreateRequest;
import com.akitflow.contract.dto.request.ContractUpdateRequest;
import com.akitflow.contract.dto.response.ContractResponse;
import com.akitflow.contract.event.payload.ContractCreatedPayload;
import com.akitflow.contract.event.payload.ContractStatusChangedPayload;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import com.akitflow.contract.exception.ResourceNotFoundException;
import com.akitflow.contract.mapper.ContractMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import com.akitflow.contract.security.HeaderPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final ContractMapper contractMapper;
    private final PartyJsonService partyJsonService;
    private final ContractEventPublisher eventPublisher;
    private final MinioService minioService;

    @Transactional
    public ContractResponse create(ContractCreateRequest request, HeaderPrincipal user) {
        Long orgId = user.organizationId();
        Long userId = user.userId();

        Contract contract = contractMapper.toEntity(request);
        contract.setOrganizationId(orgId);
        contract.setCreatedBy(userId);
        contract.setCreatorEmail(user.email());
        contract.setStatus(ContractStatus.DRAFT);

        List<Party> partyDomainList = contractMapper.partyRequestListToDomain(request.parties());
        contract.setParties(partyJsonService.serialize(partyDomainList));

        if (contract.getCurrency() == null) {
            contract.setCurrency("TRY");
        }

        contract = contractRepository.save(contract);

        eventPublisher.publishContractCreated(orgId, userId, new ContractCreatedPayload(
                contract.getId(),
                contract.getTitle(),
                contract.getContractType(),
                partyDomainList.stream().map(Party::email).filter(e -> e != null && !e.isBlank()).toList(),
                user.email()
        ));

        return toResponse(contract);
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> list(Pageable pageable, HeaderPrincipal user) {
        return contractRepository.findAllByOrganizationId(user.organizationId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long id, HeaderPrincipal user) {
        return toResponse(findOwnedOrThrow(id, user));
    }

    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        contractMapper.updateEntity(contract, request);

        if (request.parties() != null) {
            List<Party> partyDomainList = contractMapper.partyRequestListToDomain(request.parties());
            contract.setParties(partyJsonService.serialize(partyDomainList));
        }

        return toResponse(contractRepository.save(contract));
    }

    @Transactional
    public ContractResponse updateStatus(Long id, ContractStatus newStatus, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        ContractStatus oldStatus = contract.getStatus();

        if (oldStatus == newStatus) {
            return toResponse(contract);
        }
        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidContractStateTransitionException(oldStatus, newStatus);
        }

        contract.setStatus(newStatus);
        if (newStatus == ContractStatus.ACTIVE && contract.getSignedAt() == null) {
            contract.setSignedAt(Instant.now());
        }
        if (newStatus == ContractStatus.TERMINATED) {
            contract.setTerminatedAt(Instant.now());
        }

        contract = contractRepository.save(contract);

        eventPublisher.publishContractStatusChanged(user.organizationId(), user.userId(),
                new ContractStatusChangedPayload(
                        contract.getId(),
                        contract.getTitle(),
                        oldStatus,
                        newStatus,
                        user.email()
                ));

        return toResponse(contract);
    }

    @Transactional
    public void delete(Long id, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        contractFileRepository.findAllByContract_Id(id)
                .forEach(f -> minioService.delete(f.getStorageKey()));
        contractRepository.delete(contract);
    }

    private Contract findOwnedOrThrow(Long id, HeaderPrincipal user) {
        return contractRepository.findByIdAndOrganizationId(id, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + id));
    }

    private ContractResponse toResponse(Contract c) {
        ContractResponse base = contractMapper.toResponse(c);
        List<Party> partyDomain = partyJsonService.deserialize(c.getParties());
        return new ContractResponse(
                base.id(),
                base.organizationId(),
                base.title(),
                base.description(),
                base.contractType(),
                base.status(),
                contractMapper.partyListToResponse(partyDomain),
                base.startDate(),
                base.endDate(),
                base.signedAt(),
                base.terminatedAt(),
                base.value(),
                base.currency(),
                base.createdBy(),
                base.createdAt(),
                base.updatedAt()
        );
    }
}
