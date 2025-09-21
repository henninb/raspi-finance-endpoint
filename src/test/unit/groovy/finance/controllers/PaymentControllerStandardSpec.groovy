package finance.controllers

import finance.domain.Payment
import finance.services.IPaymentService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class PaymentControllerStandardSpec extends Specification {

    def service = Mock(IPaymentService)
    def controller = new PaymentController(service)

    void "standardized update returns 404 then 200"() {
        given:
        def p = new Payment(paymentId: 20L)

        when:
        controller.update(20L, p)

        then:
        1 * service.findByPaymentId(20L) >> Optional.empty()
        thrown(ResponseStatusException)

        when:
        def resp = controller.update(21L, new Payment(paymentId: 21L))

        then:
        1 * service.findByPaymentId(21L) >> Optional.of(new Payment(paymentId: 21L))
        1 * service.updatePayment(21L, _ as Payment) >> { args -> new Payment(paymentId: 21L) }
        resp.statusCode == HttpStatus.OK
        resp.body.paymentId == 21L
    }
}

