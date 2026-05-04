package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.TransactionBuilder
import finance.helpers.TransferBuilder
import finance.repositories.TransferRepository
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class TransferServiceSpec extends BaseServiceSpec {

    def transferRepositoryMock = Mock(TransferRepository)
    def transactionServiceMock = Mock(TransactionService)
    def transferService = new TransferService(
        transferRepositoryMock,
        transactionServiceMock,
        accountService,
        meterService,
        validatorMock,
        ResilienceComponents.noOp()
    )

    def "findAllActive should return active transfers for current owner"() {
        given:
        def transfers = [
            TransferBuilder.builder().withTransferId(1L).build(),
            TransferBuilder.builder().withTransferId(2L).build()
        ]

        when:
        def result = transferService.findAllActive()

        then:
        1 * transferRepositoryMock.findByOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, true, _) >> new PageImpl(transfers)
        result instanceof ServiceResult.Success
        result.data*.transferId == [1L, 2L]
        0 * _
    }

    def "findById should return NotFound when transfer does not exist"() {
        when:
        def result = transferService.findById(999L)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Transfer not found: 999")
        0 * _
    }

    def "findAllActive pageable should return paged transfers"() {
        given:
        def pageable = PageRequest.of(0, 5)
        def page = new PageImpl([TransferBuilder.builder().withTransferId(10L).build()], pageable, 1)

        when:
        def result = transferService.findAllActive(pageable)

        then:
        1 * transferRepositoryMock.findByOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, true, pageable) >> page
        result instanceof ServiceResult.Success
        result.data.content*.transferId == [10L]
        0 * _
    }

    def "save should persist existing transfer directly when GUIDs are already present"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(5L).build()
        Set noViolations = [] as Set
        validatorMock.validate(_ as finance.domain.Transfer) >> noViolations
        transferRepositoryMock.save(_ as finance.domain.Transfer) >> { args ->
            def saved = args[0] as finance.domain.Transfer
            assert saved.transferId == 5L
            assert saved.guidSource == transfer.guidSource
            assert saved.guidDestination == transfer.guidDestination
            saved
        }

        when:
        def result = transferService.save(transfer)

        then:
        result instanceof ServiceResult.Success
        result.data.owner == TEST_OWNER
        result.data.dateAdded != null
        result.data.dateUpdated != null
    }

    def "save should create backing transactions for a new transfer without GUIDs"() {
        given:
        def transfer = TransferBuilder.builder()
            .withTransferId(0L)
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking)
        def destinationAccount = new Account(accountNameOwner: "savings_primary", accountType: AccountType.Savings)
        def sourceSaved = TransactionBuilder.builder().withGuid("11111111-1111-1111-1111-111111111111").build()
        def destinationSaved = TransactionBuilder.builder().withGuid("22222222-2222-2222-2222-222222222222").build()
        Set noViolations = [] as Set
        validatorMock.validate(_ as finance.domain.Transfer) >> noViolations
        transferRepositoryMock.save(_ as finance.domain.Transfer) >> { args ->
            def saved = args[0] as finance.domain.Transfer
            assert saved.guidSource == "11111111-1111-1111-1111-111111111111"
            assert saved.guidDestination == "22222222-2222-2222-2222-222222222222"
            saved
        }

        when:
        def result = transferService.save(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.of(destinationAccount)
        1 * transactionServiceMock.save(_ as Transaction) >> { Transaction tx ->
            assert tx.accountNameOwner == "checking_primary"
            assert tx.amount == transfer.amount.negate()
            assert tx.description == "transfer withdrawal"
            assert tx.category == "transfer"
            assert tx.notes == "Transfer to savings_primary"
            assert tx.transactionState == TransactionState.Outstanding
            ServiceResult.Success.of(sourceSaved)
        }
        1 * transactionServiceMock.save(_ as Transaction) >> { Transaction tx ->
            assert tx.accountNameOwner == "savings_primary"
            assert tx.amount == transfer.amount
            assert tx.description == "transfer deposit"
            assert tx.category == "transfer"
            assert tx.notes == "Transfer from checking_primary"
            assert tx.transactionState == TransactionState.Outstanding
            ServiceResult.Success.of(destinationSaved)
        }
        result instanceof ServiceResult.Success
        result.data.guidSource == "11111111-1111-1111-1111-111111111111"
        result.data.guidDestination == "22222222-2222-2222-2222-222222222222"
        result.data.owner == TEST_OWNER
    }

    def "insertTransfer should fail when source account is missing"() {
        given:
        def transfer = TransferBuilder.builder().withGuidSource(null).withGuidDestination(null).build()

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transfer.sourceAccount) >> Optional.empty()
        def ex = thrown(RuntimeException)
        ex.message.contains("Source account not found")
        0 * _
    }

    def "insertTransfer should map source transaction validation errors to constraint violation exception"() {
        given:
        def transfer = TransferBuilder.builder()
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking)
        def destinationAccount = new Account(accountNameOwner: "savings_primary", accountType: AccountType.Savings)

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.of(destinationAccount)
        1 * transactionServiceMock.save(_ as Transaction) >> ServiceResult.ValidationError.of([amount: "invalid"])
        def ex = thrown(ConstraintViolationException)
        ex.message.contains("Source transaction validation failed")
        0 * _
    }

    def "insertTransfer should map destination transaction business errors to data integrity violation exception"() {
        given:
        def transfer = TransferBuilder.builder()
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new Account(accountNameOwner: "checking_primary", accountType: AccountType.Checking)
        def destinationAccount = new Account(accountNameOwner: "savings_primary", accountType: AccountType.Savings)
        def sourceSaved = TransactionBuilder.builder().withGuid("11111111-1111-1111-1111-111111111111").build()

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.of(destinationAccount)
        1 * transactionServiceMock.save(_ as Transaction) >> ServiceResult.Success.of(sourceSaved)
        1 * transactionServiceMock.save(_ as Transaction) >> ServiceResult.BusinessError.of("duplicate", "DATA_INTEGRITY_VIOLATION")
        def ex = thrown(DataIntegrityViolationException)
        ex.message.contains("Destination transaction business error")
        0 * _
    }

    def "update should return NotFound when transfer does not exist"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(321L).build()

        when:
        def result = transferService.update(transfer)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 321L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Transfer not found: 321")
        0 * _
    }

    def "deleteById should delete transfer when present"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(50L).build()

        when:
        def result = transferService.deleteById(50L)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 50L) >> Optional.of(transfer)
        1 * transferRepositoryMock.delete(transfer)
        result instanceof ServiceResult.Success
        result.data.transferId == 50L
        0 * _
    }

    def "findAllTransfers should return empty list when standardized lookup fails"() {
        when:
        def result = transferService.findAllTransfers()

        then:
        1 * transferRepositoryMock.findByOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, true, _) >> { throw new RuntimeException("db down") }
        result == []
        0 * _
    }

    def "deleteByTransferId should return boolean based on owner scoped lookup"() {
        given:
        def existing = TransferBuilder.builder().withTransferId(77L).build()

        when:
        def deletedExisting = transferService.deleteByTransferId(77L)
        def deletedMissing = transferService.deleteByTransferId(88L)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 77L) >> Optional.of(existing)
        1 * transferRepositoryMock.delete(existing)
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 88L) >> Optional.empty()
        deletedExisting
        !deletedMissing
        0 * _
    }

    def "findById should return Success when transfer exists"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(10L).build()

        when:
        def result = transferService.findById(10L)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 10L) >> Optional.of(transfer)
        result instanceof finance.domain.ServiceResult.Success
        result.data.transferId == 10L
        0 * _
    }

    def "update should return Success when transfer exists and passes validation"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(20L).build()
        def existing = Optional.of(transfer)
        Set noViolations = [] as Set

        when:
        def result = transferService.update(transfer)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 20L) >> existing
        1 * validatorMock.validate(_ as finance.domain.Transfer) >> noViolations
        1 * transferRepositoryMock.save(_ as finance.domain.Transfer) >> transfer
        result instanceof finance.domain.ServiceResult.Success
        result.data.transferId == 20L
        0 * _
    }

    def "updateTransfer should return transfer on success"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(30L).build()
        Set noViolations = [] as Set

        when:
        def result = transferService.updateTransfer(transfer)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 30L) >> Optional.of(transfer)
        1 * validatorMock.validate(_ as finance.domain.Transfer) >> noViolations
        1 * transferRepositoryMock.save(_ as finance.domain.Transfer) >> transfer
        result.transferId == 30L
        0 * _
    }

    def "updateTransfer should throw RuntimeException when transfer not found"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(404L).build()

        when:
        transferService.updateTransfer(transfer)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 404L) >> Optional.empty()
        def ex = thrown(RuntimeException)
        ex.message.contains("404")
        0 * _
    }

    def "insertTransfer should fail when destination account is missing"() {
        given:
        def transfer = TransferBuilder.builder()
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new finance.domain.Account(accountNameOwner: "checking_primary", accountType: finance.domain.AccountType.Checking)

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.empty()
        def ex = thrown(RuntimeException)
        ex.message.contains("Destination account not found")
        0 * _
    }

    def "insertTransfer should map source transaction generic errors to RuntimeException"() {
        given:
        def transfer = TransferBuilder.builder()
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new finance.domain.Account(accountNameOwner: "checking_primary", accountType: finance.domain.AccountType.Checking)
        def destinationAccount = new finance.domain.Account(accountNameOwner: "savings_primary", accountType: finance.domain.AccountType.Savings)

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.of(destinationAccount)
        1 * transactionServiceMock.save(_ as finance.domain.Transaction) >> finance.domain.ServiceResult.NotFound.of("tx gone")
        def ex = thrown(RuntimeException)
        ex.message.contains("Failed to create source transaction")
        0 * _
    }

    def "insertTransfer should map destination transaction validation errors to ConstraintViolationException"() {
        given:
        def transfer = TransferBuilder.builder()
            .withGuidSource(null)
            .withGuidDestination(null)
            .withSourceAccount("checking_primary")
            .withDestinationAccount("savings_primary")
            .build()
        def sourceAccount = new finance.domain.Account(accountNameOwner: "checking_primary", accountType: finance.domain.AccountType.Checking)
        def destinationAccount = new finance.domain.Account(accountNameOwner: "savings_primary", accountType: finance.domain.AccountType.Savings)
        def sourceSaved = TransactionBuilder.builder().withGuid("aaaa-0000").build()

        when:
        transferService.insertTransfer(transfer)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "checking_primary") >> Optional.of(sourceAccount)
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, "savings_primary") >> Optional.of(destinationAccount)
        1 * transactionServiceMock.save(_ as finance.domain.Transaction) >> finance.domain.ServiceResult.Success.of(sourceSaved)
        1 * transactionServiceMock.save(_ as finance.domain.Transaction) >> finance.domain.ServiceResult.ValidationError.of([amount: "invalid"])
        def ex = thrown(jakarta.validation.ConstraintViolationException)
        ex.message.contains("Destination transaction validation failed")
        0 * _
    }

    def "findByTransferId should return transfer wrapped in Optional"() {
        given:
        def transfer = TransferBuilder.builder().withTransferId(55L).build()

        when:
        def result = transferService.findByTransferId(55L)

        then:
        1 * transferRepositoryMock.findByOwnerAndTransferId(TEST_OWNER, 55L) >> Optional.of(transfer)
        result.isPresent()
        result.get().transferId == 55L
        0 * _
    }
}
