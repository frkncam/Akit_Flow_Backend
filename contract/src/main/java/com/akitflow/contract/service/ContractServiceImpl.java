package com.akitflow.contract.service;

import com.akitflow.common.client.SignatureClient;
import com.akitflow.common.client.dto.BatchSignatureRequest;
import com.akitflow.common.client.dto.SignatureDto;
import com.akitflow.common.client.dto.SignerRequest;
import com.akitflow.common.event.payload.ContractCreatedPayload;
import com.akitflow.common.event.payload.ContractStatusChangedPayload;
import com.akitflow.common.exception.ResourceNotFoundException;
import com.akitflow.common.security.HeaderPrincipal;
import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.ContractFile;
import com.akitflow.contract.domain.Party;
import com.akitflow.contract.domain.enums.ContractStatus;
import com.akitflow.contract.dto.request.ContractCreateRequest;
import com.akitflow.contract.dto.request.ContractUpdateRequest;
import com.akitflow.contract.dto.request.SendForSignatureRequest;
import com.akitflow.contract.dto.response.ContractResponse;
import com.akitflow.contract.dto.response.SignatureSummaryResponse;
import com.akitflow.contract.event.publisher.ContractEventPublisher;
import com.akitflow.contract.exception.InvalidContractStateTransitionException;
import com.akitflow.contract.mapper.ContractMapper;
import com.akitflow.contract.repository.ContractFileRepository;
import com.akitflow.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final ContractMapper contractMapper;
    private final ContractEventPublisher eventPublisher;
    private final MinioService minioService;
    private final SignatureClient signatureClient;

    @Override
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
        contract.setParties(partyDomainList);

        if (contract.getCurrency() == null) {
            contract.setCurrency("TRY");
        }

        contract = contractRepository.save(contract);

        log.info("Contract created: id={}, orgId={}", contract.getId(), orgId);

        eventPublisher.publishContractCreated(orgId, userId, new ContractCreatedPayload(
                contract.getId(),
                contract.getTitle(),
                contract.getContractType().name(),
                partyDomainList.stream().map(Party::email).filter(e -> e != null && !e.isBlank()).toList(),
                user.email()
        ));

        return toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> list(Pageable pageable, HeaderPrincipal user) {
        return contractRepository.findAllByOrganizationId(user.organizationId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse get(Long id, HeaderPrincipal user) {
        return toResponse(findOwnedOrThrow(id, user));
    }

    @Override
    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        contractMapper.updateEntity(contract, request);

        if (request.parties() != null) {
            List<Party> partyDomainList = contractMapper.partyRequestListToDomain(request.parties());
            contract.setParties(partyDomainList);
        }

        return toResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional
    public ContractResponse updateStatus(Long id, ContractStatus newStatus, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        ContractStatus oldStatus = contract.getStatus();

        if (oldStatus == newStatus) {
            return toResponse(contract);
        }
        contract.transitionTo(newStatus);
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
                        oldStatus.name(),
                        newStatus.name(),
                        user.email()
                ));

        return toResponse(contract);
    }

    @Override
    @Transactional
    public void delete(Long id, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);
        contractFileRepository.findAllByContract_Id(id)
                .forEach(f -> minioService.delete(f.getStorageKey()));
        contractRepository.delete(contract);
    }

    @Override
    @Transactional
    public List<SignatureSummaryResponse> sendForSignature(Long id, SendForSignatureRequest request, HeaderPrincipal user) {
        Contract contract = findOwnedOrThrow(id, user);

        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new InvalidContractStateTransitionException(
                    contract.getStatus(), ContractStatus.PENDING_SIGNATURE);
        }

        ContractFile file = contractFileRepository.findById(request.fileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found: id=" + request.fileId()));
        if (!file.getContract().getId().equals(id)) {
            throw new ResourceNotFoundException("File does not belong to this contract: id=" + request.fileId());
        }

        var batchRequest = new BatchSignatureRequest(
                contract.getId(),
                contract.getTitle(),
                file.getId(),
                file.getStorageKey(),
                file.getFileName(),
                request.signers().stream()
                        .map(s -> new SignerRequest(s.name(), s.email()))
                        .toList()
        );

        List<SignatureDto> created =
                signatureClient.sendForSignature(batchRequest, user.userId(), user.organizationId(),
                        user.email(), user.role());

        contract.transitionTo(ContractStatus.PENDING_SIGNATURE);
        contractRepository.save(contract);

        return created.stream().map(SignatureSummaryResponse::from).toList();
    }

    @Override
    public List<SignatureSummaryResponse> listSignatures(Long id, HeaderPrincipal user) {
        return signatureClient.listForContract(id, user.userId(), user.organizationId(), user.role()).stream()
                .map(SignatureSummaryResponse::from)
                .toList();
    }

    private Contract findOwnedOrThrow(Long id, HeaderPrincipal user) {
        return contractRepository.findByIdAndOrganizationId(id, user.organizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Sözleşme bulunamadı: id=" + id));
    }

    private ContractResponse toResponse(Contract c) {
        ContractResponse base = contractMapper.toResponse(c);
        List<Party> partyDomain = c.getParties();
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
