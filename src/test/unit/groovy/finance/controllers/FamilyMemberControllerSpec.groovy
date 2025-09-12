package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.IFamilyMemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class FamilyMemberControllerSpec extends Specification {

    private IFamilyMemberService familyMemberService = Mock()
    private FamilyMemberController familyMemberController = new FamilyMemberController(familyMemberService)

    def "getAll should return a list of family members"() {
        given:
        def familyMembers = [new FamilyMember(), new FamilyMember()]
        familyMemberService.findAll() >> familyMembers

        when:
        ResponseEntity<List<FamilyMember>> response = familyMemberController.getAll()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "getById should return a family member"() {
        given:
        def familyMember = new FamilyMember()
        familyMemberService.findById(1L) >> familyMember

        when:
        ResponseEntity<FamilyMember> response = familyMemberController.getById(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body == familyMember
    }

    def "getById should return 404 when family member not found"() {
        given:
        familyMemberService.findById(1L) >> null

        when:
        ResponseEntity<FamilyMember> response = familyMemberController.getById(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "insert should create a new family member"() {
        given:
        def familyMember = new FamilyMember().with {
            familyMemberId = 1L
            memberName = 'test'
            owner = 'test'
            relationship = FamilyRelationship.Child
            return it
        }
        familyMemberService.insertFamilyMember(_) >> familyMember

        when:
        ResponseEntity<FamilyMember> response = familyMemberController.insert(familyMember)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body == familyMember
    }

    def "byOwner should return family members for a given owner"() {
        given:
        def familyMembers = [new FamilyMember(), new FamilyMember()]
        familyMemberService.findByOwner("test") >> familyMembers

        when:
        ResponseEntity<List<FamilyMember>> response = familyMemberController.byOwner("test")

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "byOwnerAndRelationship should return family members for a given owner and relationship"() {
        given:
        def familyMember = new FamilyMember().with {
            familyMemberId = 1L
            memberName = 'test'
            owner = 'test'
            relationship = FamilyRelationship.Child
            return it
        }
        def familyMembers = [familyMember]
        familyMemberService.findByOwnerAndRelationship("test", FamilyRelationship.Child) >> familyMembers

        when:
        ResponseEntity<List<FamilyMember>> response = familyMemberController.byOwnerAndRelationship("test", FamilyRelationship.Child)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    def "activateMember should activate a family member"() {
        given:
        familyMemberService.updateActiveStatus(1L, true) >> true

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.activateMember(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body["message"] == "Family member activated"
    }

    def "deactivateMember should deactivate a family member"() {
        given:
        familyMemberService.updateActiveStatus(1L, false) >> true

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.deactivateMember(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body["message"] == "Family member deactivated"
    }

    def "softDelete should soft delete a family member"() {
        given:
        familyMemberService.softDelete(1L) >> true

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.softDelete(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body["message"] == "Family member deleted successfully"
    }

    def "activateMember should return 404 when family member not found"() {
        given:
        familyMemberService.updateActiveStatus(1L, true) >> false

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.activateMember(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deactivateMember should return 404 when family member not found"() {
        given:
        familyMemberService.updateActiveStatus(1L, false) >> false

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.deactivateMember(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "softDelete should return 404 when family member not found"() {
        given:
        familyMemberService.softDelete(1L) >> false

        when:
        ResponseEntity<Map<String, String>> response = familyMemberController.softDelete(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }
}