package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.helpers.SmartTransactionBuilder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date
import java.util.Optional

/**
 * MIGRATED INTEGRATION TEST - Robust, isolated TransactionRepository tests
 *
 * Uses BaseIntegrationSpec + TestFixtures contexts + SmartTransactionBuilder
 * to avoid brittle hardcoded names and shared state.
 */
class TransactionRepositoryMigratedIntSpec extends BaseIntegrationSpec {

    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    def repositoryContext

    @Shared
    Long primaryAccountId

    @Shared
    String ownerAccountName

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
        ownerAccountName = repositoryContext.primaryAccountName
        // Ensure account exists using repository; create via SmartBuilder if missing
        Optional<Account> acc = accountRepository.findByAccountNameOwner(ownerAccountName)
        if (!acc.isPresent()) {
            def acct = finance.helpers.SmartAccountBuilder.builderForOwner(testOwner)
                    .withAccountNameOwner(ownerAccountName)
                    .asDebit()
                    .buildAndValidate()
            acct = accountRepository.save(acct)
            primaryAccountId = acct.accountId
        } else {
            primaryAccountId = acc.get().accountId
        }
    }

    void 'basic CRUD with smart builder'() {
        given:
        Transaction txn = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-01-01"))
                .withDescription("test transaction")
                .withCategory("test_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}")
                .withAmount("100.50")
                .asCleared()
                .buildAndValidate()

        when:
        Transaction saved = transactionRepository.save(txn)

        then:
        saved.transactionId != null
        saved.guid == txn.guid
        saved.amount == new BigDecimal("100.50")

        when:
        Optional<Transaction> found = transactionRepository.findByGuid(txn.guid)

        then:
        found.isPresent()
        found.get().description == "test transaction"
    }

    void 'find by account name owner and active status ordered by date desc'() {
        given:
        (1..3).each { i ->
            Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-01-0${i}"))
                    .withDescription("txn_${i}")
                    .withCategory("cat_${i}")
                    .withAmount(new BigDecimal(100 + i))
                    .asCleared()
                    .buildAndValidate()
            transactionRepository.save(t)
        }

        when:
        List<Transaction> list = transactionRepository
                .findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(ownerAccountName, true)

        then:
        list.size() >= 3
        list[0].transactionDate.after(list[1].transactionDate)
        list.every { it.accountNameOwner == ownerAccountName && it.activeStatus }
    }

    void 'find by category and by description'() {
        given:
        Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-02-01"))
                .withDescription("grocery shopping")
                .withCategory("groceries")
                .withAmount("85.50")
                .asCleared()
                .buildAndValidate()
        transactionRepository.save(t)

        when:
        List<Transaction> byCat = transactionRepository
                .findByCategoryAndActiveStatusOrderByTransactionDateDesc("groceries", true)
        List<Transaction> byDesc = transactionRepository
                .findByDescriptionAndActiveStatusOrderByTransactionDateDesc("grocery shopping", true)

        then:
        byCat.any { it.category == "groceries" }
        byDesc.any { it.description == "grocery shopping" }
    }

    void 'sum totals for active transactions by account owner'() {
        given:
        [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future].each { state ->
            Transaction t = SmartTransactionBuilder.builderForOwner(testOwner)
                    .withAccountId(primaryAccountId)
                    .withAccountType(AccountType.Debit)
                    .withTransactionType(TransactionType.Expense)
                    .withAccountNameOwner(ownerAccountName)
                    .withTransactionDate(Date.valueOf("2023-03-01"))
                    .withDescription("sum_${state}")
                    .withCategory("sumcat")
                    .withAmount("100.00")
                    .withTransactionState(state)
                    .asActive()
                    .buildAndValidate()
            transactionRepository.save(t)
        }

        when:
        List<Object[]> results = transactionRepository
                .sumTotalsForActiveTransactionsByAccountNameOwner(ownerAccountName)

        then:
        results.size() >= 3
        results.each { row ->
            assert row.length == 3
            assert row[0] != null && row[1] != null && row[2] != null
        }
    }

    void 'find by account name owner excluding states'() {
        given:
        Transaction cleared = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-04-01"))
                .withDescription("cleared")
                .withCategory("testcat")
                .withAmount("100.00")
                .asCleared()
                .buildAndValidate()

        Transaction future = SmartTransactionBuilder.builderForOwner(testOwner)
                .withAccountId(primaryAccountId)
                .withAccountType(AccountType.Debit)
                .withTransactionType(TransactionType.Expense)
                .withAccountNameOwner(ownerAccountName)
                .withTransactionDate(Date.valueOf("2023-04-02"))
                .withDescription("future")
                .withCategory("testcat")
                .withAmount("100.00")
                .asFuture()
                .buildAndValidate()

        transactionRepository.save(cleared)
        transactionRepository.save(future)

        when:
        List<Transaction> nonFuture = transactionRepository
                .findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc(
                        ownerAccountName, true, [TransactionState.Future])

        then:
        nonFuture.size() >= 1
        nonFuture.every { it.transactionState != TransactionState.Future }
        nonFuture.any { it.description == "cleared" }
    }
}
