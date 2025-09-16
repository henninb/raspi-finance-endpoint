package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

import java.sql.Date
import java.time.LocalDate

class PaymentControllerSpec extends Specification {

    PaymentService paymentService = GroovyMock(PaymentService)

    @Subject
    PaymentController controller = new PaymentController(paymentService)

    Payment sample() {
        return new Payment(
                0L,
                'src_acc',
                'dst_acc',
                Date.valueOf(LocalDate.of(2024, 1, 1)),
                12.34G,
                '123e4567-e89b-12d3-a456-426614174000',
                '123e4567-e89b-12d3-a456-426614174111',
                true
        )
    }

    def "selectAllPayments returns list"() {
        given:
        def list = [sample()]

        when:
        ResponseEntity<List<Payment>> response = controller.selectAllPayments()

        then:
        1 * paymentService.findAllPayments() >> list
        response.statusCode == HttpStatus.OK
        response.body == list
    }

    def "updatePayment returns OK on success"() {
        given:
        Payment updated = sample()
        updated.paymentId = 1L

        when:
        ResponseEntity<Payment> response = controller.updatePayment(1L, sample())

        then:
        1 * paymentService.updatePayment(1L, _) >> updated
        response.statusCode == HttpStatus.OK
        response.body == updated
    }

    def "updatePayment maps validation and conflict errors appropriately"() {
        when:
        controller.updatePayment(1L, sample())

        then:
        1 * paymentService.updatePayment(1L, _) >> { throw new DataIntegrityViolationException('dupe') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "insertPayment success and error mapping"() {
        when:
        ResponseEntity<Payment> response = controller.insertPayment(sample())

        then:
        1 * paymentService.insertPayment(_) >> { Payment p ->
            p.paymentId = 99L
            return p
        }
        response.statusCode == HttpStatus.CREATED
        response.body.paymentId == 99L

        when:
        controller.insertPayment(sample())

        then:
        1 * paymentService.insertPayment(_) >> { throw new DataIntegrityViolationException('dupe') }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "deleteByPaymentId returns OK or NOT_FOUND"() {
        given:
        Payment p = sample(); p.paymentId = 10L

        when:
        ResponseEntity<Payment> response = controller.deleteByPaymentId(10L)

        then:
        1 * paymentService.findByPaymentId(10L) >> Optional.of(p)
        1 * paymentService.deleteByPaymentId(10L)
        response.statusCode == HttpStatus.OK

        when:
        controller.deleteByPaymentId(11L)

        then:
        1 * paymentService.findByPaymentId(11L) >> Optional.empty()
        0 * paymentService.deleteByPaymentId(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Payment not found: 11'
    }
}

