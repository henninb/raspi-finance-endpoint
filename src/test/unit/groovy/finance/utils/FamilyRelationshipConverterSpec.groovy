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
            def ex = thrown(RuntimeException)
            ex.message == 'Unknown family relationship attribute: cousin'
    }

    def "round trip conversion works for all FamilyRelationship values"() {
        expect:
        FamilyRelationship.values().every { original ->
            String db = converter.convertToDatabaseColumn(original)
            converter.convertToEntityAttribute(db) == original
        }
    }

    def "all FamilyRelationship values convert to non-empty labels"() {
        expect:
        FamilyRelationship.values().every { fr ->
            def label = converter.convertToDatabaseColumn(fr)
            label != null && !label.trim().isEmpty()
        }
    }
}
