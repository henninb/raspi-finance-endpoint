package finance.controllers

import finance.domain.PendingTransaction
import finance.services.StandardizedPendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Date

class StandardizedPendingTransactionControllerSpec extends Specification {

    finance.repositories.PendingTransactionRepository pendingRepo = Mock()
    StandardizedPendingTransactionService pendingService = new StandardizedPendingTransactionService(pendingRepo)

    @Subject
    PendingTransactionController controller = new PendingTransactionController(pendingService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        pendingService.validator = validator
        pendingService.meterService = meterService
    }

    // ===== STANDARDIZED ENDPOINTS =====

    def "findAllActive returns list of pending transactions"() {
        given:
        List<PendingTransaction> items = [
                new PendingTransaction(pendingTransactionId: 1L, accountNameOwner: "acct_one", transactionDate: Date.valueOf("2024-01-01"), description: "desc1", amount: new BigDecimal("10.00"), reviewStatus: "pending", owner: null),
                new PendingTransaction(pendingTransactionId: 2L, accountNameOwner: "acct_two", transactionDate: Date.valueOf("2024-01-02"), description: "desc2", amount: new BigDecimal("20.00"), reviewStatus: "pending", owner: null)
        ]
        and:
        pendingRepo.findAll() >> items

        when:
        ResponseEntity<List<PendingTransaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
        response.body[0].accountNameOwner == "acct_one"
    }

    def "findAllActive returns 500 on service error"() {
        given:
        pendingRepo.findAll() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<PendingTransaction>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns entity when found"() {
        given:
        long id = 10L
        PendingTransaction pt = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_a", transactionDate: Date.valueOf("2024-03-01"), description: "alpha", amount: new BigDecimal("33.33"), reviewStatus: "pending", owner: null)
        and:
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(pt)

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
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on service error"() {
        given:
        long id = 500L
        and:
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<PendingTransaction> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save creates pending transaction and returns 201"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-04-01"), description: "created", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: null)
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
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-04-01"), description: "bad", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: null)
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
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-04-01"), description: "created", amount: new BigDecimal("12.34"), reviewStatus: "pending", owner: null)
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
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-01-01"), description: "old", amount: new BigDecimal("1.00"), reviewStatus: "pending", owner: null)
        PendingTransaction patch = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-05-05"), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: null)
        and:
        1 * pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(existing)
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
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.update(id, new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-05-05"), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: null))

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 500 on system error"() {
        given:
        long id = 24L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-01-01"), description: "old", amount: new BigDecimal("1.00"), reviewStatus: "pending", owner: null)
        PendingTransaction patch = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_x", transactionDate: Date.valueOf("2024-05-05"), description: "new", amount: new BigDecimal("2.00"), reviewStatus: "pending", owner: null)
        and:
        1 * pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(existing)
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<PendingTransaction> response = controller.update(id, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        long id = 33L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_d", transactionDate: Date.valueOf("2024-02-02"), description: "to_delete", amount: new BigDecimal("5.00"), reviewStatus: "pending", owner: null)
        and:
        // find for handleDeleteOperation and again inside service.deleteById path
        2 * pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(existing)

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
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.empty()

        when:
        ResponseEntity<PendingTransaction> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById throws 500 when delete fails"() {
        given:
        long id = 35L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_d", transactionDate: Date.valueOf("2024-02-02"), description: "to_delete", amount: new BigDecimal("5.00"), reviewStatus: "pending", owner: null)
        and:
        2 * pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(existing)
        pendingRepo.delete(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<PendingTransaction> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== LEGACY ENDPOINTS =====

    def "legacy getAllPendingTransactions returns 200 when list not empty"() {
        given:
        List<PendingTransaction> items = [ new PendingTransaction(pendingTransactionId: 1L, accountNameOwner: "acct", transactionDate: Date.valueOf("2024-01-01"), description: "x", amount: new BigDecimal("1.00"), reviewStatus: "pending", owner: null) ]
        and:
        pendingRepo.findAll() >> items

        when:
        ResponseEntity<List<PendingTransaction>> response = controller.getAllPendingTransactions()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 1
    }

    def "legacy getAllPendingTransactions throws 404 when empty"() {
        given:
        pendingRepo.findAll() >> []

        when:
        controller.getAllPendingTransactions()

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "legacy getAllPendingTransactions throws 500 on error"() {
        given:
        pendingRepo.findAll() >> { throw new RuntimeException("db") }

        when:
        controller.getAllPendingTransactions()

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "legacy insertPendingTransaction returns 201"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_l", transactionDate: Date.valueOf("2024-06-06"), description: "legacy", amount: new BigDecimal("15.00"), reviewStatus: "pending", owner: null)
        and:
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { PendingTransaction p -> p.pendingTransactionId = 77L; return p }

        when:
        ResponseEntity<PendingTransaction> response = controller.insertPendingTransaction(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.pendingTransactionId == 77L
    }

    def "legacy insertPendingTransaction throws 400 on validation error"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_l", transactionDate: Date.valueOf("2024-06-06"), description: "legacy", amount: new BigDecimal("15.00"), reviewStatus: "pending", owner: null)
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        pendingService.validator = violatingValidator

        when:
        controller.insertPendingTransaction(input)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "legacy insertPendingTransaction throws 500 on error"() {
        given:
        PendingTransaction input = new PendingTransaction(pendingTransactionId: 0L, accountNameOwner: "acct_l", transactionDate: Date.valueOf("2024-06-06"), description: "legacy", amount: new BigDecimal("15.00"), reviewStatus: "pending", owner: null)
        and:
        pendingRepo.saveAndFlush(_ as PendingTransaction) >> { throw new RuntimeException("db") }

        when:
        controller.insertPendingTransaction(input)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "legacy deletePendingTransaction returns 204 when deleted"() {
        given:
        long id = 55L
        PendingTransaction existing = new PendingTransaction(pendingTransactionId: id, accountNameOwner: "acct_z", transactionDate: Date.valueOf("2024-07-07"), description: "legacy_del", amount: new BigDecimal("9.99"), reviewStatus: "pending", owner: null)
        and:
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.of(existing)

        when:
        ResponseEntity<Void> response = controller.deletePendingTransaction(id)

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "legacy deletePendingTransaction maps 404 to 500"() {
        given:
        long id = 56L
        and:
        pendingRepo.findByPendingTransactionIdOrderByTransactionDateDesc(id) >> Optional.empty()

        when:
        controller.deletePendingTransaction(id)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "legacy deleteAllPendingTransactions returns 204"() {
        when:
        ResponseEntity<Void> response = controller.deleteAllPendingTransactions()

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "legacy deleteAllPendingTransactions returns 500 on error"() {
        given:
        pendingRepo.deleteAll() >> { throw new RuntimeException("db") }

        when:
        controller.deleteAllPendingTransactions()

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
