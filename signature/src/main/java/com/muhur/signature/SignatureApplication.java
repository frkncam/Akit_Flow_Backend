package com.muhur.signature;

import com.muhur.signature.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EntityScan(basePackages = "com.muhur.signature.domain")
@EnableJpaRepositories(
    basePackages = "com.muhur.signature.repository",
    repositoryBaseClass = com.muhur.common.tenant.TenantAwareJpaRepository.class
)
public class SignatureApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignatureApplication.class, args);
    }
}
