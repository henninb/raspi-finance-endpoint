package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.IFamilyMemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject
import java.time.LocalDate

/**
 * TDD Specification for FamilyMemberController standardization.
 * This spec defines the requirements for migrating FamilyMemberController to use:
 * - StandardizedBaseController (not BaseController)
 * - StandardRestController<FamilyMember, Long> interface
 * - Dual endpoint support (legacy + standardized)
 * - Standardized exception handling patterns
 * - Standardized method naming conventions
 */
class StandardizedFamilyMemberControllerSpec extends Specification {

    IFamilyMemberService familyMemberService = GroovyMock(IFamilyMemberService)

    @Subject
    FamilyMemberController controller = new FamilyMemberController(familyMemberService)

    // ===== STANDARDIZED CRUD ENDPOINTS =====

    def "findAllActive should return all active family members using standardized pattern"() {
        given:
        List<FamilyMember> members = [
            new FamilyMember(familyMemberId: 1L, owner: "test_owner", memberName: "John Doe", relationship: FamilyRelationship.Self, activeStatus: true),
            new FamilyMember(familyMemberId: 2L, owner: "test_owner", memberName: "Jane Doe", relationship: FamilyRelationship.Spouse, activeStatus: true)
        ]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        1 * familyMemberService.findAll() >> members
        response.statusCode == HttpStatus.OK
        response.body == members
        response.body.size() == 2
    }

    def "findAllActive should return empty list when no active family members exist"() {
        given:
        List<FamilyMember> emptyMembers = []

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        1 * familyMemberService.findAll() >> emptyMembers
        response.statusCode == HttpStatus.OK
        response.body == emptyMembers
        response.body.isEmpty()
    }

    def "findById should return family member when found using standardized pattern"() {
        given:
        Long memberId = 1L
        FamilyMember member = new FamilyMember(familyMemberId: memberId, owner: "test_owner", memberName: "John Doe", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.findById(memberId)

        then:
        1 * familyMemberService.findById(memberId) >> member
        response.statusCode == HttpStatus.OK
        response.body == member
    }

    def "findById should throw 404 when family member not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L

        when:
        controller.findById(nonExistentId)

        then:
        1 * familyMemberService.findById(nonExistentId) >> null
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("Family member not found: 999")
    }

    def "save should create new family member and return 201 CREATED using standardized pattern"() {
        given:
        FamilyMember inputMember = new FamilyMember(familyMemberId: 0L, owner: "new_owner", memberName: "New Member", relationship: FamilyRelationship.Child, activeStatus: true)
        FamilyMember savedMember = new FamilyMember(familyMemberId: 5L, owner: "new_owner", memberName: "New Member", relationship: FamilyRelationship.Child, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.save(inputMember)

        then:
        1 * familyMemberService.insertFamilyMember(inputMember) >> savedMember
        response.statusCode == HttpStatus.CREATED
        response.body == savedMember
        response.body.familyMemberId == 5L
    }

    def "save should handle DataIntegrityViolationException with 409 CONFLICT using standardized pattern"() {
        given:
        FamilyMember duplicateMember = new FamilyMember(familyMemberId: 0L, owner: "test_owner", memberName: "Duplicate", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        controller.save(duplicateMember)

        then:
        1 * familyMemberService.insertFamilyMember(duplicateMember) >> { throw new org.springframework.dao.DataIntegrityViolationException("Duplicate") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "save should handle ValidationException with 400 BAD_REQUEST using standardized pattern"() {
        given:
        FamilyMember invalidMember = new FamilyMember(familyMemberId: 0L, owner: "", memberName: "", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        controller.save(invalidMember)

        then:
        1 * familyMemberService.insertFamilyMember(invalidMember) >> { throw new jakarta.validation.ValidationException("Invalid family member") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Validation error")
    }

    def "update should update existing family member using standardized pattern"() {
        given:
        Long memberId = 3L
        FamilyMember existingMember = new FamilyMember(familyMemberId: memberId, owner: "owner", memberName: "Old Name", relationship: FamilyRelationship.Self, activeStatus: true)
        FamilyMember updateMember = new FamilyMember(familyMemberId: memberId, owner: "owner", memberName: "New Name", relationship: FamilyRelationship.Self, activeStatus: true)
        FamilyMember updatedMember = new FamilyMember(familyMemberId: memberId, owner: "owner", memberName: "New Name", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.update(memberId, updateMember)

        then:
        1 * familyMemberService.findById(memberId) >> existingMember
        1 * familyMemberService.updateFamilyMember(updateMember) >> updatedMember
        response.statusCode == HttpStatus.OK
        response.body == updatedMember
    }

    def "update should throw 404 when family member not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L
        FamilyMember updateMember = new FamilyMember(familyMemberId: nonExistentId, owner: "owner", memberName: "Name", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        controller.update(nonExistentId, updateMember)

        then:
        1 * familyMemberService.findById(nonExistentId) >> null
        0 * familyMemberService.updateFamilyMember(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("Family member not found: 999")
    }

    def "deleteById should delete existing family member and return it using standardized pattern"() {
        given:
        Long memberId = 4L
        FamilyMember existingMember = new FamilyMember(familyMemberId: memberId, owner: "owner", memberName: "To Delete", relationship: FamilyRelationship.Child, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.deleteById(memberId)

        then:
        1 * familyMemberService.findById(memberId) >> existingMember
        1 * familyMemberService.softDelete(memberId) >> true
        response.statusCode == HttpStatus.OK
        response.body == existingMember
    }

    def "deleteById should throw 404 when family member not found using standardized pattern"() {
        given:
        Long nonExistentId = 888L

        when:
        controller.deleteById(nonExistentId)

        then:
        1 * familyMemberService.findById(nonExistentId) >> null
        0 * familyMemberService.softDelete(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("FamilyMember not found: 888")
    }

    // ===== LEGACY ENDPOINT COMPATIBILITY =====

    def "getAll should maintain backward compatibility"() {
        given:
        List<FamilyMember> members = [
            new FamilyMember(familyMemberId: 1L, owner: "legacy_owner", memberName: "Legacy Member", relationship: FamilyRelationship.Self, activeStatus: true)
        ]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.getAll()

        then:
        1 * familyMemberService.findAll() >> members
        response.statusCode == HttpStatus.OK
        response.body == members
    }

    def "getAllWithSuffix should maintain backward compatibility"() {
        given:
        List<FamilyMember> members = [
            new FamilyMember(familyMemberId: 1L, owner: "legacy_owner", memberName: "Legacy Member", relationship: FamilyRelationship.Self, activeStatus: true)
        ]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.getAllWithSuffix()

        then:
        1 * familyMemberService.findAll() >> members
        response.statusCode == HttpStatus.OK
        response.body == members
    }

    def "insert should maintain backward compatibility with legacy insert pattern"() {
        given:
        FamilyMember legacyMember = new FamilyMember(familyMemberId: 0L, owner: "legacy_owner", memberName: "Legacy Member", relationship: FamilyRelationship.Self, activeStatus: true)
        FamilyMember savedMember = new FamilyMember(familyMemberId: 10L, owner: "legacy_owner", memberName: "Legacy Member", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.insert(legacyMember)

        then:
        1 * familyMemberService.insertFamilyMember(legacyMember) >> savedMember
        response.statusCode == HttpStatus.CREATED
        response.body == savedMember
    }

    def "getById should maintain backward compatibility with legacy pattern"() {
        given:
        Long memberId = 6L
        FamilyMember existingMember = new FamilyMember(familyMemberId: memberId, owner: "legacy_owner", memberName: "Legacy Member", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        ResponseEntity<FamilyMember> response = controller.getById(memberId)

        then:
        1 * familyMemberService.findById(memberId) >> existingMember
        response.statusCode == HttpStatus.OK
        response.body == existingMember
    }

    def "byOwner should maintain backward compatibility"() {
        given:
        String owner = "test_owner"
        List<FamilyMember> ownerMembers = [
            new FamilyMember(familyMemberId: 1L, owner: owner, memberName: "Member 1", relationship: FamilyRelationship.Self, activeStatus: true),
            new FamilyMember(familyMemberId: 2L, owner: owner, memberName: "Member 2", relationship: FamilyRelationship.Spouse, activeStatus: true)
        ]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwner(owner)

        then:
        1 * familyMemberService.findByOwner(owner) >> ownerMembers
        response.statusCode == HttpStatus.OK
        response.body == ownerMembers
        response.body.size() == 2
    }

    def "byOwnerAndRelationship should maintain backward compatibility"() {
        given:
        String owner = "test_owner"
        FamilyRelationship relationship = FamilyRelationship.Child
        List<FamilyMember> children = [
            new FamilyMember(familyMemberId: 3L, owner: owner, memberName: "Child 1", relationship: relationship, activeStatus: true),
            new FamilyMember(familyMemberId: 4L, owner: owner, memberName: "Child 2", relationship: relationship, activeStatus: true)
        ]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwnerAndRelationship(owner, relationship)

        then:
        1 * familyMemberService.findByOwnerAndRelationship(owner, relationship) >> children
        response.statusCode == HttpStatus.OK
        response.body == children
        response.body.size() == 2
    }

    def "activateMember should maintain backward compatibility"() {
        given:
        Long memberId = 7L

        when:
        ResponseEntity<Map<String, String>> response = controller.activateMember(memberId)

        then:
        1 * familyMemberService.updateActiveStatus(memberId, true) >> true
        response.statusCode == HttpStatus.OK
        response.body.containsKey("message")
        response.body["message"] == "Family member activated"
    }

    def "deactivateMember should maintain backward compatibility"() {
        given:
        Long memberId = 8L

        when:
        ResponseEntity<Map<String, String>> response = controller.deactivateMember(memberId)

        then:
        1 * familyMemberService.updateActiveStatus(memberId, false) >> true
        response.statusCode == HttpStatus.OK
        response.body.containsKey("message")
        response.body["message"] == "Family member deactivated"
    }

    def "softDelete should maintain backward compatibility"() {
        given:
        Long memberId = 9L

        when:
        ResponseEntity<Map<String, String>> response = controller.softDelete(memberId)

        then:
        1 * familyMemberService.softDelete(memberId) >> true
        response.statusCode == HttpStatus.OK
        response.body.containsKey("message")
        response.body["message"] == "Family member deleted successfully"
    }

    // ===== STANDARDIZATION REQUIREMENTS =====

    def "controller should extend StandardizedBaseController not BaseController"() {
        expect:
        // This test will pass once FamilyMemberController extends StandardizedBaseController
        controller.class.superclass.simpleName == "StandardizedBaseController"
    }

    def "controller should implement StandardRestController interface"() {
        expect:
        // This test will pass once FamilyMemberController implements StandardRestController<FamilyMember, Long>
        StandardRestController.isAssignableFrom(controller.class)
    }

    def "controller should have standardized exception handling patterns"() {
        given:
        FamilyMember errorMember = new FamilyMember(familyMemberId: 0L, owner: "error", memberName: "error", relationship: FamilyRelationship.Self, activeStatus: true)

        when:
        controller.save(errorMember)

        then:
        1 * familyMemberService.insertFamilyMember(errorMember) >> { throw new RuntimeException("Unexpected error") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Unexpected error")
    }
}