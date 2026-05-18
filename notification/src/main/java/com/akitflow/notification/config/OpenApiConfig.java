package com.akitflow.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AkitFlow Notification Service")
                        .version("v1")
                        .description("Email dispatch & audit log. HTTP endpoints are public "
                                + "(internal use); main interface is RabbitMQ event consumption."));
    }
}
