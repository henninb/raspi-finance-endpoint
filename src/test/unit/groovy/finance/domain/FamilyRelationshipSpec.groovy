package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import spock.lang.Shared
import spock.lang.Unroll

class FamilyRelationshipSpec extends BaseDomainSpec {

    @Shared
    protected String jsonPayloadSelf = '"self"'

    @Shared
    protected String jsonPayloadSpouse = '"spouse"'

    @Shared
    protected String jsonPayloadInvalid = '"invalid_relationship"'

    void 'test JSON deserialization to FamilyRelationship'() {
        when:
        FamilyRelationship relationship = mapper.readValue(jsonPayloadSelf, FamilyRelationship)

        then:
        relationship == FamilyRelationship.Self
        relationship.label == "self"
        0 * _
    }

    @Unroll
    void 'test JSON serialization from FamilyRelationship #familyRelationship'() {
        when:
        String json = mapper.writeValueAsString(familyRelationship)

        then:
        json == expectedJson
        0 * _

        where:
        familyRelationship          | expectedJson
        FamilyRelationship.Self     | '"self"'
        FamilyRelationship.Spouse   | '"spouse"'
        FamilyRelationship.Child    | '"child"'
        FamilyRelationship.Dependent| '"dependent"'
        FamilyRelationship.Other    | '"other"'
    }

    @Unroll
    void 'test toString returns lowercase name for #familyRelationship'() {
        when:
        String result = (String)(familyRelationship)

        then:
        result == expectedString
        0 * _

        where:
        familyRelationship          | expectedString
        FamilyRelationship.Self     | 'self'
        FamilyRelationship.Spouse   | 'spouse'
        FamilyRelationship.Child    | 'child'
        FamilyRelationship.Dependent| 'dependent'
        FamilyRelationship.Other    | 'other'
    }

    @Unroll
    void 'test label property for #familyRelationship'() {
        expect:
        familyRelationship.label == expectedLabel

        where:
        familyRelationship          | expectedLabel
        FamilyRelationship.Self     | 'self'
        FamilyRelationship.Spouse   | 'spouse'
        FamilyRelationship.Child    | 'child'
        FamilyRelationship.Dependent| 'dependent'
        FamilyRelationship.Other    | 'other'
    }

    @Unroll
    void 'test JSON deserialization for all valid values #jsonPayload'() {
        when:
        FamilyRelationship relationship = mapper.readValue(jsonPayload, FamilyRelationship)

        then:
        relationship == expectedRelationship
        0 * _

        where:
        jsonPayload        | expectedRelationship
        '"self"'          | FamilyRelationship.Self
        '"spouse"'        | FamilyRelationship.Spouse
        '"child"'         | FamilyRelationship.Child
        '"dependent"'     | FamilyRelationship.Dependent
        '"other"'         | FamilyRelationship.Other
    }

    void 'test JSON deserialization with invalid relationship throws exception'() {
        when:
        mapper.readValue(jsonPayloadInvalid, FamilyRelationship)

        then:
        InvalidFormatException ex = thrown(InvalidFormatException)
        ex.message.contains('Cannot deserialize value of type')
        0 * _
    }

    void 'test JSON deserialization with malformed JSON throws exception'() {
        when:
        mapper.readValue('invalid-json', FamilyRelationship)

        then:
        JsonParseException ex = thrown(JsonParseException)
        ex.message.contains('Unrecognized token')
        0 * _
    }

    void 'test all enum values are defined'() {
        expect:
        FamilyRelationship.values().length == 5
        FamilyRelationship.values().contains(FamilyRelationship.Self)
        FamilyRelationship.values().contains(FamilyRelationship.Spouse)
        FamilyRelationship.values().contains(FamilyRelationship.Child)
        FamilyRelationship.values().contains(FamilyRelationship.Dependent)
        FamilyRelationship.values().contains(FamilyRelationship.Other)
    }

    void 'test enum ordering supports family hierarchy'() {
        expect:
        FamilyRelationship.values()[0] == FamilyRelationship.Self
        FamilyRelationship.values()[1] == FamilyRelationship.Spouse
        FamilyRelationship.values()[2] == FamilyRelationship.Child
        FamilyRelationship.values()[3] == FamilyRelationship.Dependent
        FamilyRelationship.values()[4] == FamilyRelationship.Other
    }
}
