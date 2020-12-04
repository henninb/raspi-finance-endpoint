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
    protected ParameterRepository parameterRepository

    @Autowired
    protected TestEntityManager entityManager

    void 'find a parameter that does not exist.'() {
        when:
        Optional<Parameter> result = parameterRepository.findByParameterName('does-not-exist')

        then:
        result == Optional.empty()
    }

    void 'test parameter - valid insert'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()

        when:
        Parameter parameterResult = entityManager.persist(parameter)

        then:
        parameterRepository.count() == 1L
        parameterResult.parameterName == parameter.parameterName
        parameterResult.parameterValue == parameter.parameterValue
        0 * _
    }

    void 'test parameter - valid insert and find it'() {
        given:
        Parameter parameter = ParameterBuilder.builder().build()
        entityManager.persist(parameter)

        when:
        Optional<Parameter> result = parameterRepository.findByParameterName(parameter.parameterName)

        then:
        parameterRepository.count() == 1L
        result.get().parameterName == parameter.parameterName
        result.get().parameterValue == parameter.parameterValue
        0 * _
    }
}
