package finance.configurations

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme
import spock.lang.Specification

class OpenApiConfigSpec extends Specification {

    OpenApiConfig openApiConfig

    def setup() {
        openApiConfig = new OpenApiConfig()
    }

    def "should create OpenAPI configuration with correct info"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI != null
        openAPI.info != null
        openAPI.info.title == "Raspi Finance API"
        openAPI.info.version == "1.0.0"
        openAPI.info.description == "Personal Finance Management API built with Spring Boot, Kotlin, and GraphQL"
    }

    def "should configure contact information"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.info.contact != null
        openAPI.info.contact.name == "Brian Henning"
        openAPI.info.contact.email == "henninb@msn.com"
        openAPI.info.contact.url == "https://github.com/henninb/raspi-finance-endpoint"
    }

    def "should configure license information"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.info.license != null
        openAPI.info.license.name == "MIT License"
        openAPI.info.license.url == "https://opensource.org/licenses/MIT"
    }

    def "should configure production and development servers"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.servers != null
        openAPI.servers.size() == 2

        def productionServer = openAPI.servers.find { it.url == "https://finance.bhenning.com" }
        productionServer != null
        productionServer.description == "Production server"

        def developmentServer = openAPI.servers.find { it.url == "http://localhost:8080" }
        developmentServer != null
        developmentServer.description == "Development server"
    }

    def "should configure JWT Bearer token security scheme"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.components != null
        openAPI.components.securitySchemes != null
        openAPI.components.securitySchemes.containsKey("bearerAuth")

        SecurityScheme bearerAuth = openAPI.components.securitySchemes.get("bearerAuth")
        bearerAuth != null
        bearerAuth.type == SecurityScheme.Type.HTTP
        bearerAuth.scheme == "bearer"
        bearerAuth.bearerFormat == "JWT"
        bearerAuth.description == "JWT Bearer Token Authentication"
    }

    def "should configure global security requirement"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.security != null
        openAPI.security.size() == 1
        openAPI.security[0].containsKey("bearerAuth")
    }

    def "should configure all required OpenAPI components"() {
        when:
        OpenAPI openAPI = openApiConfig.customOpenAPI()

        then:
        openAPI.info != null
        openAPI.servers != null && !openAPI.servers.isEmpty()
        openAPI.components != null
        openAPI.security != null && !openAPI.security.isEmpty()
    }
}