package finance.controllers
import finance.configurations.ResilienceComponents

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject

class StandardizedDescriptionControllerSpec extends Specification {

    static final String TEST_OWNER = "test_owner"

    finance.repositories.DescriptionRepository descriptionRepository = Mock()
    finance.repositories.TransactionRepository transactionRepository = Mock()
    jakarta.validation.Validator validator = GroovyMock(jakarta.validation.Validator)
    finance.services.MeterService meterService = new finance.services.MeterService()
    DescriptionService descriptionService = new DescriptionService(descriptionRepository, transactionRepository, meterService, validator, ResilienceComponents.noOp())

    @Subject
    DescriptionController controller = new DescriptionController(descriptionService)

    def setup() {
        validator.validate(_ as Object) >> ([] as Set)
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private static Description desc(Long id = 0L, String name = "alpha", boolean active = true) {
        new Description(descriptionId: id, owner: TEST_OWNER, descriptionName: name, activeStatus: active)
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns list with counts"() {
        given:
        Description d1 = desc(1L, "first")
        Description d2 = desc(2L, "second")
        and:
        descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(TEST_OWNER, true) >> [d1, d2]
        transactionRepository.countByOwnerAndDescriptionNameIn(TEST_OWNER, ["first", "second"]) >> [
            ["first", 4L] as Object[],
            ["second", 2L] as Object[]
        ]

        when:
        ResponseEntity<List<Description>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body*.descriptionCount.containsAll([4L, 2L])
    }

    def "findAllActive returns empty list when none"() {
        given:
        descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(TEST_OWNER, true) >> []

        when:
        ResponseEntity<List<Description>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        descriptionRepository.findByOwnerAndActiveStatusOrderByDescriptionName(TEST_OWNER, true) >> { throw new RuntimeException("db down") }

        when:
        ResponseEntity<List<Description>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns description when found"() {
        given:
        Description d = desc(10L, "target")
        and:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "target") >> Optional.of(d)
        transactionRepository.countByOwnerAndDescriptionName(TEST_OWNER, "target") >> 7L

        when:
        ResponseEntity<Description> response = controller.findById("target")

        then:
        response.statusCode == HttpStatus.OK
        response.body.descriptionId == 10L
        response.body.descriptionCount == 7L
    }

    def "findById returns 404 when missing"() {
        given:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "missing") >> Optional.empty()

        when:
        ResponseEntity<Description> response = controller.findById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "err") >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Description> response = controller.findById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save creates description and returns 201"() {
        given:
        Description input = desc(0L, "newdesc")
        and:
        descriptionRepository.saveAndFlush(_ as Description) >> { Description x -> x.descriptionId = 99L; return x }

        when:
        ResponseEntity<Description> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.descriptionId == 99L
    }

    def "save returns 400 for validation error"() {
        given:
        Description invalid = desc(0L, "")
        and:
        def violatingValidator = GroovyMock(jakarta.validation.Validator)
        violatingValidator.validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        def localService = new DescriptionService(descriptionRepository, transactionRepository, meterService, violatingValidator, ResilienceComponents.noOp())
        def localController = new DescriptionController(localService)

        when:
        ResponseEntity<Description> response = localController.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 409 on duplicate"() {
        given:
        Description dup = desc(0L, "dup")
        and:
        descriptionRepository.saveAndFlush(_ as Description) >> { throw new org.springframework.dao.DataIntegrityViolationException("duplicate") }

        when:
        ResponseEntity<Description> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "save returns 500 on system error"() {
        given:
        Description input = desc(0L, "sys")
        and:
        descriptionRepository.saveAndFlush(_ as Description) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Description> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: update =====
    def "update returns 200 when exists"() {
        given:
        Description existing = desc(5L, "old")
        Description patch = desc(5L, "new")
        and:
        descriptionRepository.findByOwnerAndDescriptionId(TEST_OWNER, 5L) >> Optional.of(existing)
        descriptionRepository.saveAndFlush(_ as Description) >> { Description x -> x }

        when:
        ResponseEntity<Description> response = controller.update("new", patch)

        then:
        response.statusCode == HttpStatus.OK
        response.body.descriptionName == "new"
    }

    def "update returns 404 when missing"() {
        given:
        Description patch = desc(777L, "nope")
        and:
        descriptionRepository.findByOwnerAndDescriptionId(TEST_OWNER, 777L) >> Optional.empty()

        when:
        ResponseEntity<Description> response = controller.update("nope", patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        Description existing = desc(6L, "oldv")
        Description patch = desc(6L, "newv")
        and:
        descriptionRepository.findByOwnerAndDescriptionId(TEST_OWNER, 6L) >> Optional.of(existing)
        descriptionRepository.saveAndFlush(_ as Description) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<Description> response = controller.update("newv", patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 409 on business error"() {
        given:
        Description existing = desc(7L, "oldb")
        Description patch = desc(7L, "newb")
        and:
        descriptionRepository.findByOwnerAndDescriptionId(TEST_OWNER, 7L) >> Optional.of(existing)
        descriptionRepository.saveAndFlush(_ as Description) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Description> response = controller.update("newb", patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "update returns 500 on system error"() {
        given:
        Description existing = desc(8L, "olds")
        Description patch = desc(8L, "news")
        and:
        descriptionRepository.findByOwnerAndDescriptionId(TEST_OWNER, 8L) >> Optional.of(existing)
        descriptionRepository.saveAndFlush(_ as Description) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Description> response = controller.update("news", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: deleteById =====
    def "deleteById returns 200 with deleted description when found"() {
        given:
        Description existing = desc(8L, "gone")
        and:
        1 * descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "gone") >> Optional.of(existing)

        when:
        ResponseEntity<Description> response = controller.deleteById("gone")

        then:
        response.statusCode == HttpStatus.OK
        response.body.descriptionId == 8L
    }

    def "deleteById returns 404 when missing"() {
        given:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "missing") >> Optional.empty()

        when:
        ResponseEntity<Description> response = controller.deleteById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on system error during find"() {
        given:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "errs") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Description> response = controller.deleteById("errs")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 409 on business error during delete"() {
        given:
        Description existing = desc(9L, "delb")
        and:
        1 * descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "delb") >> Optional.of(existing)
        descriptionRepository.delete(_ as Description) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Description> response = controller.deleteById("delb")

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    // ===== findAllActivePaged =====
    def "findAllActivePaged returns page of descriptions"() {
        given:
        Description d1 = desc(1L, "alpha")
        Description d2 = desc(2L, "beta")
        def pageable = org.springframework.data.domain.PageRequest.of(0, 10)
        def page = new org.springframework.data.domain.PageImpl<>([d1, d2], pageable, 2)
        and:
        descriptionRepository.findAllByOwnerAndActiveStatusOrderByDescriptionName(TEST_OWNER, true, _) >> page
        transactionRepository.countByOwnerAndDescriptionNameIn(TEST_OWNER, _) >> []

        when:
        ResponseEntity<?> response = controller.findAllActivePaged(pageable)

        then:
        response.statusCode == HttpStatus.OK
        (response.body as org.springframework.data.domain.Page<Description>).content.size() == 2
    }

    def "findAllActivePaged returns 500 on system error"() {
        given:
        def pageable = org.springframework.data.domain.PageRequest.of(0, 10)
        and:
        descriptionRepository.findAllByOwnerAndActiveStatusOrderByDescriptionName(TEST_OWNER, true, _) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.findAllActivePaged(pageable)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== mergeDescriptions =====
    def "mergeDescriptions returns 200 when valid request"() {
        given:
        Description target = desc(1L, "target")
        Description source = desc(2L, "source")
        def request = new finance.domain.MergeDescriptionsRequest(["source"], "target")
        and:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "target") >> Optional.of(target)
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "source") >> Optional.of(source)
        transactionRepository.bulkUpdateDescriptionByOwner(TEST_OWNER, "source", "target") >> 2
        descriptionRepository.saveAndFlush(_ as Description) >> source

        when:
        ResponseEntity<Description> response = controller.mergeDescriptions(request)

        then:
        response.statusCode == HttpStatus.OK
    }

    def "mergeDescriptions throws 400 when targetName is blank"() {
        given:
        def request = new finance.domain.MergeDescriptionsRequest(["source"], "")

        when:
        controller.mergeDescriptions(request)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == org.springframework.http.HttpStatus.BAD_REQUEST
    }

    def "mergeDescriptions throws 400 when sourceNames is empty"() {
        given:
        def request = new finance.domain.MergeDescriptionsRequest([], "target")

        when:
        controller.mergeDescriptions(request)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == org.springframework.http.HttpStatus.BAD_REQUEST
    }

    def "mergeDescriptions throws 500 when service throws generic exception"() {
        given:
        def request = new finance.domain.MergeDescriptionsRequest(["source"], "target")
        and:
        descriptionRepository.findByOwnerAndDescriptionName(TEST_OWNER, "target") >> { throw new RuntimeException("db error") }

        when:
        controller.mergeDescriptions(request)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Failed to merge descriptions")
    }

}
