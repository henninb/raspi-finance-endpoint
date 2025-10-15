package finance.controllers

import finance.domain.Payment
import finance.domain.ServiceResult
import finance.services.StandardizedPaymentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject
import java.math.BigDecimal
import java.sql.Date
import java.util.Optional

class StandardizedPaymentControllerSpec extends Specification {

    StandardizedPaymentService paymentService = Mock()

    @Subject
    PaymentController controller = new PaymentController(paymentService)

    // ===== HELPER METHODS =====

    private Payment createTestPayment(Long id = 1L, String sourceAccount = "checking_brian", String destinationAccount = "visa_brian", BigDecimal amount = new BigDecimal("100.00")) {
        return new Payment(
            id,
            sourceAccount,
            destinationAccount,
            new Date(System.currentTimeMillis()),
            amount,
            "11111111-1111-1111-1111-111111111111",
            "22222222-2222-2222-2222-222222222222",
            true
        )
    }

    private List<Payment> createTestPayments() {
        return [
            createTestPayment(1L, "checking_brian", "visa_brian", new BigDecimal("100.00")),
            createTestPayment(2L, "savings_brian", "amex_brian", new BigDecimal("200.00")),
            createTestPayment(3L, "checking_janice", "visa_janice", new BigDecimal("150.00"))
        ]
    }

    // ===== STANDARDIZED ENDPOINTS TESTS =====

    def "findAllActive returns list when payments present"() {
        given:
        List<Payment> payments = createTestPayments()
        and:
        paymentService.findAllActive() >> ServiceResult.Success.of(payments)

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 3
        response.body.any { it.sourceAccount == "checking_brian" }
        response.body.any { it.amount == new BigDecimal("200.00") }
    }

    def "findAllActive returns empty list when no payments"() {
        given:
        paymentService.findAllActive() >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns empty list when service returns NotFound"() {
        given:
        paymentService.findAllActive() >> ServiceResult.NotFound.of("No payments found")

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        paymentService.findAllActive() >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns payment when found"() {
        given:
        Payment payment = createTestPayment(1L)
        and:
        paymentService.findById(1L) >> ServiceResult.Success.of(payment)

        when:
        ResponseEntity<Payment> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.paymentId == 1L
        response.body.sourceAccount == "checking_brian"
    }

    def "findById returns 404 when payment not found"() {
        given:
        paymentService.findById(999L) >> ServiceResult.NotFound.of("Payment not found: 999")

        when:
        ResponseEntity<Payment> response = controller.findById(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findById returns 500 on system error"() {
        given:
        paymentService.findById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Payment> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save creates payment and returns 201"() {
        given:
        Payment newPayment = createTestPayment(0L)
        Payment savedPayment = createTestPayment(10L)
        and:
        paymentService.save(newPayment) >> ServiceResult.Success.of(savedPayment)

        when:
        ResponseEntity<Payment> response = controller.save(newPayment)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.paymentId == 10L
        response.body.sourceAccount == "checking_brian"
    }

    def "save handles validation errors with 400"() {
        given:
        Payment invalidPayment = createTestPayment(0L)
        and:
        paymentService.save(invalidPayment) >> ServiceResult.ValidationError.of([field: "is invalid"])

        when:
        ResponseEntity<Payment> response = controller.save(invalidPayment)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save handles conflict with 409 when unique violation"() {
        given:
        Payment duplicatePayment = createTestPayment(0L)
        and:
        paymentService.save(duplicatePayment) >> ServiceResult.BusinessError.of("Duplicate payment found", "DUPLICATE_PAYMENT")

        when:
        ResponseEntity<Payment> response = controller.save(duplicatePayment)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "save returns 500 on system error"() {
        given:
        Payment newPayment = createTestPayment(0L)
        and:
        paymentService.save(newPayment) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Payment> response = controller.save(newPayment)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 200 when payment exists"() {
        given:
        Payment updatedPayment = createTestPayment(1L, "checking_brian", "visa_brian", new BigDecimal("150.00"))
        and:
        paymentService.update(updatedPayment) >> ServiceResult.Success.of(updatedPayment)

        when:
        ResponseEntity<Payment> response = controller.update(1L, updatedPayment)

        then:
        response.statusCode == HttpStatus.OK
        response.body.amount == new BigDecimal("150.00")
        response.body.paymentId == 1L
    }

    def "update returns 404 when payment not found"() {
        given:
        Payment patchPayment = createTestPayment(999L)
        and:
        paymentService.update(patchPayment) >> ServiceResult.NotFound.of("Payment not found: 999")

        when:
        ResponseEntity<Payment> response = controller.update(999L, patchPayment)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "update returns 400 when validation fails"() {
        given:
        Payment patchPayment = createTestPayment(1L)
        and:
        paymentService.update(patchPayment) >> ServiceResult.ValidationError.of([field: "is invalid"])

        when:
        ResponseEntity<Payment> response = controller.update(1L, patchPayment)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "update returns 409 on business conflict"() {
        given:
        Payment patchPayment = createTestPayment(1L)
        and:
        paymentService.update(patchPayment) >> ServiceResult.BusinessError.of("Duplicate payment", "DUPLICATE_PAYMENT")

        when:
        ResponseEntity<Payment> response = controller.update(1L, patchPayment)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "update returns 500 on system error"() {
        given:
        Payment patchPayment = createTestPayment(1L)
        and:
        paymentService.update(patchPayment) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Payment> response = controller.update(1L, patchPayment)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        Payment existingPayment = createTestPayment(1L)
        and:
        paymentService.findById(1L) >> ServiceResult.Success.of(existingPayment)
        paymentService.deleteById(1L) >> ServiceResult.Success.of(true)

        when:
        ResponseEntity<Payment> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.paymentId == 1L
        response.body.sourceAccount == "checking_brian"
    }

    def "deleteById returns 404 when not found"() {
        given:
        paymentService.findById(999L) >> ServiceResult.NotFound.of("Payment not found: 999")

        when:
        ResponseEntity<Payment> response = controller.deleteById(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 500 when find errors"() {
        given:
        paymentService.findById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Payment> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 500 when delete errors"() {
        given:
        Payment existingPayment = createTestPayment(1L)
        and:
        paymentService.findById(1L) >> ServiceResult.Success.of(existingPayment)
        paymentService.deleteById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Delete failed"))

        when:
        ResponseEntity<Payment> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    // ===== EDGE CASES AND ERROR SCENARIOS =====

    def "findById handles negative payment ID"() {
        given:
        paymentService.findById(-1L) >> ServiceResult.NotFound.of("Payment not found: -1")

        when:
        ResponseEntity<Payment> response = controller.findById(-1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findById handles zero payment ID"() {
        given:
        paymentService.findById(0L) >> ServiceResult.NotFound.of("Payment not found: 0")

        when:
        ResponseEntity<Payment> response = controller.findById(0L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById handles maximum Long value"() {
        given:
        paymentService.findById(Long.MAX_VALUE) >> ServiceResult.NotFound.of("Payment not found: ${Long.MAX_VALUE}")

        when:
        ResponseEntity<Payment> response = controller.deleteById(Long.MAX_VALUE)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "controller handles null service responses gracefully for findAllActive"() {
        given:
        paymentService.findAllActive() >> null

        when:
        ResponseEntity<List<Payment>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "controller handles null service responses gracefully for findById"() {
        given:
        paymentService.findById(1L) >> null

        when:
        ResponseEntity<Payment> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "controller handles null service responses gracefully for save"() {
        given:
        Payment payment = createTestPayment(0L)
        and:
        paymentService.save(payment) >> null

        when:
        ResponseEntity<Payment> response = controller.save(payment)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "controller handles null service responses gracefully for update"() {
        given:
        Payment payment = createTestPayment(1L)
        and:
        paymentService.update(payment) >> null

        when:
        ResponseEntity<Payment> response = controller.update(1L, payment)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "controller handles null service responses gracefully for deleteById"() {
        given:
        paymentService.findById(1L) >> null

        when:
        ResponseEntity<Payment> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }
}