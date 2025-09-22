package finance.services

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.domain.ServiceResult
import finance.helpers.FamilyMemberBuilder
import finance.repositories.FamilyMemberRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import java.sql.Date

/**
 * TDD Specification for StandardizedFamilyMemberService
 * Tests the FamilyMember service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedFamilyMemberServiceSpec extends BaseServiceSpec {

    def familyMemberRepositoryMock = Mock(FamilyMemberRepository)
    def standardizedFamilyMemberService = new StandardizedFamilyMemberService(familyMemberRepositoryMock)

    void setup() {
        standardizedFamilyMemberService.meterService = meterService
        standardizedFamilyMemberService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with family members when found"() {
        given: "existing active family members"
        def members = [
            FamilyMemberBuilder.builder().withFamilyMemberId(1L).withOwner("owner1").withMemberName("member1").build(),
            FamilyMemberBuilder.builder().withFamilyMemberId(2L).withOwner("owner1").withMemberName("member2").build()
        ]

        when: "finding all active family members"
        def result = standardizedFamilyMemberService.findAllActive()

        then: "should return Success with family members"
        1 * familyMemberRepositoryMock.findByActiveStatusTrue() >> members
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].familyMemberId == 1L
        result.data[0].memberName == "member1"
        result.data[1].familyMemberId == 2L
        result.data[1].memberName == "member2"
        0 * _
    }

    def "findAllActive should return Success with empty list when no family members found"() {
        when: "finding all active family members with none existing"
        def result = standardizedFamilyMemberService.findAllActive()

        then: "should return Success with empty list"
        1 * familyMemberRepositoryMock.findByActiveStatusTrue() >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with family member when found"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "finding by valid ID"
        def result = standardizedFamilyMemberService.findByIdServiceResult(1L)

        then: "should return Success with family member"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        result instanceof ServiceResult.Success
        result.data.familyMemberId == 1L
        0 * _
    }

    def "findById should return NotFound when family member does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedFamilyMemberService.findByIdServiceResult(999L)

        then: "should return NotFound result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        result instanceof ServiceResult.NotFound
        result.message.contains("FamilyMember not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved family member when valid"() {
        given: "valid family member"
        def member = FamilyMemberBuilder.builder().build()
        def savedMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()
        Set<ConstraintViolation<FamilyMember>> noViolations = [] as Set

        when: "saving family member"
        def result = standardizedFamilyMemberService.save(member)

        then: "should return Success with saved family member"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> null
        1 * validatorMock.validate(member) >> noViolations
        1 * familyMemberRepositoryMock.save(member) >> savedMember
        result instanceof ServiceResult.Success
        result.data.familyMemberId == 1L
        0 * _
    }

    def "save should return ValidationError when family member has constraint violations"() {
        given: "invalid family member"
        def member = FamilyMemberBuilder.builder().withOwner("").build()
        ConstraintViolation<FamilyMember> violation = Mock(ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "owner"
        violation.propertyPath >> mockPath
        violation.message >> "must not be blank"
        Set<ConstraintViolation<FamilyMember>> violations = [violation] as Set

        when: "saving invalid family member"
        def result = standardizedFamilyMemberService.save(member)

        then: "should return ValidationError result"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> null
        1 * validatorMock.validate(member) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("must not be blank")
    }

    def "save should return BusinessError when family member already exists"() {
        given: "family member that already exists"
        def member = FamilyMemberBuilder.builder().build()
        def existingMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "saving duplicate family member"
        def result = standardizedFamilyMemberService.save(member)

        then: "should return BusinessError result"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> existingMember
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("family member already exists")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated family member when exists"() {
        given: "existing family member to update"
        def existingMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).withMemberName("old_name").build()
        def updatedMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).withMemberName("new_name").build()

        when: "updating existing family member"
        def result = standardizedFamilyMemberService.update(updatedMember)

        then: "should return Success with updated family member"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> existingMember
        1 * familyMemberRepositoryMock.save(_ as FamilyMember) >> { FamilyMember member ->
            assert member.memberName == "new_name"
            return member
        }
        result instanceof ServiceResult.Success
        result.data.memberName == "new_name"
        0 * _
    }

    def "update should return NotFound when family member does not exist"() {
        given: "family member with non-existent ID"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(999L).build()

        when: "updating non-existent family member"
        def result = standardizedFamilyMemberService.update(member)

        then: "should return NotFound result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        result instanceof ServiceResult.NotFound
        result.message.contains("FamilyMember not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when family member exists"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "soft deleting existing family member"
        def result = standardizedFamilyMemberService.deleteById(1L)

        then: "should return Success"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        1 * familyMemberRepositoryMock.softDeleteByFamilyMemberId(1L) >> 1
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when family member does not exist"() {
        when: "soft deleting non-existent family member"
        def result = standardizedFamilyMemberService.deleteById(999L)

        then: "should return NotFound result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        result instanceof ServiceResult.NotFound
        result.message.contains("FamilyMember not found: 999")
        0 * _
    }

    // ===== TDD Tests for Business Logic Methods =====

    def "findByOwnerServiceResult should return Success with family members when found"() {
        given: "existing family members for owner"
        def members = [FamilyMemberBuilder.builder().withOwner("test_owner").build()]

        when: "finding by owner using ServiceResult"
        def result = standardizedFamilyMemberService.findByOwnerServiceResult("test_owner")

        then: "should return Success with family members"
        1 * familyMemberRepositoryMock.findByOwnerAndActiveStatusTrue("test_owner") >> members
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].owner == "test_owner"
        0 * _
    }

    def "findByOwnerServiceResult should return Success with empty list when no family members found"() {
        when: "finding by owner with no family members using ServiceResult"
        def result = standardizedFamilyMemberService.findByOwnerServiceResult("unknown_owner")

        then: "should return Success with empty list"
        1 * familyMemberRepositoryMock.findByOwnerAndActiveStatusTrue("unknown_owner") >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    def "findByOwnerAndRelationshipServiceResult should return Success with family members when found"() {
        given: "existing family members for owner and relationship"
        def members = [
            FamilyMemberBuilder.builder().withOwner("test_owner").withRelationship(FamilyRelationship.Spouse).build()
        ]

        when: "finding by owner and relationship using ServiceResult"
        def result = standardizedFamilyMemberService.findByOwnerAndRelationshipServiceResult("test_owner", FamilyRelationship.Spouse)

        then: "should return Success with family members"
        1 * familyMemberRepositoryMock.findByOwnerAndRelationshipAndActiveStatusTrue("test_owner", FamilyRelationship.Spouse) >> members
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].relationship == FamilyRelationship.Spouse
        0 * _
    }

    def "updateActiveStatusServiceResult should return Success when family member exists"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "updating active status using ServiceResult"
        def result = standardizedFamilyMemberService.updateActiveStatusServiceResult(1L, false)

        then: "should return Success"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        1 * familyMemberRepositoryMock.updateActiveStatus(1L, false) >> 1
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "updateActiveStatusServiceResult should return NotFound when family member does not exist"() {
        when: "updating active status for non-existent family member using ServiceResult"
        def result = standardizedFamilyMemberService.updateActiveStatusServiceResult(999L, false)

        then: "should return NotFound result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        result instanceof ServiceResult.NotFound
        result.message.contains("FamilyMember not found: 999")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====

    def "findAll should delegate to findAllActive and return data"() {
        given: "existing family members"
        def members = [FamilyMemberBuilder.builder().build()]

        when: "calling legacy findAll method"
        def result = standardizedFamilyMemberService.findAll()

        then: "should return family member list"
        1 * familyMemberRepositoryMock.findByActiveStatusTrue() >> members
        result.size() == 1
        0 * _
    }

    def "findByIdLegacy method should delegate to findById and return data"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "calling legacy findByIdLegacy method"
        def result = standardizedFamilyMemberService.findByIdLegacy(1L)

        then: "should return family member"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        result.familyMemberId == 1L
        0 * _
    }

    def "insertFamilyMember should delegate to save and return data"() {
        given: "valid family member"
        def member = FamilyMemberBuilder.builder().build()
        def savedMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()
        Set<ConstraintViolation<FamilyMember>> noViolations = [] as Set

        when: "calling legacy insertFamilyMember method"
        def result = standardizedFamilyMemberService.insertFamilyMember(member)

        then: "should return saved family member"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> null
        1 * validatorMock.validate(member) >> noViolations
        1 * familyMemberRepositoryMock.save(member) >> savedMember
        result.familyMemberId == 1L
        0 * _
    }

    def "updateFamilyMember should delegate to update and return data"() {
        given: "existing family member to update"
        def existingMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).withMemberName("old_name").build()
        def updatedMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).withMemberName("new_name").build()

        when: "calling legacy updateFamilyMember method"
        def result = standardizedFamilyMemberService.updateFamilyMember(updatedMember)

        then: "should return updated family member"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> existingMember
        1 * familyMemberRepositoryMock.save(_ as FamilyMember) >> { FamilyMember member -> return member }
        result.memberName == "new_name"
        0 * _
    }

    def "findByOwner legacy method should return family member list"() {
        given: "existing family members for owner"
        def members = [FamilyMemberBuilder.builder().withOwner("test_owner").build()]

        when: "calling legacy findByOwner method"
        def result = standardizedFamilyMemberService.findByOwner("test_owner")

        then: "should return family member list"
        1 * familyMemberRepositoryMock.findByOwnerAndActiveStatusTrue("test_owner") >> members
        result.size() == 1
        0 * _
    }

    def "findByOwnerAndRelationship legacy method should return family member list"() {
        given: "existing family members for owner and relationship"
        def members = [
            FamilyMemberBuilder.builder().withOwner("test_owner").withRelationship(FamilyRelationship.Child).build()
        ]

        when: "calling legacy findByOwnerAndRelationship method"
        def result = standardizedFamilyMemberService.findByOwnerAndRelationship("test_owner", FamilyRelationship.Child)

        then: "should return family member list"
        1 * familyMemberRepositoryMock.findByOwnerAndRelationshipAndActiveStatusTrue("test_owner", FamilyRelationship.Child) >> members
        result.size() == 1
        0 * _
    }

    def "updateActiveStatus legacy method should return boolean"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "calling legacy updateActiveStatus method"
        def result = standardizedFamilyMemberService.updateActiveStatus(1L, false)

        then: "should return boolean result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        1 * familyMemberRepositoryMock.updateActiveStatus(1L, false) >> 1
        result == true
        0 * _
    }

    def "softDelete legacy method should return boolean"() {
        given: "existing family member"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "calling legacy softDelete method"
        def result = standardizedFamilyMemberService.softDelete(1L)

        then: "should return boolean result"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(1L) >> member
        1 * familyMemberRepositoryMock.softDeleteByFamilyMemberId(1L) >> 1
        result == true
        0 * _
    }

    def "softDelete legacy method should return false when family member does not exist"() {
        when: "soft deleting non-existent family member"
        def result = standardizedFamilyMemberService.softDelete(999L)

        then: "should return false"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        result == false
        0 * _
    }

    // ===== TDD Tests for Error Handling in Legacy Methods =====

    def "insertFamilyMember should throw DataIntegrityViolationException for duplicate family member"() {
        given: "family member that already exists"
        def member = FamilyMemberBuilder.builder().build()
        def existingMember = FamilyMemberBuilder.builder().withFamilyMemberId(1L).build()

        when: "calling legacy insertFamilyMember with duplicate data"
        standardizedFamilyMemberService.insertFamilyMember(member)

        then: "should throw DataIntegrityViolationException"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> existingMember
        thrown(DataIntegrityViolationException)
    }

    def "insertFamilyMember should throw ValidationException for invalid family member"() {
        given: "invalid family member"
        def member = FamilyMemberBuilder.builder().withOwner("").build()
        ConstraintViolation<FamilyMember> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "must not be blank"
        Set<ConstraintViolation<FamilyMember>> violations = [violation] as Set

        when: "calling legacy insertFamilyMember with invalid data"
        standardizedFamilyMemberService.insertFamilyMember(member)

        then: "should throw ValidationException"
        1 * familyMemberRepositoryMock.findByOwnerAndMemberName(member.owner, member.memberName) >> null
        1 * validatorMock.validate(member) >> { throw new ConstraintViolationException("Validation failed", violations) }
        thrown(jakarta.validation.ValidationException)
    }

    def "updateFamilyMember should throw IllegalArgumentException when family member not found"() {
        given: "family member with non-existent ID"
        def member = FamilyMemberBuilder.builder().withFamilyMemberId(999L).build()

        when: "calling legacy updateFamilyMember with non-existent family member"
        standardizedFamilyMemberService.updateFamilyMember(member)

        then: "should throw IllegalArgumentException"
        1 * familyMemberRepositoryMock.findByFamilyMemberIdAndActiveStatusTrue(999L) >> null
        thrown(IllegalArgumentException)
        0 * _
    }
}