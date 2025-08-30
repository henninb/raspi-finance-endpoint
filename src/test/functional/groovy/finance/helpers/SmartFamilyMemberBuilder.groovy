package finance.helpers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import groovy.util.logging.Slf4j
import java.sql.Date

@Slf4j
class SmartFamilyMemberBuilder {

    private String owner
    private String memberName
    private FamilyRelationship relationship = FamilyRelationship.Self
    private Date dateOfBirth = null
    private String insuranceMemberId = null
    private String ssnLastFour = null
    private String medicalRecordNumber = null
    private Boolean activeStatus = true

    private SmartFamilyMemberBuilder(String owner) {
        this.owner = owner
        this.memberName = owner // default to self
    }

    static SmartFamilyMemberBuilder builderForOwner(String owner) {
        return new SmartFamilyMemberBuilder(owner)
    }

    SmartFamilyMemberBuilder withMemberName(String name) {
        this.memberName = name
        return this
    }

    SmartFamilyMemberBuilder asRelationship(FamilyRelationship relationship) {
        this.relationship = relationship
        return this
    }

    SmartFamilyMemberBuilder withRelationship(String relationship) {
        if (relationship == null) return this
        try {
            this.relationship = FamilyRelationship.valueOf(relationship.capitalize())
        } catch(Exception ignored) {
            // default remains
        }
        return this
    }

    SmartFamilyMemberBuilder withInsuranceId(String id) {
        this.insuranceMemberId = id
        return this
    }

    SmartFamilyMemberBuilder withSsnLastFour(String lastFour) {
        this.ssnLastFour = lastFour
        return this
    }

    SmartFamilyMemberBuilder withMedicalRecordNumber(String mrn) {
        this.medicalRecordNumber = mrn
        return this
    }

    SmartFamilyMemberBuilder inactive() {
        this.activeStatus = false
        return this
    }

    SmartFamilyMemberBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartFamilyMemberBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    SmartFamilyMemberBuilder withDateOfBirth(Date dob) {
        this.dateOfBirth = dob
        return this
    }

    SmartFamilyMemberBuilder withDateOfBirth(String dob) {
        try {
            this.dateOfBirth = Date.valueOf(dob)
        } catch(Exception ignored) {
            // leave as null to let validation fail in persistence if needed
        }
        return this
    }

    FamilyMember build() {
        new FamilyMember().with {
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
    }
}
