package finance.controllers

import finance.domain.Payment
import finance.services.PaymentService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class PaymentControllerMoreSpec extends Specification {

    def service = Mock(PaymentService)
    def controller = new PaymentController(service)

    void "selectAllPayments returns 200 and list"() {
        when:
        def resp = controller.selectAllPayments()

        then:
        1 * service.findAllPayments() >> [new Payment(paymentId: 1L)]
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    void "updatePayment maps DataIntegrityViolationException to 409"() {
        when:
        controller.updatePayment(1L, new Payment())

        then:
        1 * service.updatePayment(1L, _ as Payment) >> { throw new org.springframework.dao.DataIntegrityViolationException('dup') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    void "updatePayment maps ValidationException to 400"() {
        when:
        controller.updatePayment(2L, new Payment())

        then:
        1 * service.updatePayment(2L, _ as Payment) >> { throw new jakarta.validation.ValidationException('bad') }
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    void "insertPayment legacy returns 201 and maps DI violations to 409"() {
        given:
        def p = new Payment(paymentId: 0L)

        when:
        def resp = controller.insertPayment(p)

        then:
        1 * service.insertPayment(p) >> new Payment(paymentId: 3L)
        resp.statusCode == HttpStatus.CREATED

        when:
        controller.insertPayment(p)

        then:
        1 * service.insertPayment(p) >> { throw new org.springframework.dao.DataIntegrityViolationException('dup') }
        thrown(ResponseStatusException)
    }

    void "deleteByPaymentId legacy returns 404 when missing then 200 on success"() {
        when:
        controller.deleteByPaymentId(7L)

        then:
        1 * service.findByPaymentId(7L) >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.deleteByPaymentId(8L)

        then:
        1 * service.findByPaymentId(8L) >> Optional.of(new Payment(paymentId: 8L))
        1 * service.deleteByPaymentId(8L) >> true
        resp.statusCode == HttpStatus.OK
    }

    void "standardized findById returns 404 when missing then 200 when found"() {
        when:
        controller.findById(5L)

        then:
        1 * service.findByPaymentId(5L) >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.findById(6L)

        then:
        1 * service.findByPaymentId(6L) >> Optional.of(new Payment(paymentId: 6L))
        resp.statusCode == HttpStatus.OK
        resp.body.paymentId == 6L
    }
}

