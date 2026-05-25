package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.ParameterBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE

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

    @Unroll
    void 'test Parameter validation invalid #invalidField has error expectedError'() {
        given:
        Parameter parameter = new ParameterBuilder().builder()
                .withParameterName(parameterName)
                .withParameterValue(parameterValue)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<Parameter>> violations = validator.validate(parameter)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == parameter.properties[invalidField]

        where:
        invalidField     | parameterName                                            | parameterValue                                           | activeStatus | expectedError                               | errorCount
        'parameterValue' | 'someName'                                               | 'ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot' | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'parameterName'  | 'ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot' | 'someValue'                                              | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'parameterName'  | ''                                                       | 'someValue'                                              | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'parameterValue' | 'someName'                                               | ''                                                       | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
    }
}
