package finance.controllers

import finance.domain.FamilyMember
import finance.domain.FamilyRelationship
import finance.services.FamilyMemberService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedFamilyMemberControllerSpec extends Specification {

    finance.repositories.FamilyMemberRepository familyRepo = Mock()
    FamilyMemberService service = new FamilyMemberService(familyRepo)

    @Subject
    FamilyMemberController controller = new FamilyMemberController(service)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        service.validator = validator
        service.meterService = meterService
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
        familyRepo.findByActiveStatusTrue() >> list

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive returns empty list and 200"() {
        given:
        familyRepo.findByActiveStatusTrue() >> []

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        familyRepo.findByActiveStatusTrue() >> { throw new RuntimeException("db down") }

        when:
        ResponseEntity<List<FamilyMember>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save creates family member and returns 201"() {
        given:
        FamilyMember input = fm(familyMemberId: 0L, owner: "alice", memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName("alice", "bob") >> null
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
        service.validator = violatingValidator
        familyRepo.findByOwnerAndMemberName(_, _) >> null

        when:
        ResponseEntity<?> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null  // Standardized pattern: no body on error
    }

    def "save returns 409 when duplicate"() {
        given:
        FamilyMember dup = fm(owner: "alice", memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName("alice", "bob") >> fm(owner: "alice", memberName: "bob")

        when:
        ResponseEntity<?> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null  // Standardized pattern: no body on error
    }

    def "save returns 500 on system error"() {
        given:
        FamilyMember input = fm(familyMemberId: 0L, owner: "alice", memberName: "bob")
        and:
        familyRepo.findByOwnerAndMemberName("alice", "bob") >> null
        familyRepo.save(_ as FamilyMember) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== DEFENSIVE PROGRAMMING TESTS =====
    // These tests verify that our defensive else clauses handle unexpected service responses

    def "save handles null service response gracefully"() {
        given:
        FamilyMember input = fm(familyMemberId: 0L, memberName: "test")
        and:
        FamilyMemberService mockService = Mock()
        mockService.save(_ as FamilyMember) >> null
        FamilyMemberController controllerWithMockedService = new FamilyMemberController(mockService)

        when:
        ResponseEntity<?> response = controllerWithMockedService.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findAllActive handles null service response gracefully"() {
        given:
        FamilyMemberService mockService = Mock()
        mockService.findAllActive() >> null
        FamilyMemberController controllerWithMockedService = new FamilyMemberController(mockService)

        when:
        ResponseEntity<List<FamilyMember>> response = controllerWithMockedService.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }
}
