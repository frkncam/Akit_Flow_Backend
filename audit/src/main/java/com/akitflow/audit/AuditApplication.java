package com.akitflow.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.akitflow.audit.domain")
@EnableJpaRepositories(
    basePackages = "com.akitflow.audit.repository",
    repositoryBaseClass = com.akitflow.common.tenant.TenantAwareJpaRepository.class
)
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
