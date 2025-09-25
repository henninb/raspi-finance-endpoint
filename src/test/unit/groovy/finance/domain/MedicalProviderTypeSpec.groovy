package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import spock.lang.Shared
import spock.lang.Unroll

class MedicalProviderTypeSpec extends BaseDomainSpec {

    @Shared
    protected String jsonPayloadGeneral = '"general"'

    @Shared
    protected String jsonPayloadSpecialist = '"specialist"'

    @Shared
    protected String jsonPayloadInvalid = '"invalid_type"'

    void 'test JSON deserialization to MedicalProviderType'() {
        when:
        MedicalProviderType type = mapper.readValue(jsonPayloadGeneral, MedicalProviderType)

        then:
        type == MedicalProviderType.General
        type.label == "general"
        0 * _
    }

    @Unroll
    void 'test JSON serialization from MedicalProviderType #providerType'() {
        when:
        String json = mapper.writeValueAsString(providerType)

        then:
        json == expectedJson
        0 * _

        where:
        providerType                     | expectedJson
        MedicalProviderType.General      | '"general"'
        MedicalProviderType.Specialist   | '"specialist"'
        MedicalProviderType.Hospital     | '"hospital"'
        MedicalProviderType.Pharmacy     | '"pharmacy"'
        MedicalProviderType.Laboratory   | '"laboratory"'
        MedicalProviderType.Imaging      | '"imaging"'
        MedicalProviderType.UrgentCare   | '"urgent_care"'
        MedicalProviderType.Emergency    | '"emergency"'
        MedicalProviderType.MentalHealth | '"mental_health"'
        MedicalProviderType.Dental       | '"dental"'
        MedicalProviderType.Vision       | '"vision"'
        MedicalProviderType.PhysicalTherapy | '"physical_therapy"'
        MedicalProviderType.Other        | '"other"'
    }

    @Unroll
    void 'test toString returns lowercase name for #providerType'() {
        when:
        String result = (String)(providerType)

        then:
        result == expectedString
        0 * _

        where:
        providerType                     | expectedString
        MedicalProviderType.General      | 'general'
        MedicalProviderType.Specialist   | 'specialist'
        MedicalProviderType.Hospital     | 'hospital'
        MedicalProviderType.Pharmacy     | 'pharmacy'
        MedicalProviderType.Laboratory   | 'laboratory'
        MedicalProviderType.Imaging      | 'imaging'
        MedicalProviderType.UrgentCare   | 'urgentcare'
        MedicalProviderType.Emergency    | 'emergency'
        MedicalProviderType.MentalHealth | 'mentalhealth'
        MedicalProviderType.Dental       | 'dental'
        MedicalProviderType.Vision       | 'vision'
        MedicalProviderType.PhysicalTherapy | 'physicaltherapy'
        MedicalProviderType.Other        | 'other'
    }

    @Unroll
    void 'test label property for #providerType'() {
        expect:
        providerType.label == expectedLabel

        where:
        providerType                     | expectedLabel
        MedicalProviderType.General      | 'general'
        MedicalProviderType.Specialist   | 'specialist'
        MedicalProviderType.Hospital     | 'hospital'
        MedicalProviderType.Pharmacy     | 'pharmacy'
        MedicalProviderType.Laboratory   | 'laboratory'
        MedicalProviderType.Imaging      | 'imaging'
        MedicalProviderType.UrgentCare   | 'urgent_care'
        MedicalProviderType.Emergency    | 'emergency'
        MedicalProviderType.MentalHealth | 'mental_health'
        MedicalProviderType.Dental       | 'dental'
        MedicalProviderType.Vision       | 'vision'
        MedicalProviderType.PhysicalTherapy | 'physical_therapy'
        MedicalProviderType.Other        | 'other'
    }

    void 'test JSON deserialization with invalid provider type throws exception'() {
        when:
        mapper.readValue(jsonPayloadInvalid, MedicalProviderType)

        then:
        InvalidFormatException ex = thrown(InvalidFormatException)
        ex.message.contains('Cannot deserialize value of type')
        0 * _
    }

    void 'test JSON deserialization with malformed JSON throws exception'() {
        when:
        mapper.readValue('invalid-json', MedicalProviderType)

        then:
        JsonParseException ex = thrown(JsonParseException)
        ex.message.contains('Unrecognized token')
        0 * _
    }

    void 'test all enum values are defined'() {
        expect:
        MedicalProviderType.values().length == 13
        MedicalProviderType.values().contains(MedicalProviderType.General)
        MedicalProviderType.values().contains(MedicalProviderType.Specialist)
        MedicalProviderType.values().contains(MedicalProviderType.Hospital)
        MedicalProviderType.values().contains(MedicalProviderType.Pharmacy)
        MedicalProviderType.values().contains(MedicalProviderType.Laboratory)
        MedicalProviderType.values().contains(MedicalProviderType.Imaging)
        MedicalProviderType.values().contains(MedicalProviderType.UrgentCare)
        MedicalProviderType.values().contains(MedicalProviderType.Emergency)
        MedicalProviderType.values().contains(MedicalProviderType.MentalHealth)
        MedicalProviderType.values().contains(MedicalProviderType.Dental)
        MedicalProviderType.values().contains(MedicalProviderType.Vision)
        MedicalProviderType.values().contains(MedicalProviderType.PhysicalTherapy)
        MedicalProviderType.values().contains(MedicalProviderType.Other)
    }
}
