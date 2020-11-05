package finance.repositories

import finance.domain.Category
import finance.domain.Parm
import finance.helpers.CategoryBuilder
import finance.helpers.ParmBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Ignore
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

    def "test parm - valid insert"() {
        given:
        Parm parm = ParmBuilder.builder().build()

        when:
        def parmResult = entityManager.persist(parm)

        then:
        parmRepository.count() == 1L
        parmResult.parmName == parm.parmName
        parmResult.parmValue == parm.parmValue
        0 * _
    }
}
