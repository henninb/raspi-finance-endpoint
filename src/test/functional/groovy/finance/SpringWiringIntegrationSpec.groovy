package finance

import finance.Application
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

//@ActiveProfiles("stage")
//@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@ActiveProfiles("local")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringWiringIntegrationSpec extends Specification {

    @Autowired
    ApplicationContext context

    def "test spring wiring"() {
        given:
        def foo = 1
        when:
        def bar = 2

        then:
        foo != bar
    }
}
