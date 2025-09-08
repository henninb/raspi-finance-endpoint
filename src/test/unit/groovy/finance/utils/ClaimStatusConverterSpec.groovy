package finance.utils

import finance.domain.ClaimStatus
import spock.lang.Specification

class ClaimStatusConverterSpec extends Specification {
    def converter = new ClaimStatusConverter()

    def "convertToEntityAttribute maps all valid statuses"() {
        expect:
        converter.convertToEntityAttribute(input) == expected

        where:
        input          || expected
        'submitted'    || ClaimStatus.Submitted
        ' processing ' || ClaimStatus.Processing
        'APPROVED'     || ClaimStatus.Approved
        'Denied'       || ClaimStatus.Denied
        'paid'         || ClaimStatus.Paid
        'CLOSED'       || ClaimStatus.Closed
    }

    def "convertToDatabaseColumn returns enum label"() {
        expect:
        converter.convertToDatabaseColumn(cs) == cs.getLabel()

        where:
        cs << ClaimStatus.values()
    }

    def "invalid claim status throws RuntimeException"() {
        when:
        converter.convertToEntityAttribute('unknown_status')

        then:
        thrown(RuntimeException)
    }
}

