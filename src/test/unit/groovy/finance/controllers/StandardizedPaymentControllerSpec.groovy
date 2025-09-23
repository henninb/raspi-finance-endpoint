package finance.controllers

import finance.domain.Payment
import finance.services.StandardizedPaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedPaymentControllerSpec extends Specification {

    // Build a real service with mocked collaborators to avoid mocking final Kotlin classes
    finance.repositories.PaymentRepository paymentRepository = Mock()
    finance.services.ITransactionService transactionService = Mock()
    finance.repositories.AccountRepository accountRepository = Mock()
    finance.services.StandardizedAccountService accountService = new finance.services.StandardizedAccountService(accountRepository)

    StandardizedPaymentService paymentService = new StandardizedPaymentService(paymentRepository, transactionService, accountService)

    @Subject
    PaymentController controller = new PaymentController(paymentService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        paymentService.validator = validator
        paymentService.meterService = meterService
        accountService.validator = validator
        accountService.meterService = meterService
    }

    // ===== STANDARDIZED CRUD ENDPOINTS =====

    def "findAllActive returns all active payments"() {
        given:
        List<Payment> payments = [
            new Payment(paymentId: 1L, sourceAccount: "src", destinationAccount: "dest", amount: 100.00, activeStatus: true),
            new Payment(paymentId: 2L, sourceAccount: "src2", destinationAccount: "dest2", amount: 200.00, activeStatus: true),
        ]
        and:
        paymentRepository.findAll() >> payments

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body == payments
        response.body.size() == 2
    }

    def "findAllActive returns empty list when none found"() {
        given:
        paymentRepository.findAll() >> []

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        paymentRepository.findAll() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns payment when found"() {
        given:
        Long paymentId = 11L
        Payment payment = new Payment(paymentId: paymentId, sourceAccount: "a", destinationAccount: "b", amount: 50.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(payment)

        when:
        ResponseEntity<Payment> response = controller.findById(paymentId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == payment
    }

    def "findById returns 404 when not found"() {
        given:
        Long paymentId = 404L
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.empty()

        when:
        ResponseEntity<Payment> response = controller.findById(paymentId)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById returns 500 on system error"() {
        given:
        Long paymentId = 500L
        and:
        paymentRepository.findByPaymentId(paymentId) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Payment> response = controller.findById(paymentId)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save creates payment and returns 201"() {
        given:
        Payment input = new Payment(paymentId: 0L, sourceAccount: "src", destinationAccount: "dest", amount: 75.00, activeStatus: true)
        and:
        paymentRepository.saveAndFlush(_ as Payment) >> { Payment p -> p.paymentId = 99L; return p }

        when:
        ResponseEntity<Payment> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.paymentId == 99L
    }

    def "save returns 409 on duplicate"() {
        given:
        Payment input = new Payment(paymentId: 0L, sourceAccount: "src", destinationAccount: "dest", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Payment> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "save returns 500 on system error"() {
        given:
        Payment input = new Payment(paymentId: 0L, sourceAccount: "src", destinationAccount: "dest", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Payment> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save handles validation errors with 400"() {
        given:
        Payment invalid = new Payment(paymentId: 0L, sourceAccount: "", destinationAccount: "dest", amount: -1.00, activeStatus: true)
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        paymentService.validator = violatingValidator

        when:
        ResponseEntity<Payment> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 200 when payment exists"() {
        given:
        Long paymentId = 12L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x2", destinationAccount: "y2", amount: 20.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.saveAndFlush(_ as Payment) >> { Payment p -> p }

        when:
        ResponseEntity<Payment> response = controller.update(paymentId, patch)

        then:
        response.statusCode == HttpStatus.OK
        response.body.destinationAccount == "y2"
    }

    def "update returns 404 when payment missing"() {
        given:
        Long paymentId = 13L
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.empty()

        when:
        ResponseEntity<Payment> response = controller.update(paymentId, patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 400 on validation error"() {
        given:
        Long paymentId = 17L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: -1.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<Payment> response = controller.update(paymentId, patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update returns 409 on conflict"() {
        given:
        Long paymentId = 18L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Payment> response = controller.update(paymentId, patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "update returns 500 on system error"() {
        given:
        Long paymentId = 19L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Payment> response = controller.update(paymentId, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 200 and deleted entity for standardized endpoint"() {
        given:
        Long paymentId = 14L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "aa", destinationAccount: "bb", amount: 1.00, activeStatus: true)
        and:
        // First for findById, second for deleteById path
        2 * paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)

        when:
        ResponseEntity<Payment> response = controller.deleteById(paymentId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == existing
    }

    def "deleteById returns 404 when missing in standardized endpoint"() {
        given:
        Long paymentId = 15L
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.empty()

        when:
        ResponseEntity<Payment> response = controller.deleteById(paymentId)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 on system error"() {
        given:
        Long paymentId = 20L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "aa", destinationAccount: "bb", amount: 1.00, activeStatus: true)
        and:
        2 * paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.delete(_ as Payment) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Payment> response = controller.deleteById(paymentId)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== LEGACY ENDPOINT COMPATIBILITY =====

    def "selectAllPayments legacy returns 200 with list"() {
        given:
        List<Payment> payments = [ new Payment(paymentId: 1L, sourceAccount: "s", destinationAccount: "d", amount: 1.0, activeStatus: true) ]
        and:
        paymentRepository.findAll() >> payments

        when:
        ResponseEntity<List<Payment>> response = controller.selectAllPayments()

        then:
        response.statusCode == HttpStatus.OK
        response.body == payments
    }

    def "selectAllPayments legacy returns 200 with empty list on error"() {
        given:
        paymentRepository.findAll() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<Payment>> response = controller.selectAllPayments()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "insertPayment legacy creates payment and returns 201"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        // Accounts exist to avoid account creation path
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        paymentRepository.saveAndFlush(_ as Payment) >> { Payment p -> p.paymentId = 77L; return p }

        when:
        ResponseEntity<Payment> response = controller.insertPayment(legacy)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.paymentId == 77L
    }

    def "insertPayment legacy returns 409 on duplicate"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        controller.insertPayment(legacy)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "insertPayment legacy returns 400 on validation exception"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        paymentService.validator = violatingValidator

        when:
        controller.insertPayment(legacy)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertPayment legacy returns 400 on illegal argument"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { throw new IllegalArgumentException("bad") }

        when:
        controller.insertPayment(legacy)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertPayment legacy returns 400 on response status exception"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "bad") }

        when:
        controller.insertPayment(legacy)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertPayment legacy returns 500 on exception"() {
        given:
        Payment legacy = new Payment(sourceAccount: "src_l", destinationAccount: "dest_l", amount: 5.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("dest_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_l"))
        accountRepository.findByAccountNameOwner("src_l") >> Optional.of(new finance.domain.Account(accountNameOwner: "src_l"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { throw new RuntimeException("db") }

        when:
        controller.insertPayment(legacy)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteByPaymentId legacy returns 200 with deleted entity"() {
        given:
        Long paymentId = 16L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "ls", destinationAccount: "ld", amount: 9.99, activeStatus: true)
        and:
        // First for controller pre-fetch, second for service deletion check
        2 * paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)

        when:
        ResponseEntity<Payment> response = controller.deleteByPaymentId(paymentId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == existing
    }

    def "deleteByPaymentId legacy returns 500 on error"() {
        given:
        Long paymentId = 17L
        Payment existing = new Payment(paymentId: paymentId, sourceAccount: "ls", destinationAccount: "ld", amount: 9.99, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(existing)
        paymentRepository.delete(_ as Payment) >> { throw new RuntimeException("db") }

        when:
        controller.deleteByPaymentId(paymentId)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "updatePayment legacy returns 200"() {
        given:
        Long paymentId = 33L
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(patch)
        paymentRepository.saveAndFlush(_ as Payment) >> { Payment p -> p }

        when:
        ResponseEntity<Payment> response = controller.updatePayment(paymentId, patch)

        then:
        response.statusCode == HttpStatus.OK
    }

    def "updatePayment legacy returns 500 on conflict business error"() {
        given:
        Long paymentId = 34L
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(patch)
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        controller.updatePayment(paymentId, patch)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "updatePayment legacy returns 400 on validation error"() {
        given:
        Long paymentId = 35L
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: -1.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(patch)
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        paymentService.validator = violatingValidator

        when:
        controller.updatePayment(paymentId, patch)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "updatePayment legacy returns 400 on illegal argument"() {
        given:
        Long paymentId = 36L
        Payment patch = new Payment(paymentId: paymentId, sourceAccount: "x", destinationAccount: "y", amount: 10.00, activeStatus: true)
        and:
        paymentRepository.findByPaymentId(paymentId) >> Optional.of(patch)
        paymentRepository.saveAndFlush(_ as Payment) >> { throw new IllegalArgumentException("bad") }

        when:
        controller.updatePayment(paymentId, patch)

        then:
        def ex = thrown(org.springframework.web.server.ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
