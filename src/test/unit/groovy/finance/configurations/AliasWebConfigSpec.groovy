package finance.configurations

import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration
import spock.lang.Specification

class AliasWebConfigSpec extends Specification {

    AliasWebConfig config
    RequestLoggingInterceptor mockInterceptor

    def setup() {
        config = new AliasWebConfig()
        mockInterceptor = Mock(RequestLoggingInterceptor)
        config.requestLoggingInterceptor = mockInterceptor
    }

    def "should register health endpoint view controller"() {
        given:
        def registry = Mock(ViewControllerRegistry)
        def viewController = Mock(ViewControllerRegistration)

        when:
        config.addViewControllers(registry)

        then:
        1 * registry.addViewController("/health") >> viewController
        1 * viewController.setViewName("forward:/actuator/health")
    }

    def "should register request logging interceptor"() {
        given:
        def registry = Mock(InterceptorRegistry)

        when:
        config.addInterceptors(registry)

        then:
        1 * registry.addInterceptor(mockInterceptor)
    }

    def "should have RequestLoggingInterceptor autowired"() {
        when:
        config.requestLoggingInterceptor = mockInterceptor

        then:
        config.requestLoggingInterceptor == mockInterceptor
    }

    def "should be Spring Configuration"() {
        expect:
        config.class.isAnnotationPresent(org.springframework.context.annotation.Configuration)
    }

    def "should implement WebMvcConfigurer"() {
        expect:
        config instanceof org.springframework.web.servlet.config.annotation.WebMvcConfigurer
    }
}