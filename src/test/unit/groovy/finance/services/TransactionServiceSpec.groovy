package finance.services

import finance.domain.*
import finance.helpers.AccountBuilder
import finance.helpers.CategoryBuilder
import finance.helpers.ReceiptImageBuilder
import finance.helpers.TransactionBuilder
import org.hibernate.NonUniqueResultException
import org.springframework.util.ResourceUtils
import spock.lang.Ignore

import javax.validation.ConstraintViolation
import java.sql.Date

import static finance.utils.Constants.*

@SuppressWarnings("GroovyAccessibility")
class TransactionServiceSpec extends BaseServiceSpec {

    void setup() {
        transactionService.validator = validatorMock
        transactionService.meterService = meterService
    }

    protected Category category = CategoryBuilder.builder().build()

    void 'test transactionService - deleteByGuid'() {
        given:
        String guid = UUID.randomUUID() // should use GUID generator
        Transaction transaction = new Transaction()
        Optional<Transaction> transactionOptional = Optional.of(transaction)

        when:
        Boolean isDeleted = transactionService.deleteTransactionByGuid(guid)

        then:
        isDeleted
        1 * transactionRepositoryMock.delete(transaction)
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
        !isDeleted
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
        Transaction transactionInserted = transactionService.insertTransaction(transaction)

        then:
        transactionInserted.guid == transaction.guid
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * accountServiceMock.account(transaction.accountNameOwner) >> accountOptional
        1 * categoryServiceMock.category(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    //TODO: might not be working as expected
    void 'test transactionService - attempt to insert duplicate transaction - update is called'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Optional<Transaction> transactionOptional = Optional.of(transaction)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Transaction transactionInserted = transactionService.insertTransaction(transaction)

        then:
        transactionInserted.guid == transaction.guid
        constraintViolations.size() == 0
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * transactionRepositoryMock.findByGuid(guid) >> transactionOptional
        1 * accountServiceMock.account(transaction.getAccountNameOwner()) >> Optional.of(AccountBuilder.builder().build())
        1 * categoryServiceMock.category(transaction.category) >> Optional.of(new Category())
        1 * transactionRepositoryMock.saveAndFlush({ Transaction entity ->
            assert entity.transactionDate == transaction.transactionDate
            assert entity.category == transaction.category
            assert entity.accountNameOwner == transaction.accountNameOwner
            assert entity.guid == transaction.guid
            assert entity.description == transaction.description
        }) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_ALREADY_EXISTS_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
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
        Transaction transactionInserted = transactionService.insertTransaction(transaction)

        then:
        transactionInserted.guid == transaction.guid
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * accountServiceMock.account(transaction.accountNameOwner) >> accountOptional
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * categoryServiceMock.category(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test transactionService - insert valid transaction where account name does not exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = transactionService.createDefaultAccount(transaction.accountNameOwner, AccountType.Credit)
        Category category = CategoryBuilder.builder().build()
        category.categoryName = transaction.category
        Optional<Account> accountOptional = Optional.of(account)
        Optional<Category> categoryOptional = Optional.of(category)
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Transaction transactionInserted = transactionService.insertTransaction(transaction)

        then:
        transactionInserted.guid == transaction.guid
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * accountServiceMock.account(transaction.accountNameOwner) >> Optional.empty()
        1 * accountServiceMock.account(transaction.accountNameOwner) >> accountOptional
        1 * accountServiceMock.insertAccount(account) >> account
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * categoryServiceMock.category(transaction.category) >> categoryOptional
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test transactionService - insert a valid transaction where category name does not exist'() {
        given:
        String guid = UUID.randomUUID()
        Transaction transaction = TransactionBuilder.builder().withGuid(guid).build()
        Account account = new Account()
        Optional<Account> accountOptional = Optional.of(account)
        transaction.guid = guid
        category.categoryName = transaction.category
        category.categoryId = 0
        Set<ConstraintViolation<Transaction>> constraintViolations = validator.validate(transaction)

        when:
        Transaction transactionInserted = transactionService.insertTransaction(transaction)

        then:
        transactionInserted.guid == transaction.guid
        constraintViolations.size() == 0
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()
        1 * validatorMock.validate(transaction) >> constraintViolations
        1 * accountServiceMock.account(transaction.accountNameOwner) >> accountOptional
        1 * categoryServiceMock.category(transaction.category) >> Optional.empty()
        //1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryServiceMock.insertCategory(category) >> category
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.transactionState = TransactionState.Undefined
        transaction.notes = 'my note will be removed'

        when:
        Transaction transactionInserted = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactionInserted.guid == transaction.guid
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- updateTransactionState cleared and reoccurring - fortnightly'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.FortNightly
        transaction.transactionState = TransactionState.Undefined
        transaction.notes = 'my note will be removed'

        when:
        Transaction transactionInserted = transactionService.updateTransactionState(transaction.guid, TransactionState.Cleared)

        then:
        transactionInserted.guid == transaction.guid
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test -- updateTransactionState not cleared and reoccurring - monthly'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.transactionState = TransactionState.Cleared

        when:
        Transaction transactionInserted = transactionService.updateTransactionState(transaction.guid, TransactionState.Future)

        then:
        transactionInserted.guid == transaction.guid
        transactionInserted.transactionState == TransactionState.Future
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test updateTransactionReceiptImageByGuid'() {
        given:
        Transaction transaction = TransactionBuilder.builder().build()
        transaction.transactionId = 1
        ReceiptImage receiptImage = ReceiptImageBuilder.builder().build()
        receiptImage.receiptImageId = 1
        //Set<ConstraintViolation<ReceiptImage>> constraintViolations = validator.validate(receiptImage)
        String base64Jpeg = '/9j/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/yQALCAABAAEBAREA/8wABgAQEAX/2gAIAQEAAD8A0s8g/9k='
        byte[] jpgFileBytes = ResourceUtils.getFile("${baseName}/src/test/unit/resources/viking-icon.jpg").getBytes()
        base64Jpeg = Base64.getEncoder().encodeToString(jpgFileBytes)

        when:
        ReceiptImage receiptImageInserted = transactionService.updateTransactionReceiptImageByGuid(transaction.guid, base64Jpeg)

        then:

        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(transaction)
        1 * receiptImageServiceMock.insertReceiptImage(_ as ReceiptImage) >> receiptImage
        1 * transactionRepositoryMock.saveAndFlush(transaction)
        1 * meterRegistryMock.counter(setMeterId(TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER, transaction.accountNameOwner)) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'create Future Transaction with jan 1 of leap year'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(Date.valueOf('2020-01-01'))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == Date.valueOf('2021-01-01')
        0 * _
    }

    void 'create Future Transaction with Feb 29'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(Date.valueOf('2020-02-29'))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == Date.valueOf('2021-02-28')
        0 * _
    }

    void 'create Future Transaction with leap year in play'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(Date.valueOf('2019-03-01'))
                .withReoccurringType(ReoccurringType.Monthly)
                .build()

        when:
        Transaction result = transactionService.createFutureTransaction(preLeapYearTransaction)

        then:
        result.transactionDate == Date.valueOf('2020-03-01')
        0 * _
    }

    void 'create Future Transaction with reoccurringType undefined'() {
        given:
        Transaction preLeapYearTransaction = TransactionBuilder.builder()
                .withTransactionDate(Date.valueOf('2019-11-01'))
                .withReoccurringType(ReoccurringType.Undefined)
                .build()

        when:
        transactionService.createFutureTransaction(preLeapYearTransaction)


        then:
        thrown(RuntimeException)
        1 * meterRegistryMock.counter(runtimeExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    @Ignore
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
        Account account2 = new Account(accountNameOwner: 'test2', accountType: AccountType.Credit, future: new BigDecimal(2), cleared: new BigDecimal(2))
        Account account3 = new Account(accountNameOwner: 'test3', accountType: AccountType.Credit, cleared: new BigDecimal(5))
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
        1 * accountRepositoryMock.updateTotalsForAllAccounts()
//        1 * accountRepositoryMock.updateTotalsForClearedTransactionState()
//        1 * accountRepositoryMock.updateTotalsForOutstandingTransactionState()
//        1 * accountRepositoryMock.updateTotalsForFutureTransactionState()
        //1 * accountRepositoryMock.findByActiveStatusOrderByAccountNameOwner(true) >> [account1, account2, account3]  //TODO: why is this not triggered?
        1 * accountRepositoryMock.findByActiveStatusAndAccountTypeOrderByAccountNameOwner(true, AccountType.Credit) >> [account1, account2, account3]
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test1', true, [TransactionState.Cleared.toString()]) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]

        //1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test2', true, [TransactionState.Cleared.toString()]) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]

        //1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test2', true, [TransactionState.Cleared.toString()]) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        //1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test2', true, [TransactionState.Cleared.toString()]) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusAndTransactionStateNotInOrderByTransactionDateDesc('test3', true, [TransactionState.Cleared.toString()]) >> [transaction1, transaction2, transaction3, transaction4, transaction5, transaction6]
        0 * _
    }
}
