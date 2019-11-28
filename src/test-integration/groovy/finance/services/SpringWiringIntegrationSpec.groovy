package finance.services

import finance.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(classes = Application.class)
@ActiveProfiles("local")
class SpringWiringIntegrationSpec extends Specification {

    @Autowired
    ApplicationContext context

    def "SpringWiring Test"() {
        given:
        def foo = 1
        when:
        def bar = 2

        then:
        foo != bar
    }
}
