package finance.controllers
import finance.configurations.ResilienceComponents

import finance.domain.ValidationAmount
import finance.domain.TransactionState
import finance.services.ValidationAmountService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Timestamp

class StandardizedValidationAmountControllerSpec extends Specification {

    static final String TEST_OWNER = "test_owner"

    finance.repositories.ValidationAmountRepository validationRepo = Mock()
    finance.repositories.AccountRepository accountRepository = Mock()
    jakarta.validation.Validator validator = Mock() { validate(_ as Object) >> ([] as Set) }
    finance.services.MeterService meterService = new finance.services.MeterService()
    ValidationAmountService service = new ValidationAmountService(validationRepo, accountRepository, meterService, validator, ResilienceComponents.noOp())

    @Subject
    ValidationAmountController controller = new ValidationAmountController(service)

    def setup() {
        // Set up SecurityContext for TenantContext.getCurrentOwner()
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private static ValidationAmount va(Map args = [:]) {
        new ValidationAmount(
            validationId: (args.validationId ?: 0L) as Long,
            owner: (args.owner ?: TEST_OWNER) as String,
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
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> list

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActive empty returns 200 with empty list"() {
        given:
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> []

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 404 when service NotFound"() {
        given:
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findAllActive returns 500 on system error"() {
        given:
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> { throw new RuntimeException("db") }

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(va(validationId: id))

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.empty()

        when:
        ResponseEntity<ValidationAmount> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        long id = 500L
        and:
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> { throw new RuntimeException("boom") }

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
        accountRepository.updateValidationDateForAccountByOwner(_, TEST_OWNER) >> 1

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
        def localService = new ValidationAmountService(validationRepo, accountRepository, meterService, violatingValidator, ResilienceComponents.noOp())
        def localController = new ValidationAmountController(localService)

        when:
        ResponseEntity<ValidationAmount> response = localController.save(invalid)

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount v -> v }
        accountRepository.updateValidationDateForAccountByOwner(_, TEST_OWNER) >> 1

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.empty()

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)
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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)
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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)
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
        1 * validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)

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
        validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.empty()

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
        1 * validationRepo.findByOwnerAndValidationIdAndActiveStatusTrue(TEST_OWNER, id) >> Optional.of(existing)
        validationRepo.delete(_ as ValidationAmount) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<ValidationAmount> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== findAllActiveWithFilters (GET /active with params) =====
    def "findAllActiveWithFilters with accountNameOwner returns filtered list"() {
        given:
        def list = [va(validationId: 1L, accountId: 5L), va(validationId: 2L, accountId: 5L)]
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> list
        def account = new finance.domain.Account(accountId: 5L, accountNameOwner: "checking_primary", owner: TEST_OWNER)
        accountRepository.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(account)

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActiveWithFilters("checking_primary", null)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActiveWithFilters with unknown accountNameOwner returns empty list"() {
        given:
        def list = [va(validationId: 1L)]
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> list
        accountRepository.findByOwnerAndAccountNameOwner(TEST_OWNER, "unknown") >> Optional.empty()

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActiveWithFilters("unknown", null)

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActiveWithFilters with transactionState returns filtered list"() {
        given:
        def list = [va(validationId: 1L, transactionState: TransactionState.Cleared)]
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> list

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActiveWithFilters(null, "Cleared")

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    def "findAllActiveWithFilters with invalid transactionState ignores filter"() {
        given:
        def list = [va(validationId: 1L), va(validationId: 2L)]
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> list

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActiveWithFilters(null, "InvalidState")

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findAllActiveWithFilters with no params delegates to findAllActive"() {
        given:
        validationRepo.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(TEST_OWNER) >> [va(validationId: 1L)]

        when:
        ResponseEntity<List<ValidationAmount>> response = controller.findAllActiveWithFilters(null, null)

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    // ===== insertValidationAmount (legacy POST /insert/{accountNameOwner}) =====
    def "insertValidationAmount returns 200 on success"() {
        given:
        ValidationAmount input = va(validationId: 0L, accountId: 7L)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { ValidationAmount v -> v.validationId = 88L; return v }
        accountRepository.updateValidationDateForAccountByOwner(_, TEST_OWNER) >> 1

        when:
        ResponseEntity resp = controller.insertValidationAmount(input, "checking_primary")

        then:
        resp.statusCode == HttpStatus.OK
    }

    def "insertValidationAmount returns 400 on validation error"() {
        given:
        ValidationAmount input = va(validationId: 0L, accountId: 0L)
        accountRepository.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.empty()

        when:
        ResponseEntity resp = controller.insertValidationAmount(input, "checking_primary")

        then:
        resp.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertValidationAmount returns 500 on unexpected error"() {
        given:
        ValidationAmount input = va(validationId: 0L, accountId: 7L)
        validationRepo.saveAndFlush(_ as ValidationAmount) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity resp = controller.insertValidationAmount(input, "checking_primary")

        then:
        resp.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== selectValidationAmountByAccountId (legacy GET /select/{accountNameOwner}/{state}) =====
    def "selectValidationAmountByAccountId returns 200 when found"() {
        given:
        def account = new finance.domain.Account(accountId: 5L, accountNameOwner: "checking_primary", owner: TEST_OWNER)
        accountRepository.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(account)
        validationRepo.findByOwnerAndTransactionStateAndAccountId(TEST_OWNER, TransactionState.Cleared, 5L) >> [va(validationId: 1L)]

        when:
        ResponseEntity<ValidationAmount> response = controller.selectValidationAmountByAccountId("checking_primary", "Cleared")

        then:
        response.statusCode == HttpStatus.OK
    }

    def "selectValidationAmountByAccountId returns 404 when account not found"() {
        given:
        accountRepository.findByOwnerAndAccountNameOwner(TEST_OWNER, "unknown") >> Optional.empty()

        when:
        def response = controller.selectValidationAmountByAccountId("unknown", "Cleared")

        then:
        thrown(org.springframework.web.server.ResponseStatusException)
    }
}
