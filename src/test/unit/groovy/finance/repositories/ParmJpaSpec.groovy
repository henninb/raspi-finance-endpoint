package finance.repositories

import finance.domain.Parm
import finance.helpers.ParmBuilder
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
        Optional<Parm> result = parmRepository.findByParmName('does-not-exist')

        then:
        result == Optional.empty()
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

    def "test parm - valid insert and find it"() {
        given:
        Parm parm = ParmBuilder.builder().build()
        entityManager.persist(parm)

        when:
        Optional<Parm> result = parmRepository.findByParmName(parm.parmName)

        then:
        parmRepository.count() == 1L
        result.get().parmName == parm.parmName
        result.get().parmValue == parm.parmValue
        0 * _
    }
}
