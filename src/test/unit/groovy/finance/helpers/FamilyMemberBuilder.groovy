package finance.helpers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship

import java.sql.Date

class FamilyMemberBuilder {

    Long familyMemberId = 0L
    String owner = 'test_owner'
    String memberName = 'test_member'
    FamilyRelationship relationship = FamilyRelationship.Self
    Date dateOfBirth = Date.valueOf('1990-01-01')
    String insuranceMemberId = 'INS123456'
    String ssnLastFour = '1234'
    String medicalRecordNumber = 'MRN789012'
    Boolean activeStatus = true

    static FamilyMemberBuilder builder() {
        return new FamilyMemberBuilder()
    }

    FamilyMember build() {
        FamilyMember member = new FamilyMember().with {
            familyMemberId = this.familyMemberId
            owner = this.owner
            memberName = this.memberName
            relationship = this.relationship
            dateOfBirth = this.dateOfBirth
            insuranceMemberId = this.insuranceMemberId
            ssnLastFour = this.ssnLastFour
            medicalRecordNumber = this.medicalRecordNumber
            activeStatus = this.activeStatus
            return it
        }
        return member
    }

    FamilyMemberBuilder withFamilyMemberId(Long familyMemberId) {
        this.familyMemberId = familyMemberId
        return this
    }

    FamilyMemberBuilder withOwner(String owner) {
        this.owner = owner
        return this
    }

    FamilyMemberBuilder withMemberName(String memberName) {
        this.memberName = memberName
        return this
    }

    FamilyMemberBuilder withRelationship(FamilyRelationship relationship) {
        this.relationship = relationship
        return this
    }

    FamilyMemberBuilder withDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth
        return this
    }

    FamilyMemberBuilder withInsuranceMemberId(String insuranceMemberId) {
        this.insuranceMemberId = insuranceMemberId
        return this
    }

    FamilyMemberBuilder withSsnLastFour(String ssnLastFour) {
        this.ssnLastFour = ssnLastFour
        return this
    }

    FamilyMemberBuilder withMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber
        return this
    }

    FamilyMemberBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }
}