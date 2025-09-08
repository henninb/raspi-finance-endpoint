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
        thrown(RuntimeException)
    }
}

