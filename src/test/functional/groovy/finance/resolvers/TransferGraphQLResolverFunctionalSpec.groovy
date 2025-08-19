package finance.resolvers

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transfer
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import finance.repositories.TransactionRepository
import finance.services.ITransferService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.UUID

@ActiveProfiles("func")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class TransferGraphQLResolverFunctionalSpec extends Specification {

    @Autowired
    ITransferService transferService

    @Autowired
    MeterRegistry meterRegistry

    @Autowired
    AccountRepository accountRepository

    @Autowired
    TransferRepository transferRepository

    @Autowired
    TransactionRepository transactionRepository

    TransferGraphQLResolver transferGraphQLResolver

    void setup() {
        transferGraphQLResolver = new TransferGraphQLResolver(transferService, meterRegistry)
        cleanup()
    }

    void cleanup() {
        transferRepository.deleteAll()
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
    }

    void setupTestAccounts() {
        // Create test accounts
        Account sourceAccount = new Account()
        sourceAccount.accountNameOwner = "transfersource_brian"
        sourceAccount.accountType = AccountType.Debit
        sourceAccount.activeStatus = true
        sourceAccount.moniker = "1001"
        sourceAccount.outstanding = new BigDecimal("0.00")
        sourceAccount.future = new BigDecimal("0.00")
        sourceAccount.cleared = new BigDecimal("1000.00")
        sourceAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        sourceAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(sourceAccount)

        Account destinationAccount = new Account()
        destinationAccount.accountNameOwner = "transferdest_brian"
        destinationAccount.accountType = AccountType.Debit
        destinationAccount.activeStatus = true
        destinationAccount.moniker = "1002"
        destinationAccount.outstanding = new BigDecimal("0.00")
        destinationAccount.future = new BigDecimal("0.00")
        destinationAccount.cleared = new BigDecimal("500.00")
        destinationAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        destinationAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(destinationAccount)
    }

    def "should fetch all transfers via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "existing transfers in the database"
        createTestTransfer("transfersource_brian", "transferdest_brian", new BigDecimal("100.00"))
        createTestTransfer("transfersource_brian", "transferdest_brian", new BigDecimal("200.00"))

        when: "transfers data fetcher is called"
        def dataFetcher = transferGraphQLResolver.transfers
        def transfers = dataFetcher.get(null)

        then: "should return all transfers from database in functional environment"
        transfers.size() == 2
        transfers.every { it instanceof Transfer }
        transfers.any { it.amount == new BigDecimal("100.00") }
        transfers.any { it.amount == new BigDecimal("200.00") }
    }

    def "should create transfer via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "transfer input data"
        def transferInput = [
            sourceAccount: "transfersource_brian",
            destinationAccount: "transferdest_brian",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("250.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> transferInput }] as graphql.schema.DataFetchingEnvironment

        when: "create transfer mutation is called"
        def dataFetcher = transferGraphQLResolver.createTransfer()
        def result = dataFetcher.get(environment)

        then: "should create and return transfer from database in functional environment"
        result != null
        result.transferId > 0
        result.sourceAccount == "transfersource_brian"
        result.destinationAccount == "transferdest_brian"
        result.amount == new BigDecimal("250.00")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus == true

        and: "transfer should be persisted in database"
        def savedTransfer = transferRepository.findByTransferId(result.transferId)
        savedTransfer.isPresent()
        savedTransfer.get().sourceAccount == "transfersource_brian"
    }

    def "should fetch transfer by ID via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "an existing transfer in the database"
        def savedTransfer = createTestTransfer("transfersource_brian", "transferdest_brian", new BigDecimal("150.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedTransfer.transferId }] as graphql.schema.DataFetchingEnvironment

        when: "transfer data fetcher is called"
        def dataFetcher = transferGraphQLResolver.transfer()
        def result = dataFetcher.get(environment)

        then: "should return the specific transfer from database in functional environment"
        result != null
        result.transferId == savedTransfer.transferId
        result.sourceAccount == "transfersource_brian"
        result.destinationAccount == "transferdest_brian"
        result.amount == new BigDecimal("150.00")
    }

    def "should delete transfer via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "an existing transfer in the database"
        def savedTransfer = createTestTransfer("transfersource_brian", "transferdest_brian", new BigDecimal("75.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedTransfer.transferId }] as graphql.schema.DataFetchingEnvironment

        when: "delete transfer mutation is called"
        def dataFetcher = transferGraphQLResolver.deleteTransfer()
        def result = dataFetcher.get(environment)

        then: "should successfully delete transfer and return true in functional environment"
        result == true

        and: "transfer should be removed from database"
        def deletedTransfer = transferRepository.findByTransferId(savedTransfer.transferId)
        !deletedTransfer.isPresent()
    }

    def "should handle validation errors for invalid transfer creation in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "invalid transfer input data (non-existent source account)"
        def transferInput = [
            sourceAccount: "nonexistent_brian",
            destinationAccount: "transferdest_brian",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> transferInput }] as graphql.schema.DataFetchingEnvironment

        when: "create transfer mutation is called"
        def dataFetcher = transferGraphQLResolver.createTransfer()
        dataFetcher.get(environment)

        then: "should throw runtime exception in functional environment"
        thrown(RuntimeException)
    }

    def "should successfully execute GraphQL operations in functional environment without authentication issues"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "an existing transfer in the database"
        createTestTransfer("transfersource_brian", "transferdest_brian", new BigDecimal("50.00"))

        when: "transfers data fetcher is called"
        def dataFetcher = transferGraphQLResolver.transfers
        def transfers = dataFetcher.get(null)

        then: "should execute successfully in functional environment"
        transfers != null
        transfers.size() >= 1
        transfers.any { it.amount == new BigDecimal("50.00") }
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