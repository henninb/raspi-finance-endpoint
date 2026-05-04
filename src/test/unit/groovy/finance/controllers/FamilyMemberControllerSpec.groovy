package finance.controllers
import finance.configurations.ResilienceComponents

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.FamilyMemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedFamilyMemberControllerSpec extends Specification {

    private static final String TEST_OWNER = "test_owner"

    finance.repositories.FamilyMemberRepository familyRepo = Mock()
    jakarta.validation.Validator validator = Mock() { validate(_ as Object) >> ([] as Set) }
    finance.services.MeterService meterService = new finance.services.MeterService()
    FamilyMemberService service = new FamilyMemberService(familyRepo, meterService, validator, ResilienceComponents.noOp())

    @Subject
    FamilyMemberController controller = new FamilyMemberController(service)

    def setup() {
        def auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(TEST_OWNER, "password")
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext()
    }

    private static FamilyMember fm(Map args = [:]) {
        new FamilyMember(
            familyMemberId: (args.familyMemberId ?: 0L) as Long,
            owner: (args.owner ?: "owner1") as String,
            memberName: (args.memberName ?: "member1") as String,
            relationship: (args.relationship ?: FamilyRelationship.Self) as FamilyRelationship,
            dateOfBirth: (args.dateOfBirth ?: null) as java.sql.Date,
            insuranceMemberId: (args.insuranceMemberId ?: null) as String,
            ssnLastFour: (args.ssnLastFour ?: null) as String,
            medicalRecordNumber: (args.medicalRecordNumber ?: null) as String,
            activeStatus: (args.activeStatus ?: true) as Boolean
        )
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns list and 200"() {
        given:
        def list = [fm(familyMemberId: 1L), fm(familyMemberId: 2L)]
        and:
        familyRepo.findByOwnerAndActiveStatusTrue(TEST_OWNER) >> list

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive returns empty list and 200"() {
        given:
        familyRepo.findByOwnerAndActiveStatusTrue(TEST_OWNER) >> []

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        familyRepo.findByOwnerAndActiveStatusTrue(TEST_OWNER) >> { throw new RuntimeException("db down") }

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save creates family member and returns 201"() {
        given:
        FamilyMember input = fm(familyMemberId: 0L, owner: TEST_OWNER, memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName(TEST_OWNER, "bob") >> null
        familyRepo.save(_ as FamilyMember) >> { FamilyMember m -> m.familyMemberId = 99L; return m }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        (response.body as FamilyMember).familyMemberId == 99L
    }

    def "save returns 400 on validation errors"() {
        given:
        FamilyMember invalid = fm(owner: "", memberName: "")
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        def localService = new FamilyMemberService(familyRepo, meterService, violatingValidator, ResilienceComponents.noOp())
        def localController = new FamilyMemberController(localService)
        familyRepo.findByOwnerAndMemberName(TEST_OWNER, _) >> null

        when:
        ResponseEntity<?> response = localController.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null  // Standardized pattern: no body on error
    }

    def "save returns 409 when duplicate"() {
        given:
        FamilyMember dup = fm(owner: TEST_OWNER, memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName(TEST_OWNER, "bob") >> fm(owner: TEST_OWNER, memberName: "bob")

        when:
        ResponseEntity<?> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null  // Standardized pattern: no body on error
    }

    def "save returns 500 on system error"() {
        given:
        FamilyMember input = fm(familyMemberId: 0L, owner: TEST_OWNER, memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName(TEST_OWNER, "bob") >> null
        familyRepo.save(_ as FamilyMember) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns 200 when found"() {
        given:
        FamilyMember member = fm(familyMemberId: 10L)
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 10L) >> member

        when:
        ResponseEntity<FamilyMember> response = controller.findById(10L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.familyMemberId == 10L
    }

    def "findById returns 404 when not found"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 11L) >> null

        when:
        ResponseEntity<FamilyMember> response = controller.findById(11L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 12L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<FamilyMember> response = controller.findById(12L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: update =====
    def "update returns 200 on success"() {
        given:
        FamilyMember existing = fm(familyMemberId: 20L, owner: TEST_OWNER, memberName: "alice")
        FamilyMember patch = fm(familyMemberId: 20L, owner: TEST_OWNER, memberName: "alice_updated")
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 20L) >> existing
        familyRepo.save(_ as FamilyMember) >> { FamilyMember m -> m }

        when:
        ResponseEntity<FamilyMember> response = controller.update(20L, patch)

        then:
        response.statusCode == HttpStatus.OK
    }

    def "update returns 404 when not found"() {
        given:
        FamilyMember patch = fm(familyMemberId: 21L, owner: TEST_OWNER, memberName: "nobody")
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 21L) >> null

        when:
        ResponseEntity<FamilyMember> response = controller.update(21L, patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 500 on system error"() {
        given:
        FamilyMember existing = fm(familyMemberId: 22L, owner: TEST_OWNER, memberName: "carol")
        FamilyMember patch = fm(familyMemberId: 22L, owner: TEST_OWNER, memberName: "carol_new")
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 22L) >> existing
        familyRepo.save(_ as FamilyMember) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<FamilyMember> response = controller.update(22L, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: deleteById =====
    def "deleteById returns 200 when found and deleted"() {
        given:
        FamilyMember member = fm(familyMemberId: 30L, owner: TEST_OWNER, memberName: "dave")
        familyRepo.findByOwnerAndFamilyMemberId(TEST_OWNER, 30L) >> member
        familyRepo.softDeleteByOwnerAndFamilyMemberId(TEST_OWNER, 30L) >> 1

        when:
        ResponseEntity<FamilyMember> response = controller.deleteById(30L)

        then:
        response.statusCode == HttpStatus.OK
    }

    def "deleteById returns 404 when not found"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberId(TEST_OWNER, 31L) >> null

        when:
        ResponseEntity<FamilyMember> response = controller.deleteById(31L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    // ===== byOwner =====
    def "byOwner returns 200 with list of family members"() {
        given:
        familyRepo.findByOwnerAndActiveStatusTrue(TEST_OWNER) >> [fm(familyMemberId: 1L), fm(familyMemberId: 2L)]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwner(TEST_OWNER)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "byOwner returns 200 with empty list"() {
        given:
        familyRepo.findByOwnerAndActiveStatusTrue(TEST_OWNER) >> []

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwner(TEST_OWNER)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    // ===== byOwnerAndRelationship =====
    def "byOwnerAndRelationship returns 200 with filtered list"() {
        given:
        familyRepo.findByOwnerAndRelationshipAndActiveStatusTrue(TEST_OWNER, FamilyRelationship.Self) >> [fm(familyMemberId: 1L, relationship: FamilyRelationship.Self)]

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwnerAndRelationship(TEST_OWNER, FamilyRelationship.Self)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    def "byOwnerAndRelationship returns 200 with empty list when none match"() {
        given:
        familyRepo.findByOwnerAndRelationshipAndActiveStatusTrue(TEST_OWNER, FamilyRelationship.Spouse) >> []

        when:
        ResponseEntity<List<FamilyMember>> response = controller.byOwnerAndRelationship(TEST_OWNER, FamilyRelationship.Spouse)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    // ===== activateMember =====
    def "activateMember returns 200 when member exists and is activated"() {
        given:
        FamilyMember member = fm(familyMemberId: 40L, owner: TEST_OWNER)
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 40L) >> member
        familyRepo.updateActiveStatusByOwner(TEST_OWNER, 40L, true) >> 1

        when:
        ResponseEntity<Map<String, String>> response = controller.activateMember(40L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.get("message").contains("activated")
    }

    def "activateMember returns 404 when member not found"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 41L) >> null

        when:
        ResponseEntity<Map<String, String>> response = controller.activateMember(41L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "activateMember returns 404 when repo throws (service catches and returns false)"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 42L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, String>> response = controller.activateMember(42L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "activateMember throws ResponseStatusException when service itself throws"() {
        given:
        FamilyMemberService mockService = Mock()
        mockService.updateActiveStatus(42L, true) >> { throw new RuntimeException("service error") }
        FamilyMemberController ctrl = new FamilyMemberController(mockService)

        when:
        ctrl.activateMember(42L)

        then:
        thrown(org.springframework.web.server.ResponseStatusException)
    }

    // ===== deactivateMember =====
    def "deactivateMember returns 200 when member exists and is deactivated"() {
        given:
        FamilyMember member = fm(familyMemberId: 50L, owner: TEST_OWNER)
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 50L) >> member
        familyRepo.updateActiveStatusByOwner(TEST_OWNER, 50L, false) >> 1

        when:
        ResponseEntity<Map<String, String>> response = controller.deactivateMember(50L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.get("message").contains("deactivated")
    }

    def "deactivateMember returns 404 when member not found"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 51L) >> null

        when:
        ResponseEntity<Map<String, String>> response = controller.deactivateMember(51L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deactivateMember returns 404 when repo throws (service catches and returns false)"() {
        given:
        familyRepo.findByOwnerAndFamilyMemberIdAndActiveStatusTrue(TEST_OWNER, 52L) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Map<String, String>> response = controller.deactivateMember(52L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deactivateMember throws ResponseStatusException when service itself throws"() {
        given:
        FamilyMemberService mockService = Mock()
        mockService.updateActiveStatus(52L, false) >> { throw new RuntimeException("service error") }
        FamilyMemberController ctrl = new FamilyMemberController(mockService)

        when:
        ctrl.deactivateMember(52L)

        then:
        thrown(org.springframework.web.server.ResponseStatusException)
    }
}
