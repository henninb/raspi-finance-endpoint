package finance

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("stage")
@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RandomPortSpec extends Specification {

    @Autowired
    ApplicationContext context

    @LocalServerPort
    protected int localPort;

    def "test spring wiring"() {
        given:
        def foo = 1
        when:
        def bar = 2

        then:
        foo != bar
    }

    def "random port test" () {
        when:
        println "localPort = $localPort"
        then:
        localPort > 0
    }
}