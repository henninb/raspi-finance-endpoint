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

    ValidatorFactory validatorFactory
    Validator validator
    private ObjectMapper mapper = new ObjectMapper()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }

    def "test -- JSON serialization to Parameter"() {
        given:
        def jsonPayload = """
{"parameterName":"foo","parameterValue":"bar"}
"""
        when:
        Parameter parm = mapper.readValue(jsonPayload, Parameter.class)

        then:
        parm.parameterName == 'foo'
        parm.parameterValue == 'bar'
        0 * _
    }

    def "test JSON deserialization to Parameter object - bad non-json"() {

        given:
        def jsonPayloadBad = 'badPayload'

        when:
        mapper.readValue(jsonPayloadBad, Parameter.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unrecognized token')
        0 * _
    }

    def "test JSON deserialization to Parameter object - bad json"() {

        given:
        def jsonPayloadBad = '{"parameterName":"foo","parameterValue":"bar",}'

        when:
        mapper.readValue(jsonPayloadBad, Parameter.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unexpected character')
        0 * _
    }

    def "test validation valid parameter"() {
        given:
        Parameter parameter = new ParameterBuilder().builder().build()

        when:
        Set<ConstraintViolation<Parameter>> violations = validator.validate(parameter)

        then:
        violations.isEmpty()
    }
}