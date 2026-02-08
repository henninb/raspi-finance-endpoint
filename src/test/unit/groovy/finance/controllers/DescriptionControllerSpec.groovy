package finance.controllers

import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedDescriptionControllerSpec extends Specification {

    finance.repositories.DescriptionRepository descriptionRepository = Mock()
    finance.repositories.TransactionRepository transactionRepository = Mock()
    DescriptionService descriptionService = new DescriptionService(descriptionRepository, transactionRepository)

    @Subject
    DescriptionController controller = new DescriptionController(descriptionService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        descriptionService.validator = validator
        descriptionService.meterService = meterService
    }

    private static Description desc(Long id = 0L, String name = "alpha", boolean active = true) {
        new Description(descriptionId: id, owner: "test_owner", descriptionName: name, activeStatus: active)
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns list with counts"() {
        given:
        Description d1 = desc(1L, "first")
        Description d2 = desc(2L, "second")
        and:
        descriptionRepository.findByActiveStatusOrderByDescriptionName(true) >> [d1, d2]
        transactionRepository.countByDescriptionNameIn(["first", "second"]) >> [
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
        descriptionRepository.findByActiveStatusOrderByDescriptionName(true) >> []

        when:
        ResponseEntity<List<Description>> response = controller.findAllActive()

        then:
        // Controller currently returns 404 on NotFound, but service returns Success with empty list
        // So expect 200 with empty list
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        descriptionRepository.findByActiveStatusOrderByDescriptionName(true) >> { throw new RuntimeException("db down") }

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
        descriptionRepository.findByDescriptionName("target") >> Optional.of(d)
        transactionRepository.countByDescriptionName("target") >> 7L

        when:
        ResponseEntity<Description> response = controller.findById("target")

        then:
        response.statusCode == HttpStatus.OK
        response.body.descriptionId == 10L
        response.body.descriptionCount == 7L
    }

    def "findById returns 404 when missing"() {
        given:
        descriptionRepository.findByDescriptionName("missing") >> Optional.empty()

        when:
        ResponseEntity<Description> response = controller.findById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        descriptionRepository.findByDescriptionName("err") >> { throw new RuntimeException("boom") }

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
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        descriptionService.validator = violatingValidator

        when:
        ResponseEntity<Description> response = controller.save(invalid)

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
        descriptionRepository.findByDescriptionId(5L) >> Optional.of(existing)
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
        descriptionRepository.findByDescriptionId(777L) >> Optional.empty()

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
        descriptionRepository.findByDescriptionId(6L) >> Optional.of(existing)
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
        descriptionRepository.findByDescriptionId(7L) >> Optional.of(existing)
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
        descriptionRepository.findByDescriptionId(8L) >> Optional.of(existing)
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
        2 * descriptionRepository.findByDescriptionName("gone") >> Optional.of(existing)

        when:
        ResponseEntity<Description> response = controller.deleteById("gone")

        then:
        response.statusCode == HttpStatus.OK
        response.body.descriptionId == 8L
    }

    def "deleteById returns 404 when missing"() {
        given:
        descriptionRepository.findByDescriptionName("missing") >> Optional.empty()

        when:
        ResponseEntity<Description> response = controller.deleteById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on system error during find"() {
        given:
        descriptionRepository.findByDescriptionName("errs") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Description> response = controller.deleteById("errs")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 500 on business error during delete"() {
        given:
        Description existing = desc(9L, "delb")
        and:
        2 * descriptionRepository.findByDescriptionName("delb") >> Optional.of(existing)
        descriptionRepository.delete(_ as Description) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Description> response = controller.deleteById("delb")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
