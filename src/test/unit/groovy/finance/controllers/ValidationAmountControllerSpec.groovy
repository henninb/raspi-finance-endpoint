package finance.controllers

import finance.domain.ValidationAmount
import finance.domain.TransactionState
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Timestamp

class StandardizedValidationAmountControllerSpec extends Specification {

    finance.repositories.ValidationAmountRepository validationRepo = Mock()
    finance.repositories.AccountRepository accountRepository = Mock()
    ValidationAmountService service = new ValidationAmountService(validationRepo, accountRepository)

    @Subject
    ValidationAmountController controller = new ValidationAmountController(service)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        service.validator = validator
        service.meterService = meterService
    }

    private static ValidationAmount va(Map args = [:]) {
        new ValidationAmount(
            validationId: (args.validationId ?: 0L) as Long,
            accountId: (args.accountId ?: 1L) as Long,
            validationDate: (args.validationDate ?: new Timestamp(System.currentTimeMillis())) as Timestamp,
            activeStatus: (args.activeStatus ?: true) as Boolean,
            transactionState: (args.transactionState ?: TransactionState.Cleared) as TransactionState,
            amount: (args.amount ?: new BigDecimal("1.00")) as BigDecimal
        )
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns list with 200"() {
        given:
        def list = [va(validationId: 1L), va(validationId: 2L)]
        and:
        validationRepo.findByActiveStatusTrueOrderByValidationDateDesc() >> list

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive empty returns 200 with empty list"() {
        given:
        validationRepo.findByActiveStatusTrueOrderByValidationDateDesc() >> []

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 404 when service NotFound"() {
        given:
        validationRepo.findByActiveStatusTrueOrderByValidationDateDesc() >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findAllActive returns 500 on system error"() {
        given:
        validationRepo.findByActiveStatusTrueOrderByValidationDateDesc() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns entity when found"() {
        given:
        long id = 11L
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(va(validationId: id))

        when:
        ResponseEntity<ValidationAmount> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.validationId == id
    }

    def "findById returns 404 when missing"() {
        given:
        long id = 404L
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.empty()

        when:
        ResponseEntity<ValidationAmount> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        long id = 500L
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<ValidationAmount> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save creates validation amount and returns 201"() {
        given:
        ValidationAmount input = va(validationId: 0L)
        and:
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount v -> v.validationId = 77L; return v }

        when:
        ResponseEntity<ValidationAmount> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.validationId == 77L
    }

    def "save returns 400 on validation error"() {
        given:
        ValidationAmount invalid = va(validationId: 0L, amount: new BigDecimal("-1.00"))
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        service.validator = violatingValidator

        when:
        ResponseEntity<ValidationAmount> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 409 on data integrity violation"() {
        given:
        ValidationAmount input = va(validationId: 0L)
        and:
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new org.springframework.dao.DataIntegrityViolationException("duplicate") }

        when:
        ResponseEntity<ValidationAmount> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "save returns 500 on system error"() {
        given:
        ValidationAmount input = va(validationId: 0L)
        and:
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<ValidationAmount> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: update =====
    def "update returns 200 when exists"() {
        given:
        long id = 21L
        ValidationAmount existing = va(validationId: id, amount: new BigDecimal("1.00"))
        ValidationAmount patch = va(validationId: id, amount: new BigDecimal("2.00"))
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount v -> v }

        when:
        ResponseEntity<ValidationAmount> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.OK
        response.body.amount == new BigDecimal("2.00")
    }

    def "update returns 404 when missing"() {
        given:
        long id = 22L
        ValidationAmount patch = va(validationId: id)
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.empty()

        when:
        ResponseEntity<ValidationAmount> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        long id = 23L
        ValidationAmount existing = va(validationId: id)
        ValidationAmount patch = va(validationId: id, amount: new BigDecimal("-1.00"))
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<ValidationAmount> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 409 on business error"() {
        given:
        long id = 24L
        ValidationAmount existing = va(validationId: id)
        ValidationAmount patch = va(validationId: id)
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<ValidationAmount> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "update returns 500 on system error"() {
        given:
        long id = 25L
        ValidationAmount existing = va(validationId: id)
        ValidationAmount patch = va(validationId: id)
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<ValidationAmount> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: deleteById =====
    def "deleteById returns 200 with deleted entity when found"() {
        given:
        long id = 31L
        ValidationAmount existing = va(validationId: id)
        and:
        // First for controller pre-check, second for service delete path
        2 * validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)

        when:
        ResponseEntity<ValidationAmount> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.validationId == id
    }

    def "deleteById returns 404 when missing"() {
        given:
        long id = 32L
        and:
        validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.empty()

        when:
        ResponseEntity<ValidationAmount> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 when delete fails"() {
        given:
        long id = 33L
        ValidationAmount existing = va(validationId: id)
        and:
        2 * validationRepo.findByValidationIdAndActiveStatusTrue(id) >> Optional.of(existing)
        validationRepo.delete(_ as ValidationAmount) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<ValidationAmount> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
