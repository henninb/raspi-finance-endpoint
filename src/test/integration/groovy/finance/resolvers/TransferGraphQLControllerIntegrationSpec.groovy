package finance.resolvers

import finance.BaseIntegrationSpec
import finance.controllers.TransferGraphQLController
import finance.domain.Account
import finance.domain.Transfer
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import finance.repositories.TransactionRepository
import finance.services.ITransferService
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartTransferBuilder
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date
import java.util.UUID

/**
 * MIGRATED INTEGRATION TEST - TransferGraphQL Controller with robust, isolated architecture
 *
 * This is the migrated version of TransferGraphQLControllerIntegrationSpec showing:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Proper FK relationship management with Account setup
 * ✅ GraphQL controller testing with direct injection
 * ✅ Financial validation and consistency
 * ✅ Eliminated shared global state and cleanup issues
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferGraphQLControllerIntegrationSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    ITransferService transferService

    @Shared
    @Autowired
    MeterRegistry meterRegistry

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    @Autowired
    TransferRepository transferRepository

    @Shared
    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    TransferGraphQLController transferGraphQLController

    @Shared
    def repositoryContext

    @Shared
    Long sourceAccountId
    @Shared
    Long destinationAccountId
    @Shared
    String sourceAccountName
    @Shared
    String destinationAccountName

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)

        // Create test accounts using SmartBuilder
        sourceAccountName = "checking_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"
        destinationAccountName = "savings_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"

        // Source account (debit - checking)
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("checking")
                .asDebit()
                .withCleared(new BigDecimal("3000.00"))
                .buildAndValidate()
        Account savedSourceAccount = accountRepository.save(sourceAccount)
        sourceAccountId = savedSourceAccount.accountId
        sourceAccountName = savedSourceAccount.accountNameOwner

        // Destination account (debit - savings)
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("savings")
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .buildAndValidate()
        Account savedDestAccount = accountRepository.save(destinationAccount)
        destinationAccountId = savedDestAccount.accountId
        destinationAccountName = savedDestAccount.accountNameOwner
    }

    def "should fetch all transfers via GraphQL controller with isolated test data"() {
        given: "existing transfers in the database"
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("100.00"))
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("250.00"))

        when: "transfers query is called"
        if (transferGraphQLController == null) {
            throw new AssertionError("TransferGraphQLController is null - not properly injected")
        }
        def transfers = transferGraphQLController.transfers()

        then: "should return transfers from database with testOwner isolation"
        transfers.size() >= 2
        transfers.every { it instanceof Transfer }
        transfers.any { it.amount == new BigDecimal("100.00") && it.sourceAccount == sourceAccountName }
        transfers.any { it.amount == new BigDecimal("250.00") && it.sourceAccount == sourceAccountName }
    }

    def "should fetch transfer by ID via GraphQL controller with isolated test data"() {
        given: "an existing transfer in the database"
        def savedTransfer = createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("150.00"))

        when: "transfer query is called"
        def result = transferGraphQLController.transfer(savedTransfer.transferId)

        then: "should return the specific transfer with testOwner-based account names"
        result != null
        result.transferId == savedTransfer.transferId
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("150.00")
    }

    def "should handle transfer not found in database via GraphQL controller"() {
        given: "a non-existent transfer ID"
        def nonExistentId = 999999L

        when: "transfer query is called with non-existent ID"
        def result = transferGraphQLController.transfer(nonExistentId)

        then: "should return null"
        result == null
    }

    def "should create transfer via GraphQL controller with SmartBuilder validation"() {
        given: "transfer input data with testOwner-based account names"
        def transferInput = new TransferGraphQLController.TransferInput(
            sourceAccount: sourceAccountName,
            destinationAccount: destinationAccountName,
            transactionDate: "2024-01-15",
            amount: new BigDecimal("250.00")
        )

        when: "create transfer mutation is called"
        def result = transferGraphQLController.createTransfer(transferInput)

        then: "should create and return transfer with testOwner isolation"
        result != null
        result.transferId > 0
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("250.00")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus == true

        and: "transfer should be persisted with testOwner account names"
        def savedTransfer = transferRepository.findByTransferId(result.transferId)
        savedTransfer.isPresent()
        savedTransfer.get().sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))

        and: "corresponding debit and credit transactions should be created with proper FK relationships"
        def transactions = transactionRepository.findAll()
        transactions.size() >= 2
        transactions.any { it.guid == result.guidSource && it.accountNameOwner == sourceAccountName }
        transactions.any { it.guid == result.guidDestination && it.accountNameOwner == destinationAccountName }
    }

    def "should handle validation errors for invalid transfer creation with SmartBuilder constraints"() {
        given: "invalid transfer input data (invalid sourceAccount too short)"
        def transferInput = new TransferGraphQLController.TransferInput(
            sourceAccount: "ab", // Invalid - too short (less than 3 characters)
            destinationAccount: destinationAccountName,
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        )

        when: "create transfer mutation is called"
        transferGraphQLController.createTransfer(transferInput)

        then: "should throw runtime exception for validation failure"
        thrown(RuntimeException)
    }

    def "should delete transfer via GraphQL controller with isolated test data"() {
        given: "an existing transfer in the database"
        def savedTransfer = createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("75.00"))

        when: "delete transfer mutation is called"
        def result = transferGraphQLController.deleteTransfer(savedTransfer.transferId)

        then: "should successfully delete transfer and return true"
        result == true

        and: "transfer should be removed from database"
        def deletedTransfer = transferRepository.findByTransferId(savedTransfer.transferId)
        !deletedTransfer.isPresent()
    }

    def "should handle delete non-existent transfer via GraphQL controller"() {
        given: "a non-existent transfer ID"
        def nonExistentId = 999999L

        and: "verify controller is injected"
        if (transferGraphQLController == null) {
            throw new AssertionError("TransferGraphQLController is null - not properly injected")
        }

        when: "delete transfer mutation is called"
        def result = transferGraphQLController.deleteTransfer(nonExistentId)

        then: "should return false for non-existent transfer"
        result == false
    }

    def "should successfully execute GraphQL operations with testOwner isolation"() {
        given: "an existing transfer in the database"
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("50.00"))

        when: "transfers query is called"
        def transfers = transferGraphQLController.transfers()

        then: "should execute successfully with testOwner-based accounts"
        transfers != null
        transfers.size() >= 1
        transfers.any {
            it.amount == new BigDecimal("50.00") &&
            it.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            it.destinationAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }
    }

    def "should handle complex GraphQL transfer scenarios with FK relationships"() {
        given: "multiple transfers with different amounts and dates"
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("25.50"))
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("150.75"))
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("300.25"))

        when: "querying all transfers for this testOwner"
        def transfers = transferGraphQLController.transfers()
        def testOwnerTransfers = transfers.findAll {
            it.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }

        then: "should return all transfers for this testOwner with proper FK relationships"
        testOwnerTransfers.size() >= 3
        testOwnerTransfers.every { transfer ->
            transfer.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            transfer.destinationAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            transfer.guidSource != null &&
            transfer.guidDestination != null &&
            transfer.activeStatus == true
        }

        and: "corresponding transactions should exist with proper account relationships"
        def allTransactions = transactionRepository.findAll()
        def testOwnerTransactions = allTransactions.findAll {
            it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }
        testOwnerTransactions.size() >= 6  // 2 transactions per transfer (source + destination)
    }

    private Transfer createTestTransfer(String sourceAccount, String destinationAccount, BigDecimal amount) {
        Transfer transfer = new Transfer()
        transfer.sourceAccount = sourceAccount
        transfer.destinationAccount = destinationAccount
        transfer.transactionDate = Date.valueOf("2024-01-01")
        transfer.amount = amount
        transfer.guidSource = UUID.randomUUID().toString()
        transfer.guidDestination = UUID.randomUUID().toString()
        transfer.activeStatus = true

        return transferService.insertTransfer(transfer)
    }
}
