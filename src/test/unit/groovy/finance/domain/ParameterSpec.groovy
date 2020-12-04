package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.ParameterBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class ParameterSpec extends Specification {

    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    void 'test -- JSON serialization to Parameter'() {
        given:
        String jsonPayload = """
{"parameterName":"foo","parameterValue":"bar"}
"""
        when:
        Parameter parm = mapper.readValue(jsonPayload, Parameter)

        then:
        parm.parameterName == 'foo'
        parm.parameterValue == 'bar'
        0 * _
    }

    void 'test JSON deserialization to Parameter object - bad non-json'() {

        given:
        String jsonPayloadBad = 'badPayload'

        when:
        mapper.readValue(jsonPayloadBad, Parameter)

        then:
        JsonParseException ex = thrown()
        ex.message.contains('Unrecognized token')
        0 * _
    }

    void 'test JSON deserialization to Parameter object - bad json'() {

        given:
        String jsonPayloadBad = '{"parameterName":"foo","parameterValue":"bar",}'

        when:
        mapper.readValue(jsonPayloadBad, Parameter)

        then:
        JsonParseException ex = thrown()
        ex.message.contains('Unexpected character')
        0 * _
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
