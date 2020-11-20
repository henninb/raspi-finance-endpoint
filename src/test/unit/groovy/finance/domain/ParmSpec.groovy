package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.ParmBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class ParmSpec extends Specification {

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

    def "test -- JSON serialization to Parm"() {
        given:
        def jsonPayload = """
{"parmName":"foo","parmValue":"bar"}
"""
        when:
        Parm parm = mapper.readValue(jsonPayload, Parm.class)

        then:
        parm.parmName == 'foo'
        parm.parmValue == 'bar'
        0 * _
    }

    def "test JSON deserialization to Parm object - bad non-json"() {

        given:
        def jsonPayloadBad = 'badPayload'

        when:
        mapper.readValue(jsonPayloadBad, Parm.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unrecognized token')
        0 * _
    }

    def "test JSON deserialization to Parm object - bad json"() {

        given:
        def jsonPayloadBad = '{"parmName":"foo","parmValue":"bar",}'

        when:
        mapper.readValue(jsonPayloadBad, Parm.class)

        then:
        JsonParseException ex = thrown()
        ex.getMessage().contains('Unexpected character')
        0 * _
    }

    def "test validation valid parm"() {
        given:
        Parm parm = new ParmBuilder().builder().build()

        when:
        Set<ConstraintViolation<Parm>> violations = validator.validate(parm)

        then:
        violations.isEmpty()
    }
}