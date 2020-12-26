package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.helpers.CategoryBuilder
import finance.helpers.TransactionBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.ReceiptImageRepository
import finance.repositories.TransactionRepository
import org.hibernate.NonUniqueResultException
import spock.lang.Specification

import javax.validation.Validator
import java.sql.Date

class TransactionServiceSpec extends Specification {
    protected TransactionRepository mockTransactionRepository = GroovyMock(TransactionRepository)
    protected AccountRepository mockAccountRepository = GroovyMock(AccountRepository)
    protected Validator mockValidator = GroovyMock(Validator)
    protected MeterService mockMeterService = GroovyMock()
    protected AccountService accountService = new AccountService(mockAccountRepository, mockValidator, mockMeterService)
    protected CategoryRepository mockCategoryRepository = GroovyMock(CategoryRepository)
    protected CategoryService categoryService = new CategoryService(mockCategoryRepository, mockValidator, mockMeterService)
    protected ReceiptImageRepository mockReceiptImageRepository = GroovyMock(ReceiptImageRepository)
    protected ReceiptImageService receiptImageService = new ReceiptImageService(mockReceiptImageRepository)
    protected TransactionService transactionService = new TransactionService(mockTransactionRepository, accountService, categoryService, receiptImageService, mockValidator, mockMeterService)
    protected Category category = CategoryBuilder.builder().build()

    void 'test transactionService - deleteByGuid'() {
        given:
        String guid = '123'  // should use GUID generator
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted.is(true)
        1 * mockTransactionRepository.deleteByGuid(guid)
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - deleteByGuid - no record returned because of invalid guid'() {
        given:
        String guid = '123'
        Optional<Transaction> transactionOptional = Optional.empty()

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted.is(false)
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid'() {
        given:
        String guid = '123'
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        0 * _
    }

    void 'test transactionService - findByGuid - duplicates returned'() {
        given:
        String guid = '123'

        when:
        transactionService.findTransactionByGuid(guid)

        then:
        NonUniqueResultException ex = thrown()
        ex.message.contains("query did not return a unique result")
        1 * mockTransactionRepository.findByGuid(guid) >> { throw new NonUniqueResultException(2) }
        0 * _
    }

    void 'test transactionService - insert valid transaction'() {
        given:
        String categoryName = 'my-category'
        String accountName = 'my-account-name'
        String guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockValidator.validate(transaction) >> ([] as Set)
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        1 * mockMeterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - attempt to insert duplicate transaction - update is called'() {
        given:
        String categoryName = 'my-category'
        String accountName = 'my-account-name'
        String guid = '123'
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockValidator.validate(transaction) >> ([] as Set)
        1 * mockTransactionRepository.findByGuid(guid) >> transactionOptional
        1 * mockCategoryRepository.findByCategory('my-category') >> Optional.of(new Category())
        1 * mockTransactionRepository.saveAndFlush({ Transaction entity ->
            assert entity.transactionDate == transaction.transactionDate
            assert entity.category == transaction.category
            assert entity.accountNameOwner == transaction.accountNameOwner
            assert entity.guid == transaction.guid
            assert entity.description == transaction.description
        } )
        0 * _
    }

    void 'test transactionService - insert valid transaction where account name does exist'() {
        given:
        String categoryName = 'my-category'
        String accountName = 'my-account-name'
        String guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Category category = new Category()
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockValidator.validate(transaction) >> ([] as Set)
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        1 * mockMeterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - insert valid transaction where account name does not exist'() {
        given:
        String categoryName = 'my-category'
        String accountName = 'my-account-name'
        String guid = '123'
        Transaction transaction = new Transaction()
        Account account = transactionService.createDefaultAccount(accountName, AccountType.Undefined)
        Category category = CategoryBuilder.builder().build()
        category.category = categoryName
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> Optional.empty()
        1 * mockAccountRepository.saveAndFlush(account) >> true
        1 * mockValidator.validate(transaction) >> ([] as Set)
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> Optional.empty()
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockValidator.validate(account) >> ([] as Set)
        1 * mockCategoryRepository.findByCategory(categoryName) >> categoryOptional
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        1 * mockMeterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test transactionService - insert a valid transaction where category name does not exist'() {
        given:
        String categoryName = 'my-category'
        String accountName = 'my-account-name'
        String guid = '123'
        Transaction transaction = new Transaction()
        Account account = new Account()
        Optional<Account> accountOptional = Optional.of(account)
        transaction.guid = guid
        transaction.accountNameOwner = accountName
        transaction.category = categoryName
        category.category = categoryName
        category.categoryId = 0

        when:
        Boolean isInserted = transactionService.insertTransaction(transaction)

        then:
        isInserted.is(true)
        1 * mockTransactionRepository.findByGuid(guid) >> Optional.empty()
        1 * mockValidator.validate(transaction) >> ([] as Set)
        1 * mockAccountRepository.findByAccountNameOwner(accountName) >> accountOptional
        1 * mockCategoryRepository.findByCategory(categoryName) >> Optional.empty()
        1 * mockValidator.validate(category) >> ([] as Set)
        1 * mockCategoryRepository.saveAndFlush(category)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> true
        1 * mockMeterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        0 * _
    }

    void 'test -- updateTransactionReoccurringState - not reoccurring'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()

        when:
        Boolean isUpdated = transactionService.updateTransactionReoccurringFlag(transaction.guid, false)

        then:
        isUpdated.is(true)
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockTransactionRepository.saveAndFlush(transaction)
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
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> transaction
        1 * mockTransactionRepository.saveAndFlush({ Transaction futureTransaction ->
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
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> transaction
        1 * mockTransactionRepository.saveAndFlush({ Transaction futureTransaction ->
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
        1 * mockTransactionRepository.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * mockTransactionRepository.saveAndFlush(transaction) >> transaction
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
        Account account2 = new Account(accountNameOwner: 'test2', accountType: AccountType.Credit, totals: new BigDecimal(2), totalsBalanced: new BigDecimal(2) )
        Account account3 = new Account(accountNameOwner: 'test3', accountType: AccountType.Credit, totalsBalanced: new BigDecimal(5))
        Transaction transaction1 = new Transaction(accountNameOwner: 'test1', transactionState: TransactionState.Future, transactionDate: todayPlusPastThirty, amount: new BigDecimal(2.01))
        Transaction transaction2 = new Transaction(accountNameOwner: 'test2', transactionState: TransactionState.Future, transactionDate: todayPlusFifteen, amount: new BigDecimal(2.02))
        Transaction transaction3 = new Transaction(accountNameOwner: 'test1', transactionState: TransactionState.Outstanding, transactionDate: todayPlusPastThirty , amount: new BigDecimal(4.03))
        Transaction transaction4 = new Transaction(accountNameOwner: 'test3', transactionState: TransactionState.Future, transactionDate: todayPlusSeventeen, amount: new BigDecimal(3.04))
        Transaction transaction5 = new Transaction(accountNameOwner: 'test2', transactionState: TransactionState.Future, transactionDate: todayPlusSixteen, amount: new BigDecimal(2.05))
        Transaction transaction6 = new Transaction(accountNameOwner: 'test1', amount: new BigDecimal(2.05))

        when:
        List<Account> accounts = transactionService.findAccountsThatRequirePayment()

        then:
        accounts.size() == 1
        1 * mockAccountRepository.findByActiveStatusOrderByAccountNameOwner(true) >> [account1, account2, account3]
        1 * mockTransactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc('test3', true) >> [transaction1, transaction2, transaction3,transaction4, transaction5,transaction6]
        0 * _
    }
}
