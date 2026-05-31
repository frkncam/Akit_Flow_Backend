package com.akitflow.contract;

import com.akitflow.contract.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableFeignClients(basePackages = "com.akitflow.common.client")
@EntityScan(basePackages = "com.akitflow.contract.domain")
@EnableJpaRepositories(
    basePackages = "com.akitflow.contract.repository",
    repositoryBaseClass = com.akitflow.common.tenant.TenantAwareJpaRepository.class
)
public class ContractApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractApplication.class, args);
    }
}
