package finance.repositories

import finance.Application
import finance.domain.Parameter
import finance.helpers.SmartParameterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ActiveProfiles("func")
@DataJpaTest
@ContextConfiguration(classes = [Application])
class ParameterJpaSpec extends Specification {

    String testOwner = "test_${UUID.randomUUID().toString().replace('-', '').substring(0, 8)}"

    @Autowired
    protected ParameterRepository parameterRepository

    @Autowired
    protected TestEntityManager entityManager

    void 'find a parameter that does not exist'() {
        when:
        Optional<Parameter> result = parameterRepository.findByParameterName('does-not-exist')

        then:
        result == Optional.empty()
    }

    void 'test parameter - valid insert'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner).build()
        Long initialCount = parameterRepository.count()

        when:
        Parameter parameterResult = entityManager.persist(parameter)

        then:
        parameterRepository.count() == initialCount + 1L
        parameterResult.parameterName == parameter.parameterName
        parameterResult.parameterValue == parameter.parameterValue
        0 * _
    }

    void 'test parameter - valid insert and find it'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner).build()
        Long initialCount = parameterRepository.count()
        entityManager.persist(parameter)

        when:
        Optional<Parameter> result = parameterRepository.findByParameterName(parameter.parameterName)

        then:
        parameterRepository.count() == initialCount + 1L
        result.get().parameterName == parameter.parameterName
        result.get().parameterValue == parameter.parameterValue
        0 * _
    }
}
