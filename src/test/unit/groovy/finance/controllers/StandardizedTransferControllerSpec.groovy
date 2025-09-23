package finance.controllers

import finance.domain.Transfer
import finance.domain.ServiceResult
import finance.services.StandardizedTransferService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Subject
import java.util.Optional

/**
 * TDD Specification for TransferController standardization.
 * This spec defines the requirements for migrating TransferController to use:
 * - StandardizedTransferService (direct injection)
 * - ServiceResult pattern for all operations
 * - StandardizedBaseController (not BaseController)
 * - StandardRestController<Transfer, Long> interface
 * - Dual endpoint support (legacy + standardized)
 * - Standardized exception handling patterns
 * - Standardized method naming conventions
 */
class StandardizedTransferControllerSpec extends Specification {

    // Build a real service with mocked collaborators to avoid mocking final Kotlin classes
    finance.repositories.TransferRepository transferRepository = Mock()
    finance.services.ITransactionService transactionService = Mock()
    finance.repositories.AccountRepository accountRepository = Mock()
    finance.services.StandardizedAccountService accountService = new finance.services.StandardizedAccountService(accountRepository)

    StandardizedTransferService standardizedTransferService = new StandardizedTransferService(transferRepository, transactionService, accountService)

    @Subject
    TransferController controller = new TransferController(standardizedTransferService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        standardizedTransferService.validator = validator
        standardizedTransferService.meterService = meterService
        accountService.validator = validator
        accountService.meterService = meterService
    }

    // ===== STANDARDIZED CRUD ENDPOINTS =====

    def "findAllActive should return all active transfers using standardized pattern"() {
        given:
        List<Transfer> transfers = [
            new Transfer(transferId: 1L, sourceAccount: "src_test", destinationAccount: "dest_test", amount: 100.00, activeStatus: true),
            new Transfer(transferId: 2L, sourceAccount: "src2_test", destinationAccount: "dest2_test", amount: 200.00, activeStatus: true)
        ]

        and:
        transferRepository.findAll() >> transfers

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body == transfers
        response.body.size() == 2
    }

    def "findAllActive should return empty list when no active transfers exist"() {
        given:
        List<Transfer> emptyTransfers = []

        and:
        transferRepository.findAll() >> emptyTransfers

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        response.body == emptyTransfers
        response.body.isEmpty()
    }

    def "findAllActive should return 404 when service NotFound"() {
        given:
        transferRepository.findAll() >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findAllActive should return 500 on system error"() {
        given:
        transferRepository.findAll() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<List<Transfer>> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById should return transfer when found using standardized pattern"() {
        given:
        Long transferId = 1L
        Transfer transfer = new Transfer(transferId: transferId, sourceAccount: "src_test", destinationAccount: "dest_test", amount: 150.00, activeStatus: true)

        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(transfer)

        when:
        ResponseEntity<Transfer> response = controller.findById(transferId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == transfer
    }

    def "findById should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L

        and:
        transferRepository.findByTransferId(nonExistentId) >> Optional.empty()

        when:
        ResponseEntity<Transfer> response = controller.findById(nonExistentId)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "findById should return 500 on system error"() {
        given:
        Long id = 777L
        and:
        transferRepository.findByTransferId(id) >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<Transfer> response = controller.findById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "save should create new transfer and return 201 CREATED using standardized pattern"() {
        given:
        Transfer inputTransfer = new Transfer(transferId: 0L, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 250.00, activeStatus: true)
        Transfer savedTransfer = new Transfer(transferId: 5L, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 250.00, activeStatus: true)

        and:
        transferRepository.save(_ as Transfer) >> { Transfer t -> t.transferId = 5L; return t }

        when:
        ResponseEntity<Transfer> response = controller.save(inputTransfer)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.transferId == 5L
    }

    def "save should handle DataIntegrityViolationException with 409 CONFLICT using standardized pattern"() {
        given:
        Transfer duplicateTransfer = new Transfer(transferId: 0L, sourceAccount: "dup_src", destinationAccount: "dup_dest", amount: 100.00, activeStatus: true)

        and:
        transferRepository.save(_ as Transfer) >> { throw new org.springframework.dao.DataIntegrityViolationException("Duplicate") }

        when:
        ResponseEntity<Transfer> response = controller.save(duplicateTransfer)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "save should handle ValidationException with 400 BAD_REQUEST using standardized pattern"() {
        given:
        Transfer invalidTransfer = new Transfer(transferId: 0L, sourceAccount: "", destinationAccount: "dest", amount: -100.00, activeStatus: true)

        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        standardizedTransferService.validator = violatingValidator

        when:
        ResponseEntity<Transfer> response = controller.save(invalidTransfer)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update should update existing transfer using standardized pattern"() {
        given:
        Long transferId = 3L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "old_src", destinationAccount: "old_dest", amount: 100.00, activeStatus: true)
        Transfer updateTransfer = new Transfer(transferId: transferId, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 300.00, activeStatus: true)
        Transfer updatedTransfer = new Transfer(transferId: transferId, sourceAccount: "new_src", destinationAccount: "new_dest", amount: 300.00, activeStatus: true)

        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(existingTransfer)
        transferRepository.save(_ as Transfer) >> { Transfer t -> t }

        when:
        ResponseEntity<Transfer> response = controller.update(transferId, updateTransfer)

        then:
        response.statusCode == HttpStatus.OK
        response.body.transferId == transferId
        response.body.sourceAccount == "new_src"
    }

    def "update should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 999L
        Transfer updateTransfer = new Transfer(transferId: nonExistentId, sourceAccount: "src", destinationAccount: "dest", amount: 100.00, activeStatus: true)

        and:
        transferRepository.findByTransferId(nonExistentId) >> Optional.empty()

        when:
        ResponseEntity<Transfer> response = controller.update(nonExistentId, updateTransfer)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update should return 400 on validation error using standardized pattern"() {
        given:
        Long transferId = 31L
        Transfer existing = new Transfer(transferId: transferId, sourceAccount: "old", destinationAccount: "old", amount: 1.00, activeStatus: true)
        Transfer patch = new Transfer(transferId: transferId, sourceAccount: "new", destinationAccount: "new", amount: -1.00, activeStatus: true)
        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(existing)
        transferRepository.save(_ as Transfer) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<Transfer> response = controller.update(transferId, patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update should return 409 on business error using standardized pattern"() {
        given:
        Long transferId = 32L
        Transfer existing = new Transfer(transferId: transferId, sourceAccount: "old", destinationAccount: "old", amount: 1.00, activeStatus: true)
        Transfer patch = new Transfer(transferId: transferId, sourceAccount: "new", destinationAccount: "new", amount: 2.00, activeStatus: true)
        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(existing)
        transferRepository.save(_ as Transfer) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<Transfer> response = controller.update(transferId, patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
    }

    def "update should return 500 on system error using standardized pattern"() {
        given:
        Long transferId = 33L
        Transfer existing = new Transfer(transferId: transferId, sourceAccount: "old", destinationAccount: "old", amount: 1.00, activeStatus: true)
        Transfer patch = new Transfer(transferId: transferId, sourceAccount: "new", destinationAccount: "new", amount: 2.00, activeStatus: true)
        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(existing)
        transferRepository.save(_ as Transfer) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Transfer> response = controller.update(transferId, patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById should delete existing transfer and return it using standardized pattern"() {
        given:
        Long transferId = 4L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "del_src", destinationAccount: "del_dest", amount: 400.00, activeStatus: true)

        and:
        transferRepository.findByTransferId(transferId) >> Optional.of(existingTransfer)

        when:
        ResponseEntity<Transfer> response = controller.deleteById(transferId)

        then:
        response.statusCode == HttpStatus.OK
        response.body == existingTransfer
    }

    def "deleteById should return 500 when find errors"() {
        given:
        Long id = 41L
        and:
        transferRepository.findByTransferId(id) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Transfer> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById should return 500 when delete fails"() {
        given:
        Long id = 42L
        Transfer existing = new Transfer(transferId: id, sourceAccount: "d1", destinationAccount: "d2", amount: 5.00, activeStatus: true)
        and:
        2 * transferRepository.findByTransferId(id) >> Optional.of(existing)
        transferRepository.delete(_ as Transfer) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<Transfer> response = controller.deleteById(id)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById should throw 404 when transfer not found using standardized pattern"() {
        given:
        Long nonExistentId = 888L

        and:
        transferRepository.findByTransferId(nonExistentId) >> Optional.empty()

        when:
        ResponseEntity<Transfer> response = controller.deleteById(nonExistentId)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    // ===== LEGACY ENDPOINT COMPATIBILITY =====

    def "selectAllTransfers should maintain backward compatibility"() {
        given:
        List<Transfer> transfers = [
            new Transfer(transferId: 1L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 500.00, activeStatus: true)
        ]

        and:
        transferRepository.findAll() >> transfers

        when:
        ResponseEntity<List<Transfer>> response = controller.selectAllTransfers()

        then:
        response.statusCode == HttpStatus.OK
        response.body == transfers
    }

    def "selectAllTransfers should throw 500 on error"() {
        given:
        transferRepository.findAll() >> { throw new RuntimeException("db") }

        when:
        controller.selectAllTransfers()

        then:
        thrown(ResponseStatusException)
    }

    def "insertTransfer should maintain backward compatibility with legacy insert pattern"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)
        Transfer savedTransfer = new Transfer(transferId: 10L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)

        and:
        accountRepository.findByAccountNameOwner("legacy_src") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_src"))
        accountRepository.findByAccountNameOwner("legacy_dest") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_dest"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        transferRepository.save(_ as Transfer) >> { Transfer t -> t.transferId = 10L; return t }

        when:
        ResponseEntity<Transfer> response = controller.insertTransfer(legacyTransfer)

        then:
        response.statusCode == HttpStatus.OK  // Legacy returns 200 OK, not 201 CREATED
        response.body.transferId == 10L
    }

    def "insertTransfer legacy returns 409 on duplicate"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("legacy_src") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_src"))
        accountRepository.findByAccountNameOwner("legacy_dest") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_dest"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        transferRepository.save(_ as Transfer) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        controller.insertTransfer(legacyTransfer)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "insertTransfer legacy returns 400 on validation error"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: -100.00, activeStatus: true)
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        standardizedTransferService.validator = violatingValidator
        accountRepository.findByAccountNameOwner("legacy_src") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_src"))
        accountRepository.findByAccountNameOwner("legacy_dest") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_dest"))

        when:
        controller.insertTransfer(legacyTransfer)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertTransfer legacy returns 400 on illegal argument"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("legacy_src") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_src"))
        accountRepository.findByAccountNameOwner("legacy_dest") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_dest"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { throw new IllegalArgumentException("bad") }

        when:
        controller.insertTransfer(legacyTransfer)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "insertTransfer legacy returns 500 on exception"() {
        given:
        Transfer legacyTransfer = new Transfer(transferId: 0L, sourceAccount: "legacy_src", destinationAccount: "legacy_dest", amount: 100.00, activeStatus: true)
        and:
        accountRepository.findByAccountNameOwner("legacy_src") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_src"))
        accountRepository.findByAccountNameOwner("legacy_dest") >> Optional.of(new finance.domain.Account(accountNameOwner: "legacy_dest"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { throw new RuntimeException("db") }

        when:
        controller.insertTransfer(legacyTransfer)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteByTransferId should maintain backward compatibility with legacy delete pattern"() {
        given:
        Long transferId = 6L
        Transfer existingTransfer = new Transfer(transferId: transferId, sourceAccount: "legacy_del", destinationAccount: "legacy_del_dest", amount: 600.00, activeStatus: true)

        when:
        ResponseEntity<Transfer> response = controller.deleteByTransferId(transferId)

        then:
        2 * transferRepository.findByTransferId(transferId) >> Optional.of(existingTransfer)
        response.statusCode == HttpStatus.OK
        response.body == existingTransfer
    }

    def "deleteByTransferId legacy returns 404 when not found"() {
        given:
        Long id = 61L
        and:
        transferRepository.findByTransferId(id) >> Optional.empty()

        when:
        controller.deleteByTransferId(id)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteByTransferId legacy returns 500 on error"() {
        given:
        Long id = 62L
        Transfer existing = new Transfer(transferId: id, sourceAccount: "ls", destinationAccount: "ld", amount: 1.00, activeStatus: true)
        and:
        transferRepository.findByTransferId(id) >> Optional.of(existing)
        transferRepository.delete(_ as Transfer) >> { throw new RuntimeException("db") }

        when:
        controller.deleteByTransferId(id)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
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
        and:
        transferRepository.save(_ as Transfer) >> { throw new RuntimeException("Unexpected error") }

        when:
        ResponseEntity<Transfer> response = controller.save(errorTransfer)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
