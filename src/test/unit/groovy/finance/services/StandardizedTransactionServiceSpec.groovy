package finance.services

import finance.domain.*
import finance.repositories.TransactionRepository
import finance.repositories.CategoryRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class StandardizedTransactionServiceSpec extends BaseServiceSpec {

    // Declare all required mocks at class level
    def transactionRepositoryMock = Mock(TransactionRepository)
    // Note: accountService is inherited from BaseServiceSpec (real StandardizedAccountService with accountRepositoryMock)
    // Use a real StandardizedCategoryService wired with repository mocks
    def categoryRepositoryMock = Mock(CategoryRepository)
    def categoryTxRepositoryMock = Mock(TransactionRepository)
    def categoryService = new StandardizedCategoryService(categoryRepositoryMock, categoryTxRepositoryMock)
    // Use a real StandardizedDescriptionService wired with repository mocks
    def descriptionService = new StandardizedDescriptionService(descriptionRepositoryMock, transactionRepositoryMock)
    def standardizedReceiptImageServiceMock = Mock(StandardizedReceiptImageService)
    def imageProcessingServiceMock = Mock(ImageProcessingService)
    def calculationServiceMock = Mock(CalculationService)

    @Subject
    StandardizedTransactionService standardizedTransactionService

    def setup() {
        standardizedTransactionService = new StandardizedTransactionService(
            transactionRepositoryMock,
            accountService,
            categoryService,
            descriptionService,
            standardizedReceiptImageServiceMock,
            imageProcessingServiceMock,
            calculationServiceMock
        )
        standardizedTransactionService.validator = validatorMock
        standardizedTransactionService.meterService = meterService
        // also wire shared test collaborators into the category and description services
        categoryService.validator = validatorMock
        categoryService.meterService = meterService
        descriptionService.validator = validatorMock
        descriptionService.meterService = meterService
    }

    // ===== Test Data Builders =====

    Transaction createTestTransaction() {
        return new Transaction(
            transactionId: 1L,
            guid: "test-guid-123",
            accountNameOwner: "test_account",
            accountId: 1L,
            accountType: AccountType.Credit,
            transactionDate: Date.valueOf("2023-01-01"),
            description: "Test transaction",
            category: "test_category",
            amount: new BigDecimal("100.00"),
            transactionState: TransactionState.Outstanding,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: "Test notes",
            dateAdded: new Timestamp(System.currentTimeMillis()),
            dateUpdated: new Timestamp(System.currentTimeMillis())
        )
    }

    Transaction createTestTransactionWithoutId() {
        return new Transaction(
            transactionId: 0L,
            guid: "test-guid-new",
            accountNameOwner: "test_account",
            accountId: 1L,
            accountType: AccountType.Credit,
            transactionDate: Date.valueOf("2023-01-01"),
            description: "Test transaction",
            category: "test_category",
            amount: new BigDecimal("100.00"),
            transactionState: TransactionState.Outstanding,
            activeStatus: true,
            reoccurringType: ReoccurringType.Undefined,
            notes: "Test notes"
        )
    }

    Account createTestAccount() {
        return new Account(
            accountId: 1L,
            accountNameOwner: "test_account",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "0000"
        )
    }

    Category createTestCategory() {
        return new Category(
            categoryId: 1L,
            categoryName: "test_category",
            activeStatus: true
        )
    }

    Description createTestDescription() {
        return new Description(
            descriptionId: 1L,
            descriptionName: "Test transaction",
            activeStatus: true
        )
    }

    ReceiptImage createTestReceiptImage() {
        return new ReceiptImage(
            receiptImageId: 1L,
            transactionId: 1L,
            image: "test-image".bytes,
            thumbnail: "test-thumbnail".bytes,
            imageFormatType: ImageFormatType.JPEG
        )
    }

    Totals createTestTotals() {
        return new Totals(
            new BigDecimal("50.00"),  // totalsFuture
            new BigDecimal("100.00"), // totalsCleared
            new BigDecimal("350.00"), // totals
            new BigDecimal("200.00")  // totalsOutstanding
        )
    }

    // ===== findAllActive Tests =====

    def "findAllActive should return Success with list of active transactions"() {
        given: "a list of transactions"
        def transactions = [createTestTransaction()]

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findAll is called"
        1 * transactionRepositoryMock.findAll() >> transactions

        and: "result is Success with active transactions"
        result instanceof ServiceResult.Success
        result.data == transactions
    }

    def "findAllActive should return Success with empty list when no active transactions exist"() {
        given: "no transactions"
        def emptyList = []

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findAll is called"
        1 * transactionRepositoryMock.findAll() >> emptyList

        and: "result is Success with empty list"
        result instanceof ServiceResult.Success
        result.data == emptyList
    }

    def "findAllActive should filter inactive transactions"() {
        given: "a mix of active and inactive transactions"
        def activeTransaction = createTestTransaction()
        activeTransaction.activeStatus = true
        def inactiveTransaction = createTestTransaction()
        inactiveTransaction.activeStatus = false
        def transactions = [activeTransaction, inactiveTransaction]

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findAll is called"
        1 * transactionRepositoryMock.findAll() >> transactions

        and: "result contains only active transactions"
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0] == activeTransaction
    }

    // ===== findById Tests =====

    def "findById should return Success when transaction exists"() {
        given: "a transaction GUID and existing transaction"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()

        when: "findById is called"
        def result = standardizedTransactionService.findById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)

        and: "result is Success with transaction"
        result instanceof ServiceResult.Success
        result.data == transaction
    }

    def "findById should return NotFound when transaction does not exist"() {
        given: "a transaction GUID that doesn't exist"
        def guid = "non-existent-guid"

        when: "findById is called"
        def result = standardizedTransactionService.findById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    // ===== save Tests =====

    def "save should return Success when transaction is valid and new"() {
        given: "a valid new transaction"
        def transaction = createTestTransactionWithoutId()
        def account = createTestAccount()
        def category = createTestCategory()
        def description = createTestDescription()
        def savedTransaction = createTestTransaction()

        when: "save is called"
        def result = standardizedTransactionService.save(transaction)

        then: "validation is successful"
        1 * validatorMock.validate(transaction) >> Collections.emptySet()

        and: "transaction doesn't exist"
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.empty()

        and: "account processing occurs"
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)

        and: "category processing occurs via StandardizedCategoryService"
        1 * categoryRepositoryMock.findByCategoryName(transaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByCategoryName(category.categoryName) >> 0L

        and: "description processing occurs via StandardizedDescriptionService"
        1 * descriptionRepositoryMock.findByDescriptionName(transaction.description) >> Optional.of(description)

        and: "repository saveAndFlush is called"
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> savedTransaction

        and: "result is Success with saved transaction"
        result instanceof ServiceResult.Success
        result.data == savedTransaction
    }

    def "save should set timestamps when creating new transaction"() {
        given: "a transaction for creation"
        def transaction = createTestTransactionWithoutId()
        def account = createTestAccount()
        def category = createTestCategory()
        def description = createTestDescription()
        def savedTransaction = createTestTransaction()

        when: "save is called"
        def result = standardizedTransactionService.save(transaction)

        then: "validation is successful"
        1 * validatorMock.validate(transaction) >> Collections.emptySet()

        and: "transaction doesn't exist"
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.empty()

        and: "account processing occurs"
        1 * accountRepositoryMock.findByAccountNameOwner(transaction.accountNameOwner) >> Optional.of(account)

        and: "category processing occurs via StandardizedCategoryService"
        1 * categoryRepositoryMock.findByCategoryName(transaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByCategoryName(category.categoryName) >> 0L

        and: "description processing occurs via StandardizedDescriptionService"
        1 * descriptionRepositoryMock.findByDescriptionName(transaction.description) >> Optional.of(description)

        and: "repository saveAndFlush is called"
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> savedTransaction

        and: "timestamps are set (within last 5 seconds)"
        def now = new Timestamp(System.currentTimeMillis())
        def dateAddedDiff = Math.abs(now.time - transaction.dateAdded.time)
        def dateUpdatedDiff = Math.abs(now.time - transaction.dateUpdated.time)
        dateAddedDiff < 5000
        dateUpdatedDiff < 5000

        and: "result is Success"
        result instanceof ServiceResult.Success
    }

    def "save should return ValidationError when transaction is invalid"() {
        given: "an invalid transaction"
        def transaction = createTestTransactionWithoutId()
        def violation = Mock(ConstraintViolation)
        violation.propertyPath >> Mock(jakarta.validation.Path) {
            toString() >> "accountNameOwner"
        }
        violation.message >> "Account name owner is required"

        when: "save is called"
        def result = standardizedTransactionService.save(transaction)

        then: "validation fails"
        1 * validatorMock.validate(transaction) >> Set.of(violation)

        and: "result is ValidationError"
        result instanceof ServiceResult.ValidationError
        result.errors.containsKey("accountNameOwner")
    }

    def "save should return BusinessError when transaction already exists"() {
        given: "a transaction that already exists"
        def transaction = createTestTransactionWithoutId()
        def existingTransaction = createTestTransaction()

        when: "save is called"
        def result = standardizedTransactionService.save(transaction)

        then: "validation is successful"
        1 * validatorMock.validate(transaction) >> Collections.emptySet()

        and: "transaction already exists"
        1 * transactionRepositoryMock.findByGuid(transaction.guid) >> Optional.of(existingTransaction)

        and: "result is BusinessError"
        result instanceof ServiceResult.BusinessError
        result.message.contains("Transaction already exists")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
    }

    // ===== update Tests =====

    def "update should return Success when transaction exists and is valid"() {
        given: "an existing transaction and updated data"
        def existingTransaction = createTestTransaction()
        def updatedTransaction = createTestTransaction()
        updatedTransaction.description = "Updated description"
        def account = createTestAccount()
        def category = createTestCategory()
        def description = createTestDescription()

        when: "update is called"
        def result = standardizedTransactionService.update(updatedTransaction)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(updatedTransaction.guid) >> Optional.of(existingTransaction)

        and: "validation is successful"
        1 * validatorMock.validate(updatedTransaction) >> Collections.emptySet()

        and: "masterTransactionUpdater business logic executes"
        1 * categoryRepositoryMock.findByCategoryName(updatedTransaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByCategoryName(category.categoryName) >> 0L
        1 * accountRepositoryMock.findByAccountNameOwner(updatedTransaction.accountNameOwner) >> Optional.of(account)
        1 * descriptionRepositoryMock.findByDescriptionName(updatedTransaction.description) >> Optional.of(description)
        1 * transactionRepositoryMock.saveAndFlush(updatedTransaction) >> updatedTransaction

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data == updatedTransaction
    }

    def "update should return NotFound when transaction does not exist"() {
        given: "a transaction that doesn't exist"
        def transaction = createTestTransaction()
        transaction.guid = "non-existent-guid"

        when: "update is called"
        def result = standardizedTransactionService.update(transaction)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid("non-existent-guid") >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    // ===== deleteById Tests =====

    def "deleteById should return Success when transaction exists"() {
        given: "an existing transaction"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)

        and: "repository delete is called"
        1 * transactionRepositoryMock.delete(transaction)

        and: "result is Success with true"
        result instanceof ServiceResult.Success
        result.data == true
    }

    def "deleteById should return NotFound when transaction does not exist"() {
        given: "a transaction GUID that doesn't exist"
        def guid = "non-existent-guid"

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    // ===== Business-Specific ServiceResult Methods Tests =====

    def "findByAccountNameOwnerOrderByTransactionDateStandardized should return Success with sorted transactions"() {
        given: "transactions for an account"
        def accountNameOwner = "test_account"
        def transactions = [createTestTransaction()]

        when: "findByAccountNameOwnerOrderByTransactionDateStandardized is called"
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)

        then: "repository method is called via resilience pattern"
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> transactions

        and: "result is Success with sorted transactions"
        result instanceof ServiceResult.Success
        result.data == transactions
    }

    def "findByAccountNameOwnerOrderByTransactionDateStandardized should return empty list when no transactions"() {
        given: "no transactions for account"
        def accountNameOwner = "test_account"

        when: "findByAccountNameOwnerOrderByTransactionDateStandardized is called"
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> []

        and: "result is Success with empty list"
        result instanceof ServiceResult.Success
        result.data == []
    }

    def "findTransactionsByCategoryStandardized should return Success with transactions"() {
        given: "transactions with category"
        def categoryName = "test_category"
        def transactions = [createTestTransaction()]

        when: "findTransactionsByCategoryStandardized is called"
        def result = standardizedTransactionService.findTransactionsByCategoryStandardized(categoryName)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName, true) >> transactions

        and: "result is Success with transactions"
        result instanceof ServiceResult.Success
        result.data == transactions
    }

    def "findTransactionsByDescriptionStandardized should return Success with transactions"() {
        given: "transactions with description"
        def descriptionName = "Test transaction"
        def transactions = [createTestTransaction()]

        when: "findTransactionsByDescriptionStandardized is called"
        def result = standardizedTransactionService.findTransactionsByDescriptionStandardized(descriptionName)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName, true) >> transactions

        and: "result is Success with transactions"
        result instanceof ServiceResult.Success
        result.data == transactions
    }

    def "updateTransactionStateStandardized should return Success when state change is valid"() {
        given: "an existing transaction and new state"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()
        transaction.transactionState = TransactionState.Outstanding
        def newState = TransactionState.Cleared

        when: "updateTransactionStateStandardized is called"
        def result = standardizedTransactionService.updateTransactionStateStandardized(guid, newState)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)

        and: "repository saveAndFlush is called"
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction

        and: "transaction state is updated"
        transaction.transactionState == newState

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data == transaction
    }

    def "updateTransactionStateStandardized should throw exception when state is same"() {
        given: "an existing transaction with same state"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()
        transaction.transactionState = TransactionState.Outstanding

        when: "updateTransactionStateStandardized is called with same state"
        def result = standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Outstanding)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)

        and: "result is SystemError due to exception"
        result instanceof ServiceResult.SystemError
        result.exception instanceof InvalidTransactionStateException
    }

    def "changeAccountNameOwnerStandardized should return Success when valid"() {
        given: "valid account and transaction"
        def accountNameOwner = "new_account"
        def guid = "test-guid-123"
        def account = createTestAccount()
        account.accountNameOwner = accountNameOwner
        def transaction = createTestTransaction()

        when: "changeAccountNameOwnerStandardized is called"
        def result = standardizedTransactionService.changeAccountNameOwnerStandardized(accountNameOwner, guid)

        then: "services are called"
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction

        and: "transaction is updated"
        transaction.accountNameOwner == accountNameOwner
        transaction.accountId == account.accountId

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data == transaction
    }

    def "createFutureTransactionStandardized should return Success with future transaction"() {
        given: "a valid transaction for future creation"
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly

        when: "createFutureTransactionStandardized is called"
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then: "result is Success with future transaction"
        result instanceof ServiceResult.Success
        result.data.transactionState == TransactionState.Future
        result.data.guid != transaction.guid
        result.data.receiptImageId == null
    }

    def "createFutureTransactionStandardized should throw exception for undefined reoccurring type"() {
        given: "a transaction with undefined reoccurring type"
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Undefined

        when: "createFutureTransactionStandardized is called"
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then: "result is SystemError due to exception"
        result instanceof ServiceResult.SystemError
        result.exception instanceof InvalidReoccurringTypeException
    }

    // ===== Legacy Method Compatibility Tests (REMOVED) =====





    def "calculateActiveTotalsByAccountNameOwner should delegate to calculationService"() {
        given: "an account name owner"
        def accountNameOwner = "test_account"
        def totals = createTestTotals()

        when: "calculateActiveTotalsByAccountNameOwner is called"
        def result = standardizedTransactionService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

        then: "calculationService is called"
        1 * calculationServiceMock.calculateActiveTotalsByAccountNameOwner(accountNameOwner) >> totals

        and: "result is the totals"
        result == totals
    }

    def "findByAccountNameOwnerOrderByTransactionDate should delegate to standardized method and return list"() {
        given: "an account name owner"
        def accountNameOwner = "test_account"
        def transactions = [createTestTransaction()]

        when: "findByAccountNameOwnerOrderByTransactionDate is called"
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDate(accountNameOwner)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner, true) >> transactions

        and: "result is the list of transactions"
        result == transactions
    }


    def "changeAccountNameOwner should delegate to standardized method and return transaction"() {
        given: "valid parameters"
        def accountNameOwner = "new_account"
        def guid = "test-guid-123"
        def map = ["accountNameOwner": accountNameOwner, "guid": guid]
        def account = createTestAccount()
        account.accountNameOwner = accountNameOwner
        def transaction = createTestTransaction()

        when: "changeAccountNameOwner is called"
        def result = standardizedTransactionService.changeAccountNameOwner(map)

        then: "services are called"
        1 * accountRepositoryMock.findByAccountNameOwner(accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction

        and: "result is the updated transaction"
        result == transaction
    }

    def "changeAccountNameOwner should throw AccountValidationException when parameters are null"() {
        given: "invalid parameters"
        def map = ["accountNameOwner": null, "guid": "test-guid"]

        when: "changeAccountNameOwner is called"
        standardizedTransactionService.changeAccountNameOwner(map)

        then: "AccountValidationException is thrown"
        thrown(AccountValidationException)
    }

    def "updateTransactionState should delegate to standardized method and return transaction"() {
        given: "valid parameters"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()
        transaction.transactionState = TransactionState.Outstanding
        def newState = TransactionState.Cleared

        when: "updateTransactionState is called"
        def result = standardizedTransactionService.updateTransactionState(guid, newState)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByGuid(guid) >> Optional.of(transaction)

        and: "repository saveAndFlush is called"
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction

        and: "result is the updated transaction"
        result == transaction
    }

    def "createThumbnail should delegate to imageProcessingService"() {
        given: "image data"
        def rawImage = "test-image".bytes
        def imageFormatType = ImageFormatType.Jpeg
        def thumbnail = "test-thumbnail".bytes

        when: "createThumbnail is called"
        def result = standardizedTransactionService.createThumbnail(rawImage, imageFormatType)

        then: "imageProcessingService is called"
        1 * imageProcessingServiceMock.createThumbnail(rawImage, imageFormatType) >> thumbnail

        and: "result is the thumbnail"
        result == thumbnail
    }

    def "getImageFormatType should delegate to imageProcessingService"() {
        given: "image data"
        def rawImage = "test-image".bytes
        def imageFormatType = ImageFormatType.Jpeg

        when: "getImageFormatType is called"
        def result = standardizedTransactionService.getImageFormatType(rawImage)

        then: "imageProcessingService is called"
        1 * imageProcessingServiceMock.getImageFormatType(rawImage) >> imageFormatType

        and: "result is the image format type"
        result == imageFormatType
    }

    def "createFutureTransaction should delegate to standardized method and return transaction"() {
        given: "a valid transaction"
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly

        when: "createFutureTransaction is called"
        def result = standardizedTransactionService.createFutureTransaction(transaction)

        then: "result is a future transaction"
        result.transactionState == TransactionState.Future
        result.guid != transaction.guid
    }

    def "findTransactionsByCategory should delegate to standardized method and return list"() {
        given: "a category name"
        def categoryName = "test_category"
        def transactions = [createTestTransaction()]

        when: "findTransactionsByCategory is called"
        def result = standardizedTransactionService.findTransactionsByCategory(categoryName)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName, true) >> transactions

        and: "result is the list of transactions"
        result == transactions
    }

    def "findTransactionsByDescription should delegate to standardized method and return list"() {
        given: "a description name"
        def descriptionName = "Test transaction"
        def transactions = [createTestTransaction()]

        when: "findTransactionsByDescription is called"
        def result = standardizedTransactionService.findTransactionsByDescription(descriptionName)

        then: "repository method is called"
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName, true) >> transactions

        and: "result is the list of transactions"
        result == transactions
    }
}
