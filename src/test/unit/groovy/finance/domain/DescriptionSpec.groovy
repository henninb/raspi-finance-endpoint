package finance.domain

import finance.helpers.DescriptionBuilder
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import static finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE

class DescriptionSpec extends BaseDomainSpec {

    void 'test -- JSON serialization to Description'() {
        given:
        String jsonPayload = '{"descriptionName":"foo","activeStatus":true}'

        when:
        Description description = mapper.readValue(jsonPayload, Description)

        then:
        description.descriptionName == 'foo'
        description.activeStatus == true
    }

    void 'test validation valid description'() {
        given:
        Description description = DescriptionBuilder.builder().withDescription("foobar").build()

        when:
        Set<ConstraintViolation<Description>> violations = validator.validate(description)

        then:
        violations.empty
    }

    @Unroll
    void 'test Description validation invalid #invalidField has error expectedError'() {
        given:
        Description description = DescriptionBuilder.builder()
                .withDescription(descriptionName)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<Description>> violations = validator.validate(description)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)

        where:
        invalidField      | descriptionName                                            | activeStatus | expectedError                               | errorCount
        'descriptionName' | ''                                                         | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
        'descriptionName' | 'ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot-ynot' | true         | FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE | 1
    }

    def "test equals and hashCode"() {
        given:
        Description d1 = DescriptionBuilder.builder().withDescription("foo").build()
        Description d2 = DescriptionBuilder.builder().withDescription("foo").build()
        Description d3 = DescriptionBuilder.builder().withDescription("bar").build()

        expect:
        d1 == d2
        d1.hashCode() == d2.hashCode()
        d1 != d3
        d1 != null
    }

    def "test toString"() {
        given:
        Description d = DescriptionBuilder.builder().withDescription("foo").withActiveStatus(true).build()

        when:
        String result = d.toString()

        then:
        result.contains('"descriptionName":"foo"')
        result.contains('"activeStatus":true')
    }
}
