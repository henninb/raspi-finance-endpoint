package finance.controllers

import finance.domain.Totals
import finance.domain.Transaction
import finance.services.MeterService
import finance.services.ITransactionService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class TransactionControllerMoreSpec extends Specification {

    def service = Mock(ITransactionService)
    def meter = GroovyMock(MeterService)
    def controller = new TransactionController(service, meter)

    void "findAllActive returns empty list and 200 (standardized)"() {
        when:
        def resp = controller.findAllActive()

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.isEmpty()
    }

    void "findById returns 404 when missing (standardized)"() {
        when:
        controller.findById('missing')

        then:
        1 * service.findTransactionByGuid('missing') >> Optional.empty()
        thrown(ResponseStatusException)
    }

    void "save creates transaction and returns 201 (standardized)"() {
        given:
        def tx = new Transaction(guid: 'g')

        when:
        def resp = controller.save(tx)

        then:
        1 * service.insertTransaction(tx) >> tx
        resp.statusCode == HttpStatus.CREATED
        resp.body.guid == 'g'
    }

    void "update returns 404 when target missing (standardized)"() {
        given:
        def patch = new Transaction(guid: 'g')

        when:
        controller.update('g', patch)

        then:
        1 * service.findTransactionByGuid('g') >> Optional.empty()
        thrown(ResponseStatusException)
    }

    void "update success when found (standardized)"() {
        given:
        def db = new Transaction(transactionId: 1L, guid: 'g')
        def patch = new Transaction(guid: 'ignored')

        when:
        def resp = controller.update('g', patch)

        then:
        1 * service.findTransactionByGuid('g') >> Optional.of(db)
        1 * service.updateTransaction({ Transaction t -> t.guid == 'g' && t.transactionId == 1L }) >> { it[0] }
        resp.statusCode == HttpStatus.OK
        resp.body.guid == 'g'
    }

    void "deleteById returns 404 when entity missing (standardized)"() {
        when:
        controller.deleteById('gone')

        then:
        1 * service.findTransactionByGuid('gone') >> Optional.empty()
        thrown(ResponseStatusException)
    }

    void "selectTransactionsByCategory returns 200"() {
        when:
        def resp = controller.selectTransactionsByCategory('cat')

        then:
        1 * service.findTransactionsByCategory('cat') >> []
        resp.statusCode == HttpStatus.OK
    }

    void "selectTransactionsByDescription returns 200"() {
        when:
        def resp = controller.selectTransactionsByDescription('desc')

        then:
        1 * service.findTransactionsByDescription('desc') >> []
        resp.statusCode == HttpStatus.OK
    }

    void "selectTotalsCleared returns 200 with totals"() {
        given:
        def totals = new Totals(2G, 1G, 6G, 3G)

        when:
        def resp = controller.selectTotalsCleared('acct')

        then:
        1 * service.calculateActiveTotalsByAccountNameOwner('acct') >> totals
        resp.statusCode == HttpStatus.OK
        resp.body.totals == 6G
    }
}

