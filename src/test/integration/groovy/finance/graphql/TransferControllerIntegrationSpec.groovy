package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.GraphQLQueryController
import finance.domain.Account
import finance.domain.Transfer
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import finance.repositories.TransactionRepository
import finance.services.StandardizedTransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date
import java.util.UUID

class TransferControllerIntegrationSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    StandardizedTransferService transferService

    @Shared @Autowired
    MeterRegistry meterRegistry

    @Shared @Autowired
    AccountRepository accountRepository

    @Shared @Autowired
    TransferRepository transferRepository

    @Shared @Autowired
    TransactionRepository transactionRepository

    @Shared @Autowired
    GraphQLMutationController mutationController

    @Shared @Autowired
    GraphQLQueryController queryController

    @Shared
    String sourceAccountName

    @Shared
    String destinationAccountName

    def setupSpec() {
        // Create unique, valid account names based on testOwner
        def cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        // SmartAccountBuilder expects single underscore pattern: [letters-or-dashes]_[letters]
        sourceAccountName = "transfersrc_${cleanOwner}"
        destinationAccountName = "transferdst_${cleanOwner}"

        // Create source (debit) and destination (debit) accounts via SmartBuilder
        Account source = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(sourceAccountName)
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .buildAndValidate()
        accountRepository.save(source)

        Account dest = SmartAccountBuilder.builderForOwner(testOwner)
                .withAccountNameOwner(destinationAccountName)
                .asDebit()
                .withCleared(new BigDecimal("500.00"))
                .buildAndValidate()
        accountRepository.save(dest)
    }

    def "should fetch all transfers via controller with isolated test data"() {
        given: "existing transfers for this test owner"
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("100.00"))
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("200.00"))

        when: "controller query is called"
        def transfers = queryController.transfers()
        def scoped = transfers.findAll { it.sourceAccount == sourceAccountName && it.destinationAccount == destinationAccountName }

        then: "returns transfers created for this spec"
        scoped.size() >= 2
        scoped.any { it.amount == new BigDecimal("100.00") }
        scoped.any { it.amount == new BigDecimal("200.00") }
    }

    def "should fetch transfer by ID via controller with isolated test data"() {
        given: "an existing transfer for this owner"
        def savedTransfer = createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("150.00"))

        when: "controller query is called"
        def result = queryController.transfer(savedTransfer.transferId)

        then: "returns the specific transfer"
        result != null
        result.transferId == savedTransfer.transferId
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("150.00")
    }

    def "should handle transfer not found in database via controller"() {
        when: "controller query is called with non-existent ID"
        def result = queryController.transfer(999L)

        then: "returns null"
        result == null
    }

    def "should create transfer via controller with isolated test data"() {
        given: "authenticated user"
        withUserRole("test", ["USER"])

        when: "create transfer mutation is called"
        def result = mutationController.createTransfer(new finance.controllers.dto.TransferInputDto(
                null,
                sourceAccountName,
                destinationAccountName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("300.00"),
                null,
                null,
                null
        ))

        then: "transfer is created and persisted"
        result != null
        result.transferId > 0
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("300.00")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus

        and: "transfer exists in repository"
        def saved = transferRepository.findByTransferId(result.transferId)
        saved.isPresent()
        saved.get().sourceAccount == sourceAccountName
    }

    def "should handle validation errors during transfer creation with isolated data"() {
        given: "authenticated user"
        withUserRole("test", ["USER"])

        when: "create transfer mutation with non-existent source account"
        mutationController.createTransfer(new finance.controllers.dto.TransferInputDto(
                null,
                "nonexistent_${UUID.randomUUID().toString().take(8)}",
                destinationAccountName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("300.00"),
                null,
                null,
                null
        ))

        then: "throws runtime exception"
        thrown(RuntimeException)
    }

    def "should delete transfer via controller with isolated data"() {
        given: "an existing transfer"
        def saved = createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("250.00"))

        and: "authenticated user"
        withUserRole("test", ["USER"])

        when: "delete transfer mutation is called"
        def result = mutationController.deleteTransfer(saved.transferId)

        then: "successfully deletes"
        result

        and: "removed from repository"
        def deleted = transferRepository.findByTransferId(saved.transferId)
        !deleted.isPresent()
    }

    def "should handle delete non-existent transfer via controller"() {
        given: "authenticated user"
        withUserRole("test", ["USER"])

        expect: "returns false for missing transfer"
        !mutationController.deleteTransfer(999L)
    }

    private Transfer createTestTransfer(String sourceAccount, String destinationAccount, BigDecimal amount) {
        def t = new Transfer()
        t.sourceAccount = sourceAccount
        t.destinationAccount = destinationAccount
        t.transactionDate = Date.valueOf("2024-01-01")
        t.amount = amount
        t.guidSource = UUID.randomUUID().toString()
        t.guidDestination = UUID.randomUUID().toString()
        t.activeStatus = true
        transferService.insertTransfer(t)
    }
}
