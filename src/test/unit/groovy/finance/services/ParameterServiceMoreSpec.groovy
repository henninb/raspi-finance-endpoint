package finance.services

import finance.domain.Parameter
import finance.helpers.ParameterBuilder

class ParameterServiceMoreSpec extends BaseServiceSpec {

    void setup() {
        parameterService.validator = validatorMock
        parameterService.meterService = meterService
    }

    void "selectAll returns empty when none"() {
        when:
        def list = parameterService.selectAll()

        then:
        1 * parameterRepositoryMock.findByActiveStatusIsTrue() >> []
        list.isEmpty()
    }

    void "selectAll returns list when present"() {
        given:
        def p = ParameterBuilder.builder().build()

        when:
        def list = parameterService.selectAll()

        then:
        1 * parameterRepositoryMock.findByActiveStatusIsTrue() >> [p]
        list.size() == 1
        list[0].parameterName == p.parameterName
    }

    void "findByParameterName returns Optional present and empty"() {
        given:
        def name = 'payment_account'
        def p = ParameterBuilder.builder().withParameterName(name).build()

        when:
        def present = parameterService.findByParameterName(name)
        def missing = parameterService.findByParameterName('missing')

        then:
        1 * parameterRepositoryMock.findByParameterName(name) >> Optional.of(p)
        1 * parameterRepositoryMock.findByParameterName('missing') >> Optional.empty()
        present.isPresent()
        missing.isEmpty()
    }

    void "deleteByParameterName success returns true"() {
        given:
        def name = 'to-delete'
        def p = new Parameter(parameterId: 7L, parameterName: name, parameterValue: 'x')

        when:
        def result = parameterService.deleteByParameterName(name)

        then:
        1 * parameterRepositoryMock.findByParameterName(name) >> Optional.of(p)
        1 * parameterRepositoryMock.delete(p)
        result
    }

    void "updateParameter updates fields and saves"() {
        given:
        def existing = new Parameter(parameterId: 9L, parameterName: 'old', parameterValue: 'v1', activeStatus: true)
        def incoming = new Parameter(parameterId: 9L, parameterName: 'new', parameterValue: 'v2', activeStatus: false)

        when:
        def result = parameterService.updateParameter(incoming)

        then:
        1 * parameterRepositoryMock.findByParameterId(9L) >> Optional.of(existing)
        1 * parameterRepositoryMock.saveAndFlush({ Parameter p ->
            assert p.parameterId == 9L
            assert p.parameterName == 'new'
            assert p.parameterValue == 'v2'
            assert p.activeStatus == false
            return true
        }) >> { args -> args[0] }
        result.parameterName == 'new'
        result.parameterValue == 'v2'
        !result.activeStatus
    }

    void "updateParameter throws when not found"() {
        given:
        def incoming = new Parameter(parameterId: 10L, parameterName: 'x', parameterValue: 'y', activeStatus: true)

        when:
        parameterService.updateParameter(incoming)

        then:
        1 * parameterRepositoryMock.findByParameterId(10L) >> Optional.empty()
        thrown(RuntimeException)
    }
}

