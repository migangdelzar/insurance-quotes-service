package com.clara.insurancequotes.shared.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("openApiConfig")
public class OpenApiConfiguration {

    @Bean
    public OpenAPI insuranceQuotesOpenApi() {
        var bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .info(new Info()
                        .title("Insurance Quotes API")
                        .version("1.0.0")
                        .description("Multi-step insurance quote flow: create, coverage, submit."))
                .components(new Components().addSecuritySchemes("bearerAuth", bearer))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
