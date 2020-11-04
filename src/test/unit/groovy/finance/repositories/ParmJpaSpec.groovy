package finance.repositories

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("unit")
@DataJpaTest
class ParmJpaSpec extends Specification {

    @Autowired
    ParmRepository parmRepository

    @Autowired
    TestEntityManager entityManager

    def setupSpec() {

    }

    def "find a parm that does not exist."() {
        when:
        def result = parmRepository.findByParmName('does-not-exist')

        then:
        result.empty
    }
}
