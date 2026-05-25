package finance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("int")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RandomPortSpec extends Specification {

    @Autowired
    protected ApplicationContext applicationContext

    @LocalServerPort
    protected int localPort

    void 'test spring wiring'() {
        given:
        def foo = 1

        when:
        def bar = 2

        then:
        foo != bar
    }

    void 'random port test'() {
        when:
        Boolean x = true

        then:
        x
        localPort > 0
    }
}
