package com.akitflow.contract.mapper;

import com.akitflow.contract.domain.Contract;
import com.akitflow.contract.domain.Party;
import com.akitflow.contract.dto.request.ContractCreateRequest;
import com.akitflow.contract.dto.request.ContractUpdateRequest;
import com.akitflow.contract.dto.request.PartyRequest;
import com.akitflow.contract.dto.response.ContractResponse;
import com.akitflow.contract.dto.response.PartyResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ContractMapper {

    // Request → Entity (parties JSON service tarafından set edilir; diğerleri ignore)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "parties", ignore = true)
    @Mapping(target = "signedAt", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Contract toEntity(ContractCreateRequest request);

    // Update (parties + status servisten yönetilir)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "parties", ignore = true)
    @Mapping(target = "signedAt", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Contract entity, ContractUpdateRequest request);

    // Entity → Response (parties manuel set edilecek — service'te)
    @Mapping(target = "parties", ignore = true)
    ContractResponse toResponse(Contract entity);

    // Helpers (parties domain ↔ DTO)
    @Named("partyToResponse")
    default PartyResponse partyToResponse(Party p) {
        return p == null ? null : new PartyResponse(p.name(), p.role(), p.email());
    }

    @Named("partyListToResponse")
    default List<PartyResponse> partyListToResponse(List<Party> list) {
        return list == null ? List.of() : list.stream().map(this::partyToResponse).toList();
    }

    @Named("partyRequestToDomain")
    default Party partyRequestToDomain(PartyRequest req) {
        return req == null ? null : new Party(req.name(), req.role(), req.email());
    }

    @Named("partyRequestListToDomain")
    default List<Party> partyRequestListToDomain(List<PartyRequest> list) {
        return list == null ? List.of() : list.stream().map(this::partyRequestToDomain).toList();
    }
}
