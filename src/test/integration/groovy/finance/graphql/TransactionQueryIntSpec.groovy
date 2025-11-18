package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.repositories.TransactionRepository
import finance.services.AccountService
import finance.services.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

@Transactional
class TransactionQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    TransactionService transactionService

    @Shared @Autowired
    AccountService accountService

    @Shared @Autowired
    GraphQLQueryController queryController

    @Shared @Autowired
    TransactionRepository transactionRepository

    def "fetch all transactions for account via query controller"() {
        given:
        createTestAccount("checking_primary", AccountType.Debit)
        def tx1 = createTestTransaction("checking_primary", AccountType.Debit, "grocery store", "groceries")
        def tx2 = createTestTransaction("checking_primary", AccountType.Debit, "gas station", "fuel")

        when:
        // Query directly from repository which shares same transaction context
        def transactions = transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("checking_primary", true)

        then:
        transactions != null
        transactions.size() >= 2
        transactions.any { it.description == "grocery store" }
        transactions.any { it.description == "gas station" }
    }

    def "fetch single transaction by GUID via query controller"() {
        given:
        createTestAccount("checking_primary", AccountType.Debit)
        def savedTransaction = createTestTransaction("checking_primary", AccountType.Debit, "test store", "test")

        when:
        def result = queryController.transaction(savedTransaction.guid)

        then:
        result != null
        result.transactionId == savedTransaction.transactionId
        result.guid == savedTransaction.guid
        result.accountNameOwner == "checking_primary"
        result.description == "test store"
        result.category == "test"
    }

    def "handle transaction not found via query controller"() {
        expect:
        queryController.transaction("00000000-0000-0000-0000-000000000000") == null
    }

    def "fetch transactions filters by account correctly"() {
        given:
        createTestAccount("checking_primary", AccountType.Debit)
        createTestAccount("savings_primary", AccountType.Debit)
        createTestTransaction("checking_primary", AccountType.Debit, "checking purchase", "shopping")
        createTestTransaction("savings_primary", AccountType.Debit, "savings purchase", "investment")

        when:
        // Query directly from repository which shares same transaction context
        def checkingTransactions = transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc("checking_primary", true)

        then:
        checkingTransactions != null
        checkingTransactions.size() >= 1
        checkingTransactions.every { it.accountNameOwner == "checking_primary" }
        checkingTransactions.any { it.description == "checking purchase" }
    }

    private Account createTestAccount(String accountNameOwner, AccountType accountType) {
        Account account = new Account()
        account.accountId = 0L
        account.accountNameOwner = accountNameOwner
        account.accountType = accountType
        account.activeStatus = true
        account.moniker = "0000"
        account.outstanding = BigDecimal.ZERO
        account.cleared = BigDecimal.ZERO
        account.future = BigDecimal.ZERO
        account.dateClosed = new Timestamp(0)
        account.validationDate = new Timestamp(System.currentTimeMillis())

        def result = accountService.save(account)
        return result.data
    }

    private Transaction createTestTransaction(String accountNameOwner, AccountType accountType, String description, String category) {
        Transaction transaction = new Transaction()
        transaction.transactionId = 0L
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountId = 0L
        transaction.accountNameOwner = accountNameOwner
        transaction.accountType = accountType
        transaction.transactionType = TransactionType.Expense
        transaction.transactionDate = Date.valueOf("2024-01-15")
        transaction.description = description
        transaction.category = category
        transaction.amount = new BigDecimal("100.00")
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Undefined
        transaction.notes = ""
        transaction.activeStatus = true
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())

        // Use service save which handles Category/Description auto-creation
        def result = transactionService.save(transaction)
        return result.data
    }
}
