package com.akitflow.common.client;

import com.akitflow.common.client.dto.ContractDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "contract-service")
public interface ContractClient {

    @GetMapping("/api/v1/contracts/{id}")
    ContractDto getContract(@PathVariable Long id,
                            @RequestHeader("X-User-Id") Long userId,
                            @RequestHeader("X-Org-Id") Long orgId,
                            @RequestHeader("X-User-Email") String email,
                            @RequestHeader("X-User-Role") String role);
}
