package finance.controllers

import finance.domain.PendingTransaction
import finance.services.PendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.time.LocalDate

class PendingTransactionControllerSpec extends Specification {

    finance.repositories.PendingTransactionRepository pendingRepo = Mock()
    PendingTransactionService pendingService = new PendingTransactionService(pendingRepo)

    @Subject
    PendingTransactionController controller = new PendingTransactionController(pendingService)

    def setup() {
        def auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("test_owner", "password")
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth)

        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        pendingService.validator = validator
        pendingService.meterService = meterService
    }

    def cleanup() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext()
    }

    // ===== STANDARDIZED ENDPOINTS =====

    def "findAllActive returns list of pending transactions"() {
        given:
        List<PendingTransaction> items = [
                new PendingTransaction(pendingTransactionId: 1L, accountNameOwner: "acct_one", transactionDate: LocalDate.of(2024, 1, 1), description: "desc1", amount: new BigDecimal("10.00"), reviewStatus: "pending", owner: "test_owner"),
                new PendingTransaction(pendingTransactionId: 2L, accountNameOwner: "acct_two", transactionDate: LocalDate.of(2024, 1, 2), description: "desc2", amount: new BigDecimal("20.00"), reviewStatus: "pending", owner: "test_owner")
        ]
        and:
        pendingRepo.findAllByOwner("test_owner") >> items

        when:
        ResponseEntity<List<PendingTransaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].accountNameOwner == "acct_one"
    }

    def "findAllActive returns 500 on service error"() {
        given:
        pendingRepo.findAllByOwner("test_owner") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<PendingTransaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns entity when found"() {
        given:
        long id = 10L
        PendingTransaction pt = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_a", transactionDate: LocalDate.of(2024, 3, 1), description: "alpha", amount: new BigDecimal("33.33"), reviewStatus: "pending", owner: "test_owner")
        and:
        pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.of(pt)

        when:
        ResponseEntity<PendingTransaction> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.pendingTransactionId == id
    }

    def "findById returns 404 when not found"() {
        given:
        long id = 404L
        and:
        pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on service error"() {
        given:
        long id = 500L
        and:
        pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<PendingTransaction> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save creates pending transaction and returns 201"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 4, 1), description: "created", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: "test_owner")
        and:
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { PendingTransaction p -> p.pendingTransactionId = 99L; return p }

        when:
        ResponseEntity<PendingTransaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.pendingTransactionId == 99L
    }

    def "save returns 400 on validation error"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 4, 1), description: "bad", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: "test_owner")
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        pendingService.validator = violatingValidator

        when:
        ResponseEntity<PendingTransaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "save returns 500 on system error"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 4, 1), description: "created", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: "test_owner")
        and:
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<PendingTransaction> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "update modifies existing pending transaction and returns 200"() {
        given:
        long id = 22L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 1, 1), description: "old", amount: new BigDecimal("1.00"), reviewStatus: "pending", owner: "test_owner")
        PendingTransaction patch = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 5, 5), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: "test_owner")
        and:
        1 * pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.of(existing)
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { PendingTransaction p -> p }

        when:
        ResponseEntity<PendingTransaction> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.OK
        response.body.description == "new"
    }

    def "update returns 404 when not found"() {
        given:
        long id = 23L
        and:
        pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.update(id, new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 5, 5), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: "test_owner"))

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 500 on system error"() {
        given:
        long id = 24L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 1, 1), description: "old", amount: new BigDecimal("1.00"), reviewStatus: "pending", owner: "test_owner")
        PendingTransaction patch = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: LocalDate.of(2024, 5, 5), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: "test_owner")
        and:
        1 * pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.of(existing)
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<PendingTransaction> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        long id = 33L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_d", transactionDate: LocalDate.of(2024, 2, 2), description: "to_delete", amount: new BigDecimal("5.00"), reviewStatus: "pending", owner: "test_owner")
        and:
        // find for handleDeleteOperation and again inside service.deleteById path
        2 * pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.of(existing)

        when:
        ResponseEntity<PendingTransaction> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.OK
        response.body.pendingTransactionId == id
    }

    def "deleteById throws 404 when not found"() {
        given:
        long id = 34L
        and:
        pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById throws 500 when delete fails"() {
        given:
        long id = 35L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_d", transactionDate: LocalDate.of(2024, 2, 2), description: "to_delete", amount: new BigDecimal("5.00"), reviewStatus: "pending", owner: "test_owner")
        and:
        2 * pendingRepo.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc("test_owner",id) >> Optional.of(existing)
        pendingRepo.delete(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<PendingTransaction> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== LEGACY ENDPOINTS =====

    def "legacy deleteAllPendingTransactions returns 204"() {
        when:
        ResponseEntity<Void> response = controller.deleteAllPendingTransactions()

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "legacy deleteAllPendingTransactions returns 500 on error"() {
        given:
        pendingRepo.deleteAllByOwner("test_owner") >> { throw new RuntimeException("db") }

        when:
        controller.deleteAllPendingTransactions()

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
