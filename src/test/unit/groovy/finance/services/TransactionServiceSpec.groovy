package finance.services

import finance.domain.*
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import org.hibernate.NonUniqueResultException

import javax.validation.ConstraintViolation
import java.sql.Date

class TransactionServiceSpec extends BaseServiceSpec {
    protected AccountService accountService = new AccountService(accountRepositoryMock, validatorMock, meterServiceMock)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(receiptImageRepositoryMock)
    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterServiceMock)
    protected TransactionService transactionService = new TransactionService(transactionRepositoryMock, accountService, categoryService, receiptImageService, validatorMock, meterServiceMock)

    protected Category category = CategoryBuilder.builder().build()

    void 'test transactionService - deleteByGuid'() {
        given:
        String guid = UUID.randomUUID() // should use GUID generator
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted.is(true)
        1 * transactionRepositoryMock.deleteByGuid(guid)
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - deleteByGuid - no record returned because of invalid guid'() {
        given:
        String guid = UUID.randomUUID()
        Optional<Transaction> transactionOptional = Optional.empty()

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted.is(false)
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid - duplicates returned'() {
        given:
        String guid = UUID.randomUUID()

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        NonUniqueResultException ex = thrown()
        ex.message.contains("query did not return a unique result")
        1 * transactionRepositoryMock.findByGuid(guid) >> { throw new NonUniqueResultException(2) }
        0 * _
    }

    void 'test transactionService - insert valid transaction'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> accountOptional
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> true
        1 * meterServiceMock.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - attempt to insert duplicate transaction - update is called'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        constraintViolations.size() == 0
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> Optional.of(new Category())
        1 * transactionRepositoryMock.saveAndFlush({ Transaction entity ->
            assert entity.transactionDate == transaction.transactionDate
            assert entity.category == transaction.category
            assert entity.accountNameOwner == transaction.accountNameOwner
            assert entity.guid == transaction.guid
            assert entity.description == transaction.description
        })
        0 * _
    }

    void 'test transactionService - insert valid transaction where account name does exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> accountOptional
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> true
        1 * meterServiceMock.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - insert valid transaction where account name does not exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = transactionService.createDefaultAccount(transaction.accountNameOwner, AccountType.Credit)
        Category category = CategoryBuilder.builder().build()
        category.category = transaction.category
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)
        Set<ConstraintViolation<Account>> constraintViolationsAccount = validator.validate(account)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.saveAndFlush(account) >> true
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> accountOptional
        1 * validatorMock.validate(account) >> constraintViolationsAccount
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> true
        1 * meterServiceMock.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - insert a valid transaction where category name does not exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        Optional<Account> accountOptional = Optional.of(account)
        transaction.guid = guid
        category.category = transaction.category
        category.categoryId = 0
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> accountOptional
        1 * categoryRepositoryMock.findByCategory(transaction.category) >> Optional.empty()
        1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryRepositoryMock.saveAndFlush(category)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> true
        1 * meterServiceMock.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test -- updateTransactionReoccurringState - not reoccurring'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Boolean isUpdated = transactionService.updateTransactionReoccurringFlag(transaction.guid, false)

        then:
        isUpdated.is(true)
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.reoccurring = true
        transaction.transactionState = TransactionState.Cleared
        transaction.notes = 'my note will be removed'

        when:
        List<Transaction> transactions = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactions.size() == 2
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * transactionRepositoryMock.saveAndFlush({ Transaction futureTransaction ->
            assert 365L == (futureTransaction.transactionDate.toLocalDate() - transaction.transactionDate.toLocalDate())
            assert futureTransaction.transactionState == TransactionState.Future
            assert futureTransaction.notes == ''
            assert futureTransaction.reoccurring
            futureTransaction
        }) >> transaction
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring - fortnightly'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.FortNightly
        transaction.reoccurring = true
        transaction.transactionState = TransactionState.Cleared
        transaction.notes = 'my note will be removed'

        when:
        List<Transaction> transactions = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactions.size() == 2
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * transactionRepositoryMock.saveAndFlush({ Transaction futureTransaction ->
            assert 14L == (futureTransaction.transactionDate.toLocalDate() - transaction.transactionDate.toLocalDate())
            assert futureTransaction.transactionState == TransactionState.Future
            assert futureTransaction.notes == ''
            assert futureTransaction.reoccurring
            futureTransaction
        }) >> transaction
        0 * _
    }

    void 'test -- updateTransactionState not cleared and reoccurring - monthly'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.reoccurring = true
        transaction.transactionState = TransactionState.Cleared

        when:
        List<Transaction> transactions = transactionService.updateTransactionState(transaction.guid, TransactionState.Future)

        then:
        transactions.size() == 1
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        0 * _
    }

    void 'test - findAccountsThatRequirePayment'() {
        given:
        Date today = new Date(Calendar.instance.time.time)
        Calendar calendar = Calendar.instance
        calendar.add(Calendar.DAY_OF_MONTH, 15)
        Date todayPlusFifteen = new Date(calendar.time.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        Date todayPlusSixteen = new Date(calendar.time.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        Date todayPlusSeventeen = new Date(calendar.time.time)
        calendar.add(Calendar.DAY_OF_MONTH, 35)
        Date todayPlusPastThirty = new Date(calendar.time.time)
        Account account1 = new Account(accountNameOwner: 'test1', accountType: AccountType.Credit)
        Account account2 = new Account(accountNameOwner: 'test2', accountType: AccountType.Credit, totals: new BigDecimal(2), totalsBalanced: new BigDecimal(2))
        Account account3 = new Account(accountNameOwner: 'test3', accountType: AccountType.Credit, totalsBalanced: new BigDecimal(5))
        Transaction transaction1 = new Transaction(accountNameOwner: 'test1', transactionState: TransactionState.Future, transactionDate: todayPlusPastThirty, amount: new BigDecimal(2.01))
        Transaction transaction2 = new Transaction(accountNameOwner: 'test2', transactionState: TransactionState.Future, transactionDate: todayPlusFifteen, amount: new BigDecimal(2.02))
        Transaction transaction3 = new Transaction(accountNameOwner: 'test1', transactionState: TransactionState.Outstanding, transactionDate: todayPlusPastThirty, amount: new BigDecimal(4.03))
        Transaction transaction4 = new Transaction(accountNameOwner: 'test3', transactionState: TransactionState.Future, transactionDate: todayPlusSeventeen, amount: new BigDecimal(3.04))
        Transaction transaction5 = new Transaction(accountNameOwner: 'test2', transactionState: TransactionState.Future, transactionDate: todayPlusSixteen, amount: new BigDecimal(2.05))
        Transaction transaction6 = new Transaction(accountNameOwner: 'test1', amount: new BigDecimal(2.05))

        when:
        List<Account> accounts = transactionService.findAccountsThatRequirePayment()

        then:
        accounts.size() == 3
        1 * accountRepositoryMock.updateTheGrandTotalForAllClearedTransactions()
        1 * accountRepositoryMock.updateTheGrandTotalForAllTransactions()
        1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> [account1, account2, account3]
        1 * accountRepositoryMock.findByActiveStatusAndAccountTypeAndTotalsIsGreaterThanOrderByAccountNameOwner(true, AccountType.Credit, 0) >> [account1, account2, account3]
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test1', true, _) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test2', true, _) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test3', true, _) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        0 * _
    }
}
