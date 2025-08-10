package finance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class RandomPortSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    @LocalServerPort
    int port

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
    }
}
