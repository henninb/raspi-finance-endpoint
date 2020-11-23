package finance.repositories

import finance.domain.Parameter
import finance.helpers.ParameterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@ActiveProfiles("unit")
@DataJpaTest
class ParameterJpaSpec extends Specification {

    @Autowired
    ParameterRepository parmRepository

    @Autowired
    TestEntityManager entityManager

    def setupSpec() {
    }

    def "find a parm that does not exist."() {
        when:
        Optional<Parameter> result = parmRepository.findByParameterName('does-not-exist')

        then:
        result == Optional.empty()
    }

    def "test parm - valid insert"() {
        given:
        Parameter parm = ParameterBuilder.builder().build()

        when:
        def parmResult = entityManager.persist(parm)

        then:
        parmRepository.count() == 1L
        parmResult.parameterName == parm.parameterName
        parmResult.parameterValue == parm.parameterValue
        0 * _
    }

    def "test parm - valid insert and find it"() {
        given:
        Parameter parm = ParameterBuilder.builder().build()
        entityManager.persist(parm)

        when:
        Optional<Parameter> result = parmRepository.findByParameterName(parm.parameterName)

        then:
        parmRepository.count() == 1L
        result.get().parameterName == parm.parameterName
        result.get().parameterValue == parm.parameterValue
        0 * _
    }
}
