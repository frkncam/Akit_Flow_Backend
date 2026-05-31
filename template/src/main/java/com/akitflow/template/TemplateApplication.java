package com.akitflow.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.akitflow.template.domain")
@EnableJpaRepositories(
    basePackages = "com.akitflow.template.repository",
    repositoryBaseClass = com.akitflow.common.tenant.TenantAwareJpaRepository.class
)
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
