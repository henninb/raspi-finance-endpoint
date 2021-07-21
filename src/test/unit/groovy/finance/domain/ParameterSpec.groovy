package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.ParameterBuilder
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class ParameterSpec extends BaseDomainSpec {

    void 'test -- JSON serialization to Parameter'() {
        given:
        String jsonPayload = '{"parameterName":"foo","parameterValue":"bar"}'

        when:
        Parameter parameter = mapper.readValue(jsonPayload, Parameter)

        then:
        parameter.parameterName == 'foo'
        parameter.parameterValue == 'bar'
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to Parameter with invalid payload'() {
        when:
        mapper.readValue(payload, Parameter)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                     | exceptionThrown          | message
        'non-jsonPayload'           | JsonParseException       | 'Unrecognized token'
        '[]'                        | MismatchedInputException | 'Cannot deserialize value of type'
        '{parameterName: "test"}'   | JsonParseException       | 'was expecting double-quote to start field name'
        '{"parameterName": "123",}' | JsonParseException       | 'was expecting double-quote to start field name'
    }

    void 'test validation valid parameter'() {
        given:
        Parameter parameter = new ParameterBuilder().builder().build()

        when:
        Set<ConstraintViolation<Parameter>> violations = validator.validate(parameter)

        then:
        violations.empty
    }
}
