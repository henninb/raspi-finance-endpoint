package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.TransactionInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.services.StandardizedAccountService
import finance.services.StandardizedTransactionService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class TransactionMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    StandardizedTransactionService transactionService

    @Autowired
    StandardizedAccountService accountService

    def "createTransaction mutation succeeds with valid input"() {
        given:
        withUserRole()
        createTestAccount("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                null,                           // transactionId
                null,                           // guid
                null,                           // accountId
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "grocery store",
                "groceries",
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                null,                           // activeStatus
                null,                           // reoccurringType
                null,                           // notes
                null,                           // dueDate
                null                            // receiptImageId
        )

        when:
        def result = mutationController.createTransaction(transactionInput)

        then:
        result != null
        result.transactionId > 0
        result.accountNameOwner == "checking_primary"
        result.accountType == AccountType.Debit
        result.transactionType == TransactionType.Expense
        result.description == "grocery store"
        result.category == "groceries"
        result.amount == new BigDecimal("100.50")
        result.transactionState == TransactionState.Outstanding
        result.guid != null
    }

    def "createTransaction mutation fails validation for empty description"() {
        given:
        withUserRole()
        createTestAccount("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                null,
                null,
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "",                             // invalid: empty
                "groceries",
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createTransaction(transactionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createTransaction mutation fails validation for empty category"() {
        given:
        withUserRole()
        createTestAccount("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                null,
                null,
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "grocery store",
                "",                             // invalid: empty
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createTransaction(transactionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createTransaction mutation fails validation for invalid accountNameOwner with spaces"() {
        given:
        withUserRole()
        def transactionInput = new TransactionInputDto(
                null,
                null,
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "invalid account",              // invalid: contains space
                Date.valueOf("2024-01-15"),
                "grocery store",
                "groceries",
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createTransaction(transactionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createTransaction mutation fails validation for invalid amount precision"() {
        given:
        withUserRole()
        createTestAccount("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                null,
                null,
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "grocery store",
                "groceries",
                new BigDecimal("100.123"),      // invalid: 3 decimal places
                TransactionState.Outstanding,
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.createTransaction(transactionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateTransaction mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestTransaction("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                created.transactionId,
                created.guid,
                created.accountId,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-20"),     // changed date
                "updated store",                // changed description
                "shopping",                     // changed category
                new BigDecimal("250.75"),       // changed amount
                TransactionState.Cleared,       // changed state
                null,
                null,
                "Updated notes",                // added notes
                null,
                null
        )

        when:
        def result = mutationController.updateTransaction(transactionInput)

        then:
        result != null
        result.transactionId == created.transactionId
        result.guid == created.guid
        result.transactionDate == Date.valueOf("2024-01-20")
        result.description == "updated store"
        result.category == "shopping"
        result.amount == new BigDecimal("250.75")
        result.transactionState == TransactionState.Cleared
        result.notes == "Updated notes"
    }

    def "updateTransaction mutation fails for non-existent transaction"() {
        given:
        withUserRole()
        def transactionInput = new TransactionInputDto(
                999999L,                        // non-existent ID
                "00000000-0000-0000-0000-000000000000",
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "grocery store",
                "groceries",
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                null,
                null,
                null,
                null,
                null
        )

        when:
        mutationController.updateTransaction(transactionInput)

        then:
        thrown(RuntimeException)
    }

    def "deleteTransaction mutation returns true for existing transaction"() {
        given:
        withUserRole()
        def created = createTestTransaction("checking_primary", AccountType.Debit)

        when:
        def deleted = mutationController.deleteTransaction(created.guid)

        then:
        deleted == true
    }

    def "deleteTransaction mutation returns false for missing transaction"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteTransaction("00000000-0000-0000-0000-000000000000") == false
    }

    def "createTransaction with optional fields succeeds"() {
        given:
        withUserRole()
        createTestAccount("checking_primary", AccountType.Debit)
        def transactionInput = new TransactionInputDto(
                null,
                null,
                null,
                AccountType.Debit,
                TransactionType.Expense,
                "checking_primary",
                Date.valueOf("2024-01-15"),
                "grocery store",
                "groceries",
                new BigDecimal("100.50"),
                TransactionState.Outstanding,
                true,                           // activeStatus
                ReoccurringType.Monthly,        // reoccurringType
                "Monthly grocery bill",         // notes
                Date.valueOf("2024-02-15"),     // dueDate
                null
        )

        when:
        def result = mutationController.createTransaction(transactionInput)

        then:
        result != null
        result.activeStatus == true
        result.reoccurringType == ReoccurringType.Monthly
        result.notes == "Monthly grocery bill"
        result.dueDate == Date.valueOf("2024-02-15")
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

    private Transaction createTestTransaction(String accountNameOwner, AccountType accountType) {
        createTestAccount(accountNameOwner, accountType)

        Transaction transaction = new Transaction()
        transaction.transactionId = 0L
        transaction.guid = UUID.randomUUID().toString()
        transaction.accountNameOwner = accountNameOwner
        transaction.accountType = accountType
        transaction.transactionType = TransactionType.Expense
        transaction.transactionDate = Date.valueOf("2024-01-15")
        transaction.description = "test store"
        transaction.category = "test"
        transaction.amount = new BigDecimal("100.00")
        transaction.transactionState = TransactionState.Outstanding
        transaction.activeStatus = true
        transaction.dateAdded = new Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = new Timestamp(System.currentTimeMillis())

        def result = transactionService.save(transaction)
        return result.data
    }
}
