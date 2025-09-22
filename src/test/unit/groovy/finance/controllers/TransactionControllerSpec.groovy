package finance.controllers

import finance.domain.Transaction
import finance.domain.TransactionState
import finance.services.MeterService
import finance.services.ITransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

class TransactionControllerSpec extends Specification {

    ITransactionService transactionService = GroovyMock(ITransactionService)
    MeterService meterService = GroovyMock(MeterService)

    @Subject
    TransactionController controller = new TransactionController(transactionService, meterService)

    def "selectByAccountNameOwner returns OK with list"() {
        given:
        def list = [new Transaction(guid: 'g1'), new Transaction(guid: 'g2')]

        when:
        ResponseEntity<List<Transaction>> response = controller.selectByAccountNameOwner('acct')

        then:
        1 * transactionService.findByAccountNameOwnerOrderByTransactionDate('acct') >> list
        response.statusCode == HttpStatus.OK
        response.body.size() == 2
    }

    def "findTransaction returns transaction when found"() {
        given:
        Transaction transaction = new Transaction(guid: 'test-guid')

        when:
        ResponseEntity<Transaction> response = controller.findTransaction('test-guid')

        then:
        1 * transactionService.findTransactionByGuid('test-guid') >> Optional.of(transaction)
        response.statusCode == HttpStatus.OK
        response.body == transaction
    }

    def "updateTransactionState returns OK with valid state"() {
        given:
        Transaction updated = new Transaction(guid: 'test-guid', transactionState: TransactionState.Cleared)

        when:
        ResponseEntity<Transaction> response = controller.updateTransactionState('test-guid', 'cleared')

        then:
        1 * transactionService.updateTransactionState('test-guid', TransactionState.Cleared) >> updated
        response.statusCode == HttpStatus.OK
        response.body == updated
    }

    def "updateTransactionState with invalid value returns BAD_REQUEST"() {
        when:
        controller.updateTransactionState('guid', 'bad_state')

        then:
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.startsWith('Invalid transaction state:')
    }
}

