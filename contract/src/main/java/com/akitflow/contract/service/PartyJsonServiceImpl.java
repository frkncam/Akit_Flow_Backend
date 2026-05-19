package com.akitflow.contract.service;

import com.akitflow.contract.domain.Party;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyJsonServiceImpl implements PartyJsonService {

    private static final TypeReference<List<Party>> LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(List<Party> parties) {
        try {
            return objectMapper.writeValueAsString(parties == null ? List.of() : parties);
        } catch (Exception e) {
            throw new IllegalStateException("Parties JSON serialize edilemedi: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Party> deserialize(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("Parties JSON parse edilemedi: " + e.getMessage(), e);
        }
    }
}
