package br.com.useinet.finance.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    companion object {
        private const val SECURITY_SCHEME_NAME = "Bearer Authentication"
    }

    @Bean
    fun openAPI(): OpenAPI {
        val prodUrl = System.getenv("SWAGGER_SERVER_URL")
            ?: "https://finance-system-zv4dye6hcq-uc.a.run.app"
        return OpenAPI()
        .addServersItem(Server().url(prodUrl).description("Produção (Cloud Run)"))
        .addServersItem(Server().url("http://localhost:8080").description("Local"))
        .info(
            Info()
                .title("Finance System API")
                .version("1.0.0")
                .description(
                    "API REST para gestão financeira pessoal. " +
                    "Obtenha um Firebase ID token no app e clique em 'Authorize' " +
                    "informando: Bearer {firebaseIdToken}"
                )
        )
        .addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME_NAME,
                SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("Firebase ID Token")
            )
        )
    }
}
