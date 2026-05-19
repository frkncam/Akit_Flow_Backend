package com.akitflow.contract.service;

import com.akitflow.contract.domain.Party;

import java.util.List;

public interface PartyJsonService {

    String serialize(List<Party> parties);

    List<Party> deserialize(String json);
}
