package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import finance.helpers.FamilyMemberBuilder
import spock.lang.Shared
import spock.lang.Unroll

import jakarta.validation.ConstraintViolation

class FamilyMemberSpec extends BaseDomainSpec {

    @Shared
    protected String jsonPayload = '''
{
    "familyMemberId": 0,
    "owner": "test_owner",
    "memberName": "test_member",
    "relationship": "self",
    "dateOfBirth": "1990-01-01",
    "insuranceMemberId": "INS123456",
    "ssnLastFour": "1234",
    "medicalRecordNumber": "MRN789012",
    "activeStatus": true
}
'''

    @Shared
    protected String jsonPayloadInvalidRelationship = '''
{
    "familyMemberId": 0,
    "owner": "test_owner",
    "memberName": "test_member",
    "relationship": "invalid_relationship",
    "activeStatus": true
}
'''

    void 'test JSON deserialization to FamilyMember'() {
        when:
        FamilyMember member = mapper.readValue(jsonPayload, FamilyMember)

        then:
        member.owner == "test_owner"
        member.memberName == "test_member"
        member.relationship == FamilyRelationship.Self
        member.insuranceMemberId == "INS123456"
        member.ssnLastFour == "1234"
        member.activeStatus == true
        0 * _
    }

    void 'test validation valid family member'() {
        given:
        FamilyMember member = FamilyMemberBuilder.builder().build()

        when:
        Set<ConstraintViolation<FamilyMember>> violations = validator.validate(member)

        then:
        violations.empty
        0 * _
    }

    @Unroll
    void 'test -- JSON deserialize to FamilyMember with invalid payload'() {
        when:
        mapper.readValue(payload, FamilyMember)

        then:
        Exception ex = thrown(exceptionThrown)
        ex.message.contains(message)
        0 * _

        where:
        payload                            | exceptionThrown          | message
        'non-jsonPayload'                  | JsonParseException       | 'Unrecognized token'
        '[]'                              | MismatchedInputException | 'Cannot deserialize value of type'
        '{owner: "test"}'                 | JsonParseException       | 'was expecting double-quote to start field name'
        '{"activeStatus": "abc"}'         | InvalidFormatException   | 'Cannot deserialize value of type'
        jsonPayloadInvalidRelationship    | InvalidFormatException   | 'Cannot deserialize value of type'
    }

    @Unroll
    void 'test validation invalid #invalidField has error #expectedError'() {
        given:
        FamilyMember member = new FamilyMemberBuilder()
                .withOwner(owner)
                .withMemberName(memberName)
                .withRelationship(relationship)
                .withInsuranceMemberId(insuranceMemberId)
                .withSsnLastFour(ssnLastFour)
                .withMedicalRecordNumber(medicalRecordNumber)
                .withActiveStatus(activeStatus)
                .build()

        when:
        Set<ConstraintViolation<FamilyMember>> violations = validator.validate(member)

        then:
        violations.size() == errorCount
        violations.message.contains(expectedError)
        violations.iterator().next().invalidValue == member.properties[invalidField]

        where:
        invalidField            | owner        | memberName   | relationship             | insuranceMemberId | ssnLastFour | medicalRecordNumber | activeStatus | expectedError                                     | errorCount
        'owner'                | 'ab'         | 'test_member' | FamilyRelationship.Self  | null             | null        | null                | true         | 'Owner must be between 3 and 100 characters'     | 1
        'owner'                | 'a' * 101    | 'test_member' | FamilyRelationship.Self  | null             | null        | null                | true         | 'Owner must be between 3 and 100 characters'     | 1
        'memberName'           | 'test_owner' | ''            | FamilyRelationship.Self  | null             | null        | null                | true         | 'Member name must be between 1 and 100 characters' | 1
        'memberName'           | 'test_owner' | 'a' * 101     | FamilyRelationship.Self  | null             | null        | null                | true         | 'Member name must be between 1 and 100 characters' | 1
        'insuranceMemberId'    | 'test_owner' | 'test_member' | FamilyRelationship.Self  | 'a' * 51         | null        | null                | true         | 'Insurance member ID must be 50 characters or less' | 1
        'ssnLastFour'          | 'test_owner' | 'test_member' | FamilyRelationship.Self  | null             | '123'       | null                | true         | 'SSN last four must be exactly 4 digits'         | 1
        'ssnLastFour'          | 'test_owner' | 'test_member' | FamilyRelationship.Self  | null             | '12345'     | null                | true         | 'SSN last four must be exactly 4 digits'         | 1
        'ssnLastFour'          | 'test_owner' | 'test_member' | FamilyRelationship.Self  | null             | '123a'      | null                | true         | 'SSN last four must be exactly 4 digits'         | 1
        'medicalRecordNumber'  | 'test_owner' | 'test_member' | FamilyRelationship.Self  | null             | null        | 'a' * 51            | true         | 'Medical record number must be 50 characters or less' | 1
    }

    void 'test FamilyMember toString returns valid JSON'() {
        given:
        FamilyMember member = FamilyMemberBuilder.builder()
                .withOwner("test_owner")
                .withMemberName("test_member")
                .withRelationship(FamilyRelationship.Child)
                .build()

        when:
        String json = member.toString()
        FamilyMember parsed = mapper.readValue(json, FamilyMember)

        then:
        parsed.owner == "test_owner"
        parsed.memberName == "test_member"
        parsed.relationship == FamilyRelationship.Child
        0 * _
    }

    void 'test FamilyMember default constructor'() {
        when:
        FamilyMember member = new FamilyMember()

        then:
        member.familyMemberId == 0L
        member.owner == ""
        member.memberName == ""
        member.relationship == FamilyRelationship.Self
        member.dateOfBirth == null
        member.insuranceMemberId == null
        member.ssnLastFour == null
        member.medicalRecordNumber == null
        member.activeStatus == true
        0 * _
    }

    @Unroll
    void 'test family relationships are properly supported #relationship'() {
        given:
        FamilyMember member = FamilyMemberBuilder.builder()
                .withRelationship(relationship)
                .build()

        when:
        Set<ConstraintViolation<FamilyMember>> violations = validator.validate(member)

        then:
        violations.empty
        member.relationship == relationship
        0 * _

        where:
        relationship << [
            FamilyRelationship.Self,
            FamilyRelationship.Spouse,
            FamilyRelationship.Child,
            FamilyRelationship.Dependent,
            FamilyRelationship.Other
        ]
    }
}