package finance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class RandomPortIntSpec extends BaseRestTemplateIntegrationSpec {

    @Autowired
    ApplicationContext applicationContext

    void 'test spring wiring'() {
        given:
        def foo = 1

        when:
        def bar = 2

        then:
        foo != bar
    }

    void 'random port test'() {
        expect:
        applicationContext != null
        port > 0
        baseUrl.startsWith("http://localhost:")
    }
}
