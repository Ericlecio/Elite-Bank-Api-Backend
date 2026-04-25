package com.bancodigital.cqrs_eventsourcing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Elite Bank API")
                        .version("1.0")
                        .description("Sistema bancário")
                        .contact(new Contact()
                                .name("Ericlecio")
                                .email("etma@exemplo.com")));
    }
}