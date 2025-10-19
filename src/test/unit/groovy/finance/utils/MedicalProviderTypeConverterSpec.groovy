package finance.utils

import finance.domain.MedicalProviderType
import spock.lang.Specification

class MedicalProviderTypeConverterSpec extends Specification {
    def converter = new MedicalProviderTypeConverter()

    def "convertToEntityAttribute maps common provider types"() {
        expect:
        converter.convertToEntityAttribute(input) == expected

        where:
        input               || expected
        'general'           || MedicalProviderType.General
        'SPECIALIST'        || MedicalProviderType.Specialist
        ' hospital '        || MedicalProviderType.Hospital
        'Pharmacy'          || MedicalProviderType.Pharmacy
        'laboratory'        || MedicalProviderType.Laboratory
        'imaging'           || MedicalProviderType.Imaging
        'urgent_care'       || MedicalProviderType.UrgentCare
        'emergency'         || MedicalProviderType.Emergency
        'mental_health'     || MedicalProviderType.MentalHealth
        'dental'            || MedicalProviderType.Dental
        'vision'            || MedicalProviderType.Vision
        'physical_therapy'  || MedicalProviderType.PhysicalTherapy
        'other'             || MedicalProviderType.Other
    }

    def "convertToDatabaseColumn returns enum label"() {
        expect:
        converter.convertToDatabaseColumn(mpt) == mpt.getLabel()

        where:
        mpt << MedicalProviderType.values()
    }

    def "invalid provider type throws RuntimeException"() {
        when:
            converter.convertToEntityAttribute('unknown_provider')

        then:
            def ex = thrown(RuntimeException)
            ex.message == 'Unknown medical provider type attribute: unknown_provider'
    }

    def "round trip conversion works for common MedicalProviderType values"() {
        expect:
        [
            MedicalProviderType.General,
            MedicalProviderType.Specialist,
            MedicalProviderType.Hospital,
            MedicalProviderType.Pharmacy,
            MedicalProviderType.Laboratory,
            MedicalProviderType.Imaging,
            MedicalProviderType.UrgentCare,
            MedicalProviderType.Emergency,
            MedicalProviderType.MentalHealth,
            MedicalProviderType.Dental,
            MedicalProviderType.Vision,
            MedicalProviderType.PhysicalTherapy,
            MedicalProviderType.Other
        ].every { original ->
            String db = converter.convertToDatabaseColumn(original)
            converter.convertToEntityAttribute(db) == original
        }
    }
}
