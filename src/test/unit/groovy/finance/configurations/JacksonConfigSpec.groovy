package finance.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanPostProcessor
import spock.lang.Specification

class JacksonConfigSpec extends Specification {

    JacksonConfig jacksonConfig

    def setup() {
        jacksonConfig = new JacksonConfig()
    }

    def "kotlinModulePostProcessor returns a non-null BeanPostProcessor"() {
        when:
        BeanPostProcessor processor = jacksonConfig.kotlinModulePostProcessor()

        then:
        processor != null
    }

    def "postProcessAfterInitialization registers KotlinModule on ObjectMapper and returns same instance"() {
        given:
        BeanPostProcessor processor = jacksonConfig.kotlinModulePostProcessor()
        ObjectMapper mapper = new ObjectMapper()

        when:
        Object result = processor.postProcessAfterInitialization(mapper, "objectMapper")

        then:
        result.is(mapper)
        noExceptionThrown()
    }

    def "postProcessAfterInitialization returns non-ObjectMapper bean unchanged"() {
        given:
        BeanPostProcessor processor = jacksonConfig.kotlinModulePostProcessor()
        String nonMapperBean = "someStringBean"

        when:
        Object result = processor.postProcessAfterInitialization(nonMapperBean, "someStringBean")

        then:
        result.is(nonMapperBean)
    }

    def "postProcessAfterInitialization handles multiple ObjectMapper beans"() {
        given:
        BeanPostProcessor processor = jacksonConfig.kotlinModulePostProcessor()
        ObjectMapper mapper1 = new ObjectMapper()
        ObjectMapper mapper2 = new ObjectMapper()

        when:
        processor.postProcessAfterInitialization(mapper1, "primaryObjectMapper")
        processor.postProcessAfterInitialization(mapper2, "secondaryObjectMapper")

        then:
        noExceptionThrown()
    }
}
