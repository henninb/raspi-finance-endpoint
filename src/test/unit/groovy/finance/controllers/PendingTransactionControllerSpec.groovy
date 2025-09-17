package finance.controllers

import finance.domain.PendingTransaction
import finance.services.PendingTransactionService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class PendingTransactionControllerSpec extends Specification {

    def service = Mock(PendingTransactionService)
    def controller = new PendingTransactionController(service)

    void "findAllActive returns list and 200"() {
        when:
        def resp = controller.findAllActive()

        then:
        1 * service.getAllPendingTransactions() >> [new PendingTransaction(description: 'x')]
        resp.statusCode == HttpStatus.OK
        resp.body.size() == 1
    }

    void "findById not found throws 404"() {
        when:
        controller.findById(99L)

        then:
        1 * service.findByPendingTransactionId(99L) >> Optional.empty()
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    void "findById success returns 200 and body"() {
        given:
        def p = new PendingTransaction(pendingTransactionId: 7L, description: 'z')

        when:
        def resp = controller.findById(7L)

        then:
        1 * service.findByPendingTransactionId(7L) >> Optional.of(p)
        resp.statusCode == HttpStatus.OK
        resp.body.pendingTransactionId == 7L
    }

    void "save creates and returns 201"() {
        given:
        def p = new PendingTransaction(description: 'y')
        def saved = new PendingTransaction(pendingTransactionId: 1L, description: 'y')

        when:
        def resp = controller.save(p)

        then:
        1 * service.insertPendingTransaction(p) >> saved
        resp.statusCode == HttpStatus.CREATED
        resp.body.pendingTransactionId == 1L
    }
}
