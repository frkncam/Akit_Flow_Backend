package com.muhur.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.muhur.common.client")
@EntityScan(basePackages = "com.muhur.workflow.domain")
@EnableJpaRepositories(
    basePackages = "com.muhur.workflow.repository",
    repositoryBaseClass = com.muhur.common.tenant.TenantAwareJpaRepository.class
)
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
