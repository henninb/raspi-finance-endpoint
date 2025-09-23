package finance.resolvers

import finance.domain.Transfer
import finance.services.StandardizedTransferService
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Date
import java.util.*

class TransferGraphQLResolverSpec extends Specification {

    // Real service with mocked collaborators to avoid mocking final Kotlin classes
    finance.repositories.TransferRepository transferRepository = Mock()
    finance.services.ITransactionService transactionService = Mock()
    finance.repositories.AccountRepository accountRepository = Mock()
    finance.services.StandardizedAccountService accountService = new finance.services.StandardizedAccountService(accountRepository)

    StandardizedTransferService transferService = new StandardizedTransferService(transferRepository, transactionService, accountService)
    MeterRegistry meterRegistry = Mock()
    Counter counter = Mock()

    @Subject
    TransferGraphQLResolver transferGraphQLResolver = new TransferGraphQLResolver(transferService, meterRegistry)

    def setup() {
        // Wire required service dependencies
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        transferService.validator = validator
        accountService.validator = validator

        def meterService = new finance.services.MeterService(meterRegistry)
        transferService.meterService = meterService
        accountService.meterService = meterService

        // Default mock behavior for all counter calls
        meterRegistry.counter(_ as String) >> counter
    }

    def "should fetch all transfers"() {
        given: "a list of transfers"
        def transfer1 = createTestTransfer(1L, "source_account", "dest_account", new BigDecimal("100.00"))
        def transfer2 = createTestTransfer(2L, "account_a", "account_b", new BigDecimal("250.00"))
        def transfers = [transfer1, transfer2]

        and: "repository returns transfers"
        transferRepository.findAll() >> transfers

        when: "transfers data fetcher is called"
        DataFetcher<List<Transfer>> dataFetcher = transferGraphQLResolver.transfers
        DataFetchingEnvironment environment = Mock()
        List<Transfer> result = dataFetcher.get(environment)

        then: "should return all transfers"
        result.size() == 2
        result[0].transferId == 1L
        result[0].sourceAccount == "source_account"
        result[0].destinationAccount == "dest_account"
        result[0].amount == new BigDecimal("100.00")

        result[1].transferId == 2L
        result[1].sourceAccount == "account_a"
        result[1].destinationAccount == "account_b"
        result[1].amount == new BigDecimal("250.00")
    }

    def "should fetch transfer by ID"() {
        given: "a transfer ID"
        def transferId = 1L
        def transfer = createTestTransfer(transferId, "source_account", "dest_account", new BigDecimal("100.00"))

        and: "repository returns the transfer"
        transferRepository.findByTransferId(transferId) >> Optional.of(transfer)

        and: "data fetching environment with transfer ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("transferId") >> transferId

        when: "transfer data fetcher is called"
        DataFetcher<Transfer> dataFetcher = transferGraphQLResolver.transfer()
        Transfer result = dataFetcher.get(environment)

        then: "should return the specific transfer"
        result.transferId == transferId
        result.sourceAccount == "source_account"
        result.destinationAccount == "dest_account"
        result.amount == new BigDecimal("100.00")
    }

    def "should handle transfer not found when fetching by ID"() {
        given: "a non-existent transfer ID"
        def transferId = 999L

        and: "repository returns empty optional"
        transferRepository.findByTransferId(transferId) >> Optional.empty()

        and: "data fetching environment with transfer ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("transferId") >> transferId

        when: "transfer data fetcher is called"
        DataFetcher<Transfer> dataFetcher = transferGraphQLResolver.transfer()
        Transfer result = dataFetcher.get(environment)

        then: "should return null"
        result == null
    }

    def "should create transfer with validation"() {
        given: "transfer input data"
        def transferInput = [
            sourceAccount: "source_account",
            destinationAccount: "dest_account",
            transactionDate: "2023-12-01",
            amount: new BigDecimal("150.00")
        ]

        and: "expected created transfer"
        def createdTransfer = createTestTransfer(1L, "source_account", "dest_account", new BigDecimal("150.00"))
        createdTransfer.transactionDate = Date.valueOf("2023-12-01")

        and: "data fetching environment with transfer input"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("transfer") >> transferInput

        and: "account lookups succeed and repository saves"
        accountRepository.findByAccountNameOwner("source_account") >> Optional.of(new finance.domain.Account(accountNameOwner: "source_account"))
        accountRepository.findByAccountNameOwner("dest_account") >> Optional.of(new finance.domain.Account(accountNameOwner: "dest_account"))
        transactionService.insertTransaction(_ as finance.domain.Transaction) >> { finance.domain.Transaction t -> t }
        transferRepository.save(_ as Transfer) >> { Transfer t -> t.transferId = 1L; return t }

        when: "create transfer mutation is called"
        DataFetcher<Transfer> dataFetcher = transferGraphQLResolver.createTransfer()
        Transfer result = dataFetcher.get(environment)

        then: "should return created transfer"
        result.transferId == 1L
        result.sourceAccount == "source_account"
        result.destinationAccount == "dest_account"
        result.amount == new BigDecimal("150.00")
        result.transactionDate == Date.valueOf("2023-12-01")
    }

    def "should handle validation errors during transfer creation"() {
        given: "invalid transfer input data"
        def transferInput = [
            sourceAccount: "", // Invalid empty source account
            destinationAccount: "dest_account",
            transactionDate: "2023-12-01",
            amount: new BigDecimal("-100.00") // Invalid negative amount
        ]

        and: "account lookup fails to trigger error path"
        accountRepository.findByAccountNameOwner("") >> Optional.empty()

        and: "data fetching environment with invalid transfer input"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("transfer") >> transferInput

        when: "create transfer mutation is called"
        DataFetcher<Transfer> dataFetcher = transferGraphQLResolver.createTransfer()
        dataFetcher.get(environment)

        then: "should throw runtime exception"
        thrown(RuntimeException)
    }

    def "should increment metrics counters on successful operations"() {
        given: "a list of transfers"
        def transfers = [createTestTransfer(1L, "source_account", "dest_account", new BigDecimal("100.00"))]
        transferRepository.findAll() >> transfers

        when: "transfers data fetcher is called"
        DataFetcher<List<Transfer>> dataFetcher = transferGraphQLResolver.transfers
        DataFetchingEnvironment environment = Mock()
        dataFetcher.get(environment)

        then: "should increment success counter"
        1 * meterRegistry.counter("graphql.transfers.fetch.success") >> counter
        1 * counter.increment()
    }

    def "should increment metrics counters on failed operations"() {
        given: "repository throws exception"
        transferRepository.findAll() >> { throw new RuntimeException("Database error") }

        when: "transfers data fetcher is called"
        DataFetcher<List<Transfer>> dataFetcher = transferGraphQLResolver.transfers
        DataFetchingEnvironment environment = Mock()
        dataFetcher.get(environment)

        then: "should throw exception and increment error counter"
        thrown(RuntimeException)
        1 * meterRegistry.counter("graphql.transfers.fetch.error") >> counter
        1 * counter.increment()
    }

    private Transfer createTestTransfer(Long id, String sourceAccount, String destAccount, BigDecimal amount) {
        def transfer = new Transfer()
        transfer.transferId = id
        transfer.sourceAccount = sourceAccount
        transfer.destinationAccount = destAccount
        transfer.amount = amount
        transfer.transactionDate = Date.valueOf("2023-12-01")
        transfer.guidSource = UUID.randomUUID().toString()
        transfer.guidDestination = UUID.randomUUID().toString()
        transfer.activeStatus = true
        return transfer
    }
}
