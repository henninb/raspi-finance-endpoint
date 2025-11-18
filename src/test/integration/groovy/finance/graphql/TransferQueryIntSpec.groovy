package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.Transfer
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import finance.services.TransferService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date
import java.util.UUID

class TransferQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    TransferService transferService

    @Shared @Autowired
    AccountRepository accountRepository

    @Shared @Autowired
    TransferRepository transferRepository

    @Shared @Autowired
    GraphQLQueryController queryController

    @Shared
    String sourceAccountName

    @Shared
    String destinationAccountName

    def setupSpec() {
        def cleanOwner = testOwner.replaceAll(/[^a-z]/, '').toLowerCase()
        sourceAccountName = "transfersrc_${cleanOwner}"
        destinationAccountName = "transferdst_${cleanOwner}"

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

    def "fetch all transfers via query controller"() {
        given:
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("100.00"))
        createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("200.00"))

        when:
        def transfers = queryController.transfers()
        def scoped = transfers.findAll { it.sourceAccount == sourceAccountName && it.destinationAccount == destinationAccountName }

        then:
        scoped.size() >= 2
        scoped.any { it.amount == new BigDecimal("100.00") }
        scoped.any { it.amount == new BigDecimal("200.00") }
    }

    def "fetch transfer by ID via query controller"() {
        given:
        def savedTransfer = createTestTransfer(sourceAccountName, destinationAccountName, new BigDecimal("150.00"))

        when:
        def result = queryController.transfer(savedTransfer.transferId)

        then:
        result != null
        result.transferId == savedTransfer.transferId
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("150.00")
    }

    def "handle transfer not found via query controller"() {
        expect:
        queryController.transfer(999L) == null
    }

    private Transfer createTestTransfer(String sourceAccount, String destinationAccount, BigDecimal amount) {
        def t = new Transfer()
        t.sourceAccount = sourceAccount
        t.destinationAccount = destinationAccount
        t.transactionDate = Date.valueOf("2024-01-01").toLocalDate()
        t.amount = amount
        t.guidSource = UUID.randomUUID().toString()
        t.guidDestination = UUID.randomUUID().toString()
        t.activeStatus = true
        transferService.insertTransfer(t)
    }
}
