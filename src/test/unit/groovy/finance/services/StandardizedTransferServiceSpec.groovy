package finance.services

import finance.domain.ServiceResult
import finance.domain.Transfer
import finance.helpers.TransferBuilder
import finance.repositories.TransferRepository

import java.util.Optional

/**
 * TDD Test Specification for StandardizedTransferService
 * Following the established ServiceResult pattern and TDD methodology
 */
class StandardizedTransferServiceSpec extends BaseServiceSpec {

    def transferRepositoryMock = Mock(TransferRepository)
    def transactionServiceMock = Mock(ITransactionService)
    // Note: accountService is inherited from BaseServiceSpec (real StandardizedAccountService with accountRepositoryMock)
    def standardizedTransferService = new StandardizedTransferService(transferRepositoryMock, transactionServiceMock, accountService)

    void setup() {
        standardizedTransferService.meterService = meterService
        standardizedTransferService.validator = validator
    }

    def "should have correct entity name"() {
        expect:
        standardizedTransferService.getEntityName() == "Transfer"
    }

    // ===== findAllActive Tests =====

    def "findAllActive should return ServiceResult.Success with list of transfers"() {
        given:
        def transfer1 = TransferBuilder.builder().withTransferId(1L).build()
        def transfer2 = TransferBuilder.builder().withTransferId(2L).build()
        def transfers = [transfer1, transfer2]

        when:
        transferRepositoryMock.findAll() >> transfers
        def result = standardizedTransferService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data == transfers
    }

    def "findAllActive should return ServiceResult.Success with empty list when no transfers"() {
        given:
        transferRepositoryMock.findAll() >> []

        when:
        def result = standardizedTransferService.findAllActive()

        then:
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "findAllActive should return ServiceResult.SystemError on repository exception"() {
        given:
        transferRepositoryMock.findAll() >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedTransferService.findAllActive()

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database error")
    }

    // ===== findById Tests =====

    def "findById should return ServiceResult.Success when transfer exists"() {
        given:
        def transferId = 1L
        def transfer = TransferBuilder.builder().withTransferId(transferId).build()

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        def result = standardizedTransferService.findById(transferId)

        then:
        result instanceof ServiceResult.Success
        result.data == transfer
    }

    def "findById should return ServiceResult.NotFound when transfer does not exist"() {
        given:
        def transferId = 999L

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        def result = standardizedTransferService.findById(transferId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "Transfer not found: 999"
    }

    def "findById should return ServiceResult.SystemError on repository exception"() {
        given:
        def transferId = 1L
        transferRepositoryMock.findByTransferId(transferId) >> { throw new RuntimeException("Database connection failed") }

        when:
        def result = standardizedTransferService.findById(transferId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Database connection failed")
    }

    // ===== save Tests =====

    def "save should return ServiceResult.Success when valid transfer"() {
        given:
        def transfer = TransferBuilder.builder()
                .withTransferId(0L)
                .withSourceAccount("source123")
                .withDestinationAccount("dest456")
                .build()
        def savedTransfer = TransferBuilder.builder().withTransferId(1L).build()

        when:
        transferRepositoryMock.save(transfer) >> savedTransfer
        def result = standardizedTransferService.save(transfer)

        then:
        result instanceof ServiceResult.Success
        result.data == savedTransfer
    }

    def "save should return ServiceResult.ValidationError on constraint violation"() {
        given:
        def transfer = TransferBuilder.builder()
                .withSourceAccount("") // Invalid empty string
                .build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "sourceAccount"
        violation.propertyPath >> mockPath
        violation.message >> "must not be blank"
        Set<jakarta.validation.ConstraintViolation<Transfer>> violations = [violation] as Set

        when:
        standardizedTransferService.validator = validatorMock
        validatorMock.validate(transfer) >> violations
        def result = standardizedTransferService.save(transfer)

        then:
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("sourceAccount")
    }

    def "save should return ServiceResult.SystemError on repository exception"() {
        given:
        def transfer = TransferBuilder.builder().build()
        transferRepositoryMock.save(transfer) >> { throw new RuntimeException("Save failed") }

        when:
        def result = standardizedTransferService.save(transfer)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Save failed")
    }

    // ===== update Tests =====

    def "update should return ServiceResult.Success when transfer exists"() {
        given:
        def transfer = TransferBuilder.builder()
                .withTransferId(1L)
                .withAmount(new BigDecimal("200.00"))
                .build()
        def existingTransfer = TransferBuilder.builder().withTransferId(1L).build()
        def updatedTransfer = TransferBuilder.builder()
                .withTransferId(1L)
                .withAmount(new BigDecimal("200.00"))
                .build()

        when:
        transferRepositoryMock.findByTransferId(1L) >> Optional.of(existingTransfer)
        transferRepositoryMock.save(transfer) >> updatedTransfer
        def result = standardizedTransferService.update(transfer)

        then:
        result instanceof ServiceResult.Success
        result.data == updatedTransfer
    }

    def "update should return ServiceResult.NotFound when transfer does not exist"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(999L).build()

        when:
        transferRepositoryMock.findByTransferId(999L) >> Optional.empty()
        def result = standardizedTransferService.update(transfer)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "Transfer not found: 999"
    }

    def "update should return ServiceResult.ValidationError on constraint violation"() {
        given:
        def transfer = TransferBuilder.builder()
                .withTransferId(1L)
                .withSourceAccount("") // Invalid empty string
                .build()
        def existingTransfer = TransferBuilder.builder().withTransferId(1L).build()

        // Mock validation to return constraint violations
        def violation = Mock(jakarta.validation.ConstraintViolation)
        def mockPath = Mock(jakarta.validation.Path)
        mockPath.toString() >> "sourceAccount"
        violation.propertyPath >> mockPath
        violation.message >> "must not be blank"
        Set<jakarta.validation.ConstraintViolation<Transfer>> violations = [violation] as Set

        when:
        standardizedTransferService.validator = validatorMock
        transferRepositoryMock.findByTransferId(1L) >> Optional.of(existingTransfer)
        validatorMock.validate(transfer) >> violations
        def result = standardizedTransferService.update(transfer)

        then:
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("sourceAccount")
    }

    def "update should return ServiceResult.SystemError on repository exception"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(1L).build()
        def existingTransfer = TransferBuilder.builder().withTransferId(1L).build()
        transferRepositoryMock.findByTransferId(1L) >> Optional.of(existingTransfer)
        transferRepositoryMock.save(transfer) >> { throw new RuntimeException("Update failed") }

        when:
        def result = standardizedTransferService.update(transfer)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Update failed")
    }

    // ===== deleteById Tests =====

    def "deleteById should return ServiceResult.Success when transfer exists"() {
        given:
        def transferId = 1L
        def transfer = TransferBuilder.builder().withTransferId(transferId).build()

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        transferRepositoryMock.delete(transfer) >> {}
        def result = standardizedTransferService.deleteById(transferId)

        then:
        result instanceof ServiceResult.Success
        result.data == true
    }

    def "deleteById should return ServiceResult.NotFound when transfer does not exist"() {
        given:
        def transferId = 999L

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        def result = standardizedTransferService.deleteById(transferId)

        then:
        result instanceof ServiceResult.NotFound
        result.message == "Transfer not found: 999"
    }

    def "deleteById should return ServiceResult.SystemError on repository exception"() {
        given:
        def transferId = 1L
        def transfer = TransferBuilder.builder().withTransferId(transferId).build()
        transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        transferRepositoryMock.delete(transfer) >> { throw new RuntimeException("Delete failed") }

        when:
        def result = standardizedTransferService.deleteById(transferId)

        then:
        result instanceof ServiceResult.SystemError
        result.exception.message.contains("Delete failed")
    }

    // ===== Legacy Method Compatibility Tests =====

    def "findAllTransfers should return list from findAllActive ServiceResult"() {
        given:
        def transfer1 = TransferBuilder.builder().withTransferId(1L).build()
        def transfer2 = TransferBuilder.builder().withTransferId(2L).build()
        def transfers = [transfer1, transfer2]

        when:
        transferRepositoryMock.findAll() >> transfers
        def result = standardizedTransferService.findAllTransfers()

        then:
        result.size() == 2
        result == transfers
    }

    def "findAllTransfers should return empty list on ServiceResult failure"() {
        given:
        transferRepositoryMock.findAll() >> { throw new RuntimeException("Database error") }

        when:
        def result = standardizedTransferService.findAllTransfers()

        then:
        result.isEmpty()
    }

    def "insertTransfer should return transfer on ServiceResult.Success"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(0L).build()
        def savedTransfer = TransferBuilder.builder().withTransferId(1L).build()
        def mockAccount = GroovyMock(finance.domain.Account)

        when:
        accountRepositoryMock.findByAccountNameOwner(transfer.sourceAccount) >> Optional.of(mockAccount)
        accountRepositoryMock.findByAccountNameOwner(transfer.destinationAccount) >> Optional.of(mockAccount)
        transactionServiceMock.insertTransaction(_) >> {}
        transferRepositoryMock.save(transfer) >> savedTransfer
        def result = standardizedTransferService.insertTransfer(transfer)

        then:
        result == savedTransfer
    }

    def "insertTransfer should throw RuntimeException on ServiceResult failure"() {
        given:
        def transfer = TransferBuilder.builder()
                .withSourceAccount("") // Invalid
                .build()

        when:
        standardizedTransferService.insertTransfer(transfer)

        then:
        thrown(RuntimeException)
    }

    def "updateTransfer should return transfer on ServiceResult.Success"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(1L).build()
        def existingTransfer = TransferBuilder.builder().withTransferId(1L).build()

        when:
        transferRepositoryMock.findByTransferId(1L) >> Optional.of(existingTransfer)
        transferRepositoryMock.save(transfer) >> transfer
        def result = standardizedTransferService.updateTransfer(transfer)

        then:
        result == transfer
    }

    def "updateTransfer should throw RuntimeException on ServiceResult.NotFound"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(999L).build()

        when:
        transferRepositoryMock.findByTransferId(999L) >> Optional.empty()
        standardizedTransferService.updateTransfer(transfer)

        then:
        thrown(RuntimeException)
    }

    def "findByTransferId should return Optional.of(transfer) when found"() {
        given:
        def transferId = 1L
        def transfer = TransferBuilder.builder().withTransferId(transferId).build()

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        def result = standardizedTransferService.findByTransferId(transferId)

        then:
        result.isPresent()
        result.get() == transfer
    }

    def "findByTransferId should return Optional.empty() when not found"() {
        given:
        def transferId = 999L

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        def result = standardizedTransferService.findByTransferId(transferId)

        then:
        result.isEmpty()
    }

    def "deleteByTransferId should return true when transfer exists"() {
        given:
        def transferId = 1L
        def transfer = TransferBuilder.builder().withTransferId(transferId).build()

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.of(transfer)
        transferRepositoryMock.delete(transfer) >> {}
        def result = standardizedTransferService.deleteByTransferId(transferId)

        then:
        result == true
    }

    def "deleteByTransferId should return false when transfer does not exist"() {
        given:
        def transferId = 999L

        when:
        transferRepositoryMock.findByTransferId(transferId) >> Optional.empty()
        def result = standardizedTransferService.deleteByTransferId(transferId)

        then:
        result == false
    }
}