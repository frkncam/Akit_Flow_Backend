package com.muhur.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.muhur.audit.domain")
@EnableJpaRepositories(
    basePackages = "com.muhur.audit.repository",
    repositoryBaseClass = com.muhur.common.tenant.TenantAwareJpaRepository.class
)
public class AuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditApplication.class, args);
    }
}
