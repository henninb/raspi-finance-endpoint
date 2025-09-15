package finance.controllers

import finance.domain.Transfer
import finance.services.TransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject
import java.util.Optional

/**
 * TDD Specification for TransferController standardization.
 * This spec defines the requirements for migrating TransferController to use:
 * - StandardizedBaseController (not BaseController)
 * - StandardRestController<Transfer, Long> interface
 * - Dual endpoint support (legacy + standardized)
 * - Standardized exception handling patterns
 * - Standardized method naming conventions
 */
class StandardizedTransferControllerSpec extends Specification {

    TransferService transferService = GroovyMock(TransferService)

    @Subject
    TransferController controller = new TransferController(transferService)

    // ===== STANDARDIZED CRUD ENDPOINTS =====

    def "findAllActive should return all active transfers using standardized pattern"() {
        given:
        List<Transfer> transfers = [
            new Transfer(transferId: 1L, sourceAccount: "src_test", destinationAccount: "dest_test", amount: 100.00, activeStatus: true),
            new Transfer(transferId: 2L, sourceAccount: "src2_test", destinationAccount: "dest2_test", amount: 200.00, activeStatus: true)
        ]

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        1 * transferService.findAllTransfers() >> transfers
        response.statusCode == HttpStatus.OK
        response.body == transfers
        response.body.size() == 2
    }

    def "findAllActive should return empty list when no active transfers exist"() {
        given:
        List<Transfer> emptyTransfers = []

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        1 * transferService.findAllTransfers() >> emptyTransfers
        response.statusCode == HttpStatus.OK
        response.body == emptyTransfers
        response.body.isEmpty()
    }

    def "findById should return transfer when found using standardized pattern"() {
        given:
        Long transferId = 1L
        Transfer transfer = new Transfer(transferId: transferId, sourceAccount: "src_test", destinationAccount: "dest_test", amount: 150.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.findById(transferId)

        then:
        1 * transferService.findByTransferId(transferId) >> Optional.of(transfer)
        response.statusCode == HttpStatus.OK
        response.body == transfer
    }

    def "findById should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L

        when:
        controller.findById(nonExistentId)

        then:
        1 * transferService.findByTransferId(nonExistentId) >> Optional.empty()
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("Transfer not found: 999")
    }

    def "save should create new transfer and return 201 CREATED using standardized pattern"() {
        given:
        Transfer inputTransfer = new Transfer(transferId: 0L, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 250.00, activeStatus: true)
        Transfer savedTransfer = new Transfer(transferId: 5L, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 250.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.save(inputTransfer)

        then:
        1 * transferService.insertTransfer(inputTransfer) >> savedTransfer
        response.statusCode == HttpStatus.CREATED
        response.body == savedTransfer
        response.body.transferId == 5L
    }

    def "save should handle DataIntegrityViolationException with 409 CONFLICT using standardized pattern"() {
        given:
        Transfer duplicateTransfer = new Transfer(transferId: 0L, sourceAccount: "dup_src", destinationAccount: "dup_dest", amount: 100.00, activeStatus: true)

        when:
        controller.save(duplicateTransfer)

        then:
        1 * transferService.insertTransfer(duplicateTransfer) >> { throw new org.springframework.dao.DataIntegrityViolationException("Duplicate") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "save should handle ValidationException with 400 BAD_REQUEST using standardized pattern"() {
        given:
        Transfer invalidTransfer = new Transfer(transferId: 0L, sourceAccount: "", destinationAccount: "dest", amount: -100.00, activeStatus: true)

        when:
        controller.save(invalidTransfer)

        then:
        1 * transferService.insertTransfer(invalidTransfer) >> { throw new jakarta.validation.ValidationException("Invalid transfer") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
        ex.reason.contains("Validation error")
    }

    def "update should update existing transfer using standardized pattern"() {
        given:
        Long transferId = 3L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "old_src", destinationAccount: "old_dest", amount: 100.00, activeStatus: true)
        Transfer updateTransfer = new Transfer(transferId: transferId, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 300.00, activeStatus: true)
        Transfer updatedTransfer = new Transfer(transferId: transferId, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 300.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.update(transferId, updateTransfer)

        then:
        1 * transferService.findByTransferId(transferId) >> Optional.of(existingTransfer)
        1 * transferService.updateTransfer(updateTransfer) >> updatedTransfer
        response.statusCode == HttpStatus.OK
        response.body == updatedTransfer
    }

    def "update should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L
        Transfer updateTransfer = new Transfer(transferId: nonExistentId, sourceAccount: "src", destinationAccount: "dest", amount: 100.00, activeStatus: true)

        when:
        controller.update(nonExistentId, updateTransfer)

        then:
        1 * transferService.findByTransferId(nonExistentId) >> Optional.empty()
        0 * transferService.updateTransfer(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("Transfer not found: 999")
    }

    def "deleteById should delete existing transfer and return it using standardized pattern"() {
        given:
        Long transferId = 4L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "del_src", destinationAccount: "del_dest", amount: 400.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.deleteById(transferId)

        then:
        1 * transferService.findByTransferId(transferId) >> Optional.of(existingTransfer)
        1 * transferService.deleteByTransferId(transferId)
        response.statusCode == HttpStatus.OK
        response.body == existingTransfer
    }

    def "deleteById should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 888L

        when:
        controller.deleteById(nonExistentId)

        then:
        1 * transferService.findByTransferId(nonExistentId) >> Optional.empty()
        0 * transferService.deleteByTransferId(_)
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
        ex.reason.contains("Transfer not found: 888")
    }

    // ===== LEGACY ENDPOINT COMPATIBILITY =====

    def "selectAllTransfers should maintain backward compatibility"() {
        given:
        List<Transfer> transfers = [
            new Transfer(transferId: 1L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 500.00, activeStatus: true)
        ]

        when:
        ResponseEntity<List<Transfer>> response = controller.selectAllTransfers()

        then:
        1 * transferService.findAllTransfers() >> transfers
        response.statusCode == HttpStatus.OK
        response.body == transfers
    }

    def "insertTransfer should maintain backward compatibility with legacy insert pattern"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)
        Transfer savedTransfer = new Transfer(transferId: 10L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.insertTransfer(legacyTransfer)

        then:
        1 * transferService.insertTransfer(legacyTransfer) >> savedTransfer
        response.statusCode == HttpStatus.OK  // Legacy returns 200 OK, not 201 CREATED
        response.body == savedTransfer
    }

    def "deleteByTransferId should maintain backward compatibility with legacy delete pattern"() {
        given:
        Long transferId = 6L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "legacy_del", destinationAccount: "legacy_del_dest", amount: 600.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.deleteByTransferId(transferId)

        then:
        1 * transferService.findByTransferId(transferId) >> Optional.of(existingTransfer)
        1 * transferService.deleteByTransferId(transferId)
        response.statusCode == HttpStatus.OK
        response.body == existingTransfer
    }

    // ===== STANDARDIZATION REQUIREMENTS =====

    def "controller should extend StandardizedBaseController not BaseController"() {
        expect:
        // This test will pass once TransferController extends StandardizedBaseController
        controller.class.superclass.simpleName == "StandardizedBaseController"
    }

    def "controller should implement StandardRestController interface"() {
        expect:
        // This test will pass once TransferController implements StandardRestController<Transfer, Long>
        StandardRestController.isAssignableFrom(controller.class)
    }

    def "controller should have standardized exception handling patterns"() {
        given:
        Transfer errorTransfer = new Transfer(transferId: 0L, sourceAccount: "error", destinationAccount: "error", amount: 0.00, activeStatus: true)

        when:
        controller.save(errorTransfer)

        then:
        1 * transferService.insertTransfer(errorTransfer) >> { throw new RuntimeException("Unexpected error") }
        ResponseStatusException ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        ex.reason.contains("Unexpected error")
    }
}