package finance.configurations

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import spock.lang.Specification
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class JacksonConfigSpec extends Specification {

    JacksonConfig jacksonConfig = new JacksonConfig()

    def "kotlinModuleCustomizer returns a non-null JsonMapperBuilderCustomizer"() {
        expect:
        jacksonConfig.kotlinModuleCustomizer() != null
    }

    def "customizer registers KotlinModule with NullIsSameAsDefault enabled"() {
        given:
        JsonMapperBuilderCustomizer customizer = jacksonConfig.kotlinModuleCustomizer()
        JsonMapper.Builder builder = JsonMapper.builder()

        when:
        customizer.customize(builder)
        JsonMapper mapper = builder.build()
        def kotlinModules = mapper.registeredModules().findAll { it instanceof KotlinModule }

        then:
        kotlinModules.size() == 1
        ((KotlinModule) kotlinModules.first()).nullIsSameAsDefault
    }

    def "customizer replaces a default KotlinModule found by findAndAddModules"() {
        given: "a builder that has already auto-discovered the default KotlinModule, as Spring Boot does"
        JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules()

        when:
        jacksonConfig.kotlinModuleCustomizer().customize(builder)
        JsonMapper mapper = builder.build()
        def kotlinModules = mapper.registeredModules().findAll { it instanceof KotlinModule }

        then:
        kotlinModules.size() == 1
        ((KotlinModule) kotlinModules.first()).nullIsSameAsDefault
    }
}
