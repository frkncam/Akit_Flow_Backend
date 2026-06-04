package com.muhur.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.muhur.template.domain")
@EnableJpaRepositories(
    basePackages = "com.muhur.template.repository",
    repositoryBaseClass = com.muhur.common.tenant.TenantAwareJpaRepository.class
)
@EnableFeignClients(basePackages = "com.muhur.common.client")
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
