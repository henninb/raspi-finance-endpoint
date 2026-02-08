package finance.controllers

import finance.domain.Transfer
import finance.domain.ServiceResult
import finance.services.TransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TransferControllerSpec extends Specification {

    TransferService transferService = Mock()

    @Subject
    TransferController controller = new TransferController(transferService)

    // ===== HELPER METHODS =====

    private Transfer createTestTransfer(Long id = 1L, String sourceAccount = "checking_brian", String destinationAccount = "savings_brian", BigDecimal amount = new BigDecimal("100.00")) {
        return new Transfer(
            id,
            "test_owner",
            sourceAccount,
            destinationAccount,
            LocalDate.now(),
            amount,
            "11111111-1111-1111-1111-111111111111",
            "22222222-2222-2222-2222-222222222222",
            true
        )
    }

    private List<Transfer> createTestTransfers() {
        return [
            createTestTransfer(1L, "checking_brian", "savings_brian", new BigDecimal("100.00")),
            createTestTransfer(2L, "savings_brian", "investment_brian", new BigDecimal("200.00")),
            createTestTransfer(3L, "checking_janice", "savings_janice", new BigDecimal("150.00"))
        ]
    }

    // ===== STANDARDIZED ENDPOINTS TESTS =====

    def "findAllActive returns list when transfers present"() {
        given:
        List<Transfer> transfers = createTestTransfers()
        and:
        transferService.findAllActive() >> ServiceResult.Success.of(transfers)

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.size() == 3
        response.body.any { it.sourceAccount == "checking_brian" }
        response.body.any { it.amount == new BigDecimal("200.00") }
    }

    def "findAllActive returns empty list when no transfers"() {
        given:
        transferService.findAllActive() >> ServiceResult.Success.of([])

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
    }

    def "findAllActive returns 404 when service returns NotFound"() {
        given:
        transferService.findAllActive() >> ServiceResult.NotFound.of("No transfers found")

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findAllActive returns 500 on system error"() {
        given:
        transferService.findAllActive() >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findAllActive returns 500 on unexpected result type"() {
        given:
        transferService.findAllActive() >> ServiceResult.ValidationError.of([field: "error"])

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns transfer when found"() {
        given:
        Transfer transfer = createTestTransfer(1L)
        and:
        transferService.findById(1L) >> ServiceResult.Success.of(transfer)

        when:
        ResponseEntity<Transfer> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.transferId == 1L
        response.body.sourceAccount == "checking_brian"
    }

    def "findById returns 404 when transfer not found"() {
        given:
        transferService.findById(999L) >> ServiceResult.NotFound.of("Transfer not found: 999")

        when:
        ResponseEntity<Transfer> response = controller.findById(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findById returns 500 on system error"() {
        given:
        transferService.findById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Transfer> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "findById returns 500 on unexpected result type"() {
        given:
        transferService.findById(1L) >> ServiceResult.ValidationError.of([field: "error"])

        when:
        ResponseEntity<Transfer> response = controller.findById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save creates transfer and returns 201"() {
        given:
        Transfer newTransfer = createTestTransfer(0L)
        Transfer savedTransfer = createTestTransfer(10L)
        and:
        transferService.save(newTransfer) >> ServiceResult.Success.of(savedTransfer)

        when:
        ResponseEntity<Transfer> response = controller.save(newTransfer)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.transferId == 10L
        response.body.sourceAccount == "checking_brian"
    }

    def "save handles validation errors with 400"() {
        given:
        Transfer invalidTransfer = createTestTransfer(0L)
        and:
        transferService.save(invalidTransfer) >> ServiceResult.ValidationError.of([sourceAccount: "is required"])

        when:
        ResponseEntity<Transfer> response = controller.save(invalidTransfer)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save handles conflict with 409 when unique violation"() {
        given:
        Transfer duplicateTransfer = createTestTransfer(0L)
        and:
        transferService.save(duplicateTransfer) >> ServiceResult.BusinessError.of("Duplicate transfer found", "DUPLICATE_TRANSFER")

        when:
        ResponseEntity<Transfer> response = controller.save(duplicateTransfer)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "save returns 500 on system error"() {
        given:
        Transfer newTransfer = createTestTransfer(0L)
        and:
        transferService.save(newTransfer) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Transfer> response = controller.save(newTransfer)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save returns 500 on unexpected result type"() {
        given:
        Transfer newTransfer = createTestTransfer(0L)
        and:
        transferService.save(newTransfer) >> ServiceResult.NotFound.of("Unexpected")

        when:
        ResponseEntity<Transfer> response = controller.save(newTransfer)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 200 when transfer exists"() {
        given:
        Transfer updatedTransfer = createTestTransfer(1L, "checking_brian", "savings_brian", new BigDecimal("150.00"))
        and:
        transferService.update(updatedTransfer) >> ServiceResult.Success.of(updatedTransfer)

        when:
        ResponseEntity<Transfer> response = controller.update(1L, updatedTransfer)

        then:
        response.statusCode == HttpStatus.OK
        response.body.amount == new BigDecimal("150.00")
        response.body.transferId == 1L
        updatedTransfer.transferId == 1L // Verify ID was set from path parameter
    }

    def "update returns 404 when transfer not found"() {
        given:
        Transfer patchTransfer = createTestTransfer(999L)
        and:
        transferService.update(patchTransfer) >> ServiceResult.NotFound.of("Transfer not found: 999")

        when:
        ResponseEntity<Transfer> response = controller.update(999L, patchTransfer)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "update returns 400 when validation fails"() {
        given:
        Transfer patchTransfer = createTestTransfer(1L)
        and:
        transferService.update(patchTransfer) >> ServiceResult.ValidationError.of([amount: "must be positive"])

        when:
        ResponseEntity<Transfer> response = controller.update(1L, patchTransfer)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "update returns 409 on business conflict"() {
        given:
        Transfer patchTransfer = createTestTransfer(1L)
        and:
        transferService.update(patchTransfer) >> ServiceResult.BusinessError.of("Duplicate transfer", "DUPLICATE_TRANSFER")

        when:
        ResponseEntity<Transfer> response = controller.update(1L, patchTransfer)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "update returns 500 on system error"() {
        given:
        Transfer patchTransfer = createTestTransfer(1L)
        and:
        transferService.update(patchTransfer) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Transfer> response = controller.update(1L, patchTransfer)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 500 on unexpected result type"() {
        given:
        Transfer patchTransfer = createTestTransfer(1L)
        and:
        transferService.update(patchTransfer) >> null

        when:
        ResponseEntity<Transfer> response = controller.update(1L, patchTransfer)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        Transfer existingTransfer = createTestTransfer(1L)
        and:
        transferService.findById(1L) >> ServiceResult.Success.of(existingTransfer)
        transferService.deleteById(1L) >> ServiceResult.Success.of(true)

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.OK
        response.body.transferId == 1L
        response.body.sourceAccount == "checking_brian"
    }

    def "deleteById returns 404 when not found during find"() {
        given:
        transferService.findById(999L) >> ServiceResult.NotFound.of("Transfer not found: 999")

        when:
        ResponseEntity<Transfer> response = controller.deleteById(999L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 500 when find operation errors"() {
        given:
        transferService.findById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Database error"))

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 500 when find returns unexpected result type"() {
        given:
        transferService.findById(1L) >> ServiceResult.ValidationError.of([field: "error"])

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 404 when delete operation returns NotFound"() {
        given:
        Transfer existingTransfer = createTestTransfer(1L)
        and:
        transferService.findById(1L) >> ServiceResult.Success.of(existingTransfer)
        transferService.deleteById(1L) >> ServiceResult.NotFound.of("Transfer not found: 1")

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "deleteById returns 500 when delete operation errors"() {
        given:
        Transfer existingTransfer = createTestTransfer(1L)
        and:
        transferService.findById(1L) >> ServiceResult.Success.of(existingTransfer)
        transferService.deleteById(1L) >> ServiceResult.SystemError.of(new RuntimeException("Delete failed"))

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 500 when delete returns unexpected result type"() {
        given:
        Transfer existingTransfer = createTestTransfer(1L)
        and:
        transferService.findById(1L) >> ServiceResult.Success.of(existingTransfer)
        transferService.deleteById(1L) >> ServiceResult.ValidationError.of([field: "error"])

        when:
        ResponseEntity<Transfer> response = controller.deleteById(1L)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }
}