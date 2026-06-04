package com.muhur.ai;

import com.muhur.ai.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// common modulundeki CommonAutoConfiguration @EnableJpaRepositories/@EntityScan'i
// com.muhur.common.* ile sinirlar; bu yuzden her servis KENDI paketlerini de
// acikca bildirmek zorunda (diger tum servislerle ayni pattern). Bunlar olmadan
// ai-service'in repository/entity'leri taranmaz ve uygulama ayaga kalkmaz.
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(AppProperties.class)
@EntityScan(basePackages = "com.muhur.ai.domain")
@EnableJpaRepositories(basePackages = "com.muhur.ai.repository")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
