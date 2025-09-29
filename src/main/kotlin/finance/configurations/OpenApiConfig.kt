package finance.configurations

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OpenApiConfig {
    @Bean
    open fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Raspi Finance API")
                    .version("1.0.0")
                    .description("Personal Finance Management API built with Spring Boot, Kotlin, and GraphQL")
                    .contact(
                        Contact()
                            .name("Brian Henning")
                            .email("henninb@msn.com")
                            .url("https://github.com/henninb/raspi-finance-endpoint"),
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT"),
                    ),
            )
            .addServersItem(
                Server()
                    .url("https://finance.bhenning.com")
                    .description("Production server"),
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Development server"),
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Bearer Token Authentication"),
                    ),
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth"),
            )
    }
}
