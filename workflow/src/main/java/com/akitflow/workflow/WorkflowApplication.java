package com.akitflow.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.akitflow.common.client")
@EntityScan(basePackages = "com.akitflow.workflow.domain")
@EnableJpaRepositories(
    basePackages = "com.akitflow.workflow.repository",
    repositoryBaseClass = com.akitflow.common.tenant.TenantAwareJpaRepository.class
)
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
