package finance.controllers

import finance.domain.Transfer
import finance.services.ITransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject

class TransferControllerSpec extends Specification {

    ITransferService transferService = GroovyMock(ITransferService)

    @Subject
    TransferController controller = new TransferController(transferService)

    def "selectAllTransfers returns list"() {
        given:
        def list = [new Transfer(transferId: 1L)]

        when:
        ResponseEntity<List<Transfer>> response = controller.selectAllTransfers()

        then:
        1 * transferService.findAllTransfers() >> list
        response.statusCode == HttpStatus.OK
        response.body == list
    }

    def "insertTransfer returns OK on success"() {
        given:
        def t = new Transfer(transferId: 0L)

        when:
        ResponseEntity<Transfer> response = controller.insertTransfer(t)

        then:
        1 * transferService.insertTransfer(t) >> { Transfer x -> x.transferId = 5L; return x }
        response.statusCode == HttpStatus.OK
        response.body.transferId == 5L
    }

    def "deleteByTransferId returns NOT_FOUND when missing"() {
        when:
        controller.deleteByTransferId(42L)

        then:
        1 * transferService.findByTransferId(42L) >> Optional.empty()
        0 * transferService.deleteByTransferId(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason == 'Transfer not found: 42'
    }
}

