package finance.utils

import finance.domain.FamilyRelationship
import spock.lang.Specification

class FamilyRelationshipConverterSpec extends Specification {
    def converter = new FamilyRelationshipConverter()

    def "convertToEntityAttribute maps valid relationships"() {
        expect:
        converter.convertToEntityAttribute(input) == expected

        where:
        input        || expected
        'self'       || FamilyRelationship.Self
        ' SPOUSE '   || FamilyRelationship.Spouse
        'Child'      || FamilyRelationship.Child
        'dependent'  || FamilyRelationship.Dependent
        'OTHER'      || FamilyRelationship.Other
    }

    def "convertToDatabaseColumn returns enum label"() {
        expect:
        converter.convertToDatabaseColumn(fr) == fr.getLabel()

        where:
        fr << FamilyRelationship.values()
    }

    def "invalid relationship throws RuntimeException"() {
        when:
        converter.convertToEntityAttribute('cousin')

        then:
        thrown(RuntimeException)
    }
}

