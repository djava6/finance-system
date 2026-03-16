package br.com.useinet.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("https://finance-system-zv4dye6hcq-uc.a.run.app")
                        .description("Produção (Cloud Run)"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local"))
                .info(new Info()
                        .title("Finance System API")
                        .version("1.0.0")
                        .description("API REST para gestão financeira pessoal. " +
                                "Obtenha um Firebase ID token no app e clique em 'Authorize' " +
                                "informando: Bearer {firebaseIdToken}"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("Firebase ID Token")));
    }
}