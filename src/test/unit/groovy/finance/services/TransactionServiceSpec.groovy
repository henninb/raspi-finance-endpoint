package finance.services
import finance.configurations.ResilienceComponents

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
import java.sql.Timestamp
import java.time.LocalDate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class TransactionServiceSpec extends BaseServiceSpec {

    // Declare all required mocks at class level
    def transactionRepositoryMock = Mock(TransactionRepository)
    // Note: accountService is inherited from BaseServiceSpec (real AccountService with accountRepositoryMock)
    // Use a real CategoryService wired with repository mocks
    def categoryRepositoryMock = Mock(CategoryRepository)
    def categoryTxRepositoryMock = Mock(TransactionRepository)
    def standardizedReceiptImageServiceMock = Mock(ReceiptImageService)
    def imageProcessingServiceMock = Mock(ImageProcessingService)
    def calculationServiceMock = Mock(CalculationService)

    CategoryService localCategoryService
    DescriptionService localDescriptionService

    @Subject
    TransactionService standardizedTransactionService

    def setup() {
        localCategoryService = new CategoryService(categoryRepositoryMock, categoryTxRepositoryMock, meterService, validatorMock, ResilienceComponents.noOp())
        localDescriptionService = new DescriptionService(descriptionRepositoryMock, transactionRepositoryMock, meterService, validatorMock, ResilienceComponents.noOp())
        standardizedTransactionService = new TransactionService(
            transactionRepositoryMock,
            accountService,
            localCategoryService,
            localDescriptionService,
            standardizedReceiptImageServiceMock,
            imageProcessingServiceMock,
            calculationServiceMock,
            paymentRepositoryMock,
            meterService,
            validatorMock,
            ResilienceComponents.noOp()
        )
    }

    // ===== Test Data Builders =====

    Transaction createTestTransaction() {
        return new Transaction(
            transactionId: 1L,
            owner: "test_owner",
            guid: "test-guid-123",
            accountNameOwner: "test_account",
            accountId: 1L,
            accountType: AccountType.Credit,
            transactionDate: LocalDate.of(2023, 1, 1),
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
            owner: "test_owner",
            guid: "test-guid-new",
            accountNameOwner: "test_account",
            accountId: 1L,
            accountType: AccountType.Credit,
            transactionDate: LocalDate.of(2023, 1, 1),
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
            owner: "test_owner",
            accountNameOwner: "test_account",
            accountType: AccountType.Credit,
            activeStatus: true,
            moniker: "0000"
        )
    }

    Category createTestCategory() {
        return new Category(
            categoryId: 1L,
            owner: "test_owner",
            categoryName: "test_category",
            activeStatus: true
        )
    }

    Description createTestDescription() {
        return new Description(
            descriptionId: 1L,
            owner: "test_owner",
            descriptionName: "Test transaction",
            activeStatus: true
        )
    }

    ReceiptImage createTestReceiptImage() {
        return new ReceiptImage(
            receiptImageId: 1L,
            owner: "test_owner",
            transactionId: 1L,
            image: "test-image".bytes,
            thumbnail: "test-thumbnail".bytes,
            imageFormatType: ImageFormatType.Jpeg
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
        def page = new PageImpl<>(transactions)

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findByOwnerAndActiveStatus is called"
        1 * transactionRepositoryMock.findByOwnerAndActiveStatus(TEST_OWNER, true, _) >> page

        and: "result is Success with active transactions"
        result instanceof ServiceResult.Success
        result.data == transactions
    }

    def "findAllActive should return Success with empty list when no active transactions exist"() {
        given: "no transactions"
        def page = new PageImpl<>([])

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findByOwnerAndActiveStatus is called"
        1 * transactionRepositoryMock.findByOwnerAndActiveStatus(TEST_OWNER, true, _) >> page

        and: "result is Success with empty list"
        result instanceof ServiceResult.Success
        result.data == []
    }

    def "findAllActive should filter inactive transactions"() {
        given: "a mix of active and inactive transactions"
        def activeTransaction = createTestTransaction()
        activeTransaction.activeStatus = true
        def inactiveTransaction = createTestTransaction()
        inactiveTransaction.activeStatus = false
        def transactions = [activeTransaction, inactiveTransaction]
        def page = new PageImpl<>(transactions)

        when: "findAllActive is called"
        def result = standardizedTransactionService.findAllActive()

        then: "repository findByOwnerAndActiveStatus is called"
        1 * transactionRepositoryMock.findByOwnerAndActiveStatus(TEST_OWNER, true, _) >> page

        and: "result contains only active transactions"
        result instanceof ServiceResult.Success
        result.data.size() == 2
    }

    // ===== findById Tests =====

    def "findById should return Success when transaction exists"() {
        given: "a transaction GUID and existing transaction"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()

        when: "findById is called"
        def result = standardizedTransactionService.findById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, transaction.guid) >> Optional.empty()

        and: "account processing occurs"
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transaction.accountNameOwner) >> Optional.of(account)

        and: "category processing occurs via StandardizedCategoryService"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, transaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByOwnerAndCategoryName(TEST_OWNER, category.categoryName) >> 0L

        and: "description processing occurs via StandardizedDescriptionService"
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(TEST_OWNER, transaction.description) >> Optional.of(description)
        1 * transactionRepositoryMock.countByOwnerAndDescriptionName(TEST_OWNER, description.descriptionName) >> 0L

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, transaction.guid) >> Optional.empty()

        and: "account processing occurs"
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transaction.accountNameOwner) >> Optional.of(account)

        and: "category processing occurs via StandardizedCategoryService"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, transaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByOwnerAndCategoryName(TEST_OWNER, category.categoryName) >> 0L

        and: "description processing occurs via StandardizedDescriptionService"
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(TEST_OWNER, transaction.description) >> Optional.of(description)
        1 * transactionRepositoryMock.countByOwnerAndDescriptionName(TEST_OWNER, description.descriptionName) >> 0L

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, transaction.guid) >> Optional.of(existingTransaction)

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, updatedTransaction.guid) >> Optional.of(existingTransaction)

        and: "validation is successful"
        1 * validatorMock.validate(updatedTransaction) >> Collections.emptySet()

        and: "masterTransactionUpdater business logic executes"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, updatedTransaction.category) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByOwnerAndCategoryName(TEST_OWNER, category.categoryName) >> 0L
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, updatedTransaction.accountNameOwner) >> Optional.of(account)
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(TEST_OWNER, updatedTransaction.description) >> Optional.of(description)
        1 * transactionRepositoryMock.countByOwnerAndDescriptionName(TEST_OWNER, description.descriptionName) >> 0L
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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, "non-existent-guid") >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    // ===== deleteById Tests =====

    def "deleteById should return Success when transaction exists"() {
        given: "an existing transaction"
        def guid = "test-guid-123"
        def transaction = createTestTransaction()
        transaction.guid = guid

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

        and: "payment repository checks for references"
        1 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(TEST_OWNER, guid, TEST_OWNER, guid) >> []

        and: "repository delete is called"
        1 * transactionRepositoryMock.delete(transaction)

        and: "result is Success with true"
        result instanceof ServiceResult.Success
        result.data != null
    }

    def "deleteById should return NotFound when transaction does not exist"() {
        given: "a transaction GUID that doesn't exist"
        def guid = "non-existent-guid"

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    def "deleteById should return BusinessError when transaction is referenced by payments"() {
        given: "a transaction that is referenced by payments"
        def guid = "referenced-guid-123"
        def transaction = createTestTransaction()
        transaction.guid = guid

        and: "payments that reference this transaction"
        def payment1 = GroovyMock(finance.domain.Payment)
        def payment2 = GroovyMock(finance.domain.Payment)
        payment1.paymentId >> 1L
        payment2.paymentId >> 2L

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

        and: "payment repository checks for references"
        1 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(TEST_OWNER, guid, TEST_OWNER, guid) >> [payment1, payment2]

        and: "repository delete is NOT called"
        0 * transactionRepositoryMock.delete(_)

        and: "result is BusinessError with informative message"
        result instanceof ServiceResult.BusinessError
        result.message.contains("Cannot delete transaction")
        result.message.contains(guid)
        result.message.contains("2 payment")
    }

    def "deleteById should succeed when transaction has no payment references"() {
        given: "a transaction with no payment references"
        def guid = "unreferenced-guid-123"
        def transaction = createTestTransaction()
        transaction.guid = guid

        when: "deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

        and: "payment repository checks for references"
        1 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(TEST_OWNER, guid, TEST_OWNER, guid) >> []

        and: "repository delete is called"
        1 * transactionRepositoryMock.delete(transaction)

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data != null
    }

    // ===== TDD Tests for deleteByIdInternal() (Cascade Delete Support) =====

    def "deleteByIdInternal should delete transaction without checking payment references"() {
        given: "a transaction that IS referenced by payments"
        def guid = "transaction-with-payments"
        def transaction = createTestTransaction()
        transaction.guid = guid

        when: "deleteByIdInternal is called"
        def result = standardizedTransactionService.deleteByIdInternal(guid)

        then: "repository finds the transaction"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

        and: "NO payment reference check is performed"
        0 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(_, _, _, _)

        and: "repository delete IS called"
        1 * transactionRepositoryMock.delete(transaction)

        and: "result is Success"
        result instanceof ServiceResult.Success
        result.data != null
    }

    def "deleteByIdInternal should return NotFound when transaction does not exist"() {
        given: "a transaction GUID that doesn't exist"
        def guid = "non-existent-guid"

        when: "deleteByIdInternal is called"
        def result = standardizedTransactionService.deleteByIdInternal(guid)

        then: "repository findByGuid is called"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()

        and: "NO payment reference check is performed"
        0 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(_, _, _, _)

        and: "result is NotFound"
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    def "deleteById should still block deletion when payment references exist"() {
        given: "a transaction referenced by a payment"
        def guid = "transaction-with-payment"
        def transaction = createTestTransaction()
        transaction.guid = guid
        def payment = GroovyMock(finance.domain.Payment)

        when: "public deleteById is called"
        def result = standardizedTransactionService.deleteById(guid)

        then: "payment reference check IS performed"
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
        1 * paymentRepositoryMock.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(TEST_OWNER, guid, TEST_OWNER, guid) >> [payment]

        and: "repository delete is NOT called"
        0 * transactionRepositoryMock.delete(_)

        and: "result is BusinessError"
        result instanceof ServiceResult.BusinessError
    }

    // ===== Business-Specific ServiceResult Methods Tests =====

    def "findByAccountNameOwnerOrderByTransactionDateStandardized should return Success with sorted transactions"() {
        given: "transactions for an account"
        def accountNameOwner = "test_account"
        def transactions = [createTestTransaction()]

        when: "findByAccountNameOwnerOrderByTransactionDateStandardized is called"
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)

        then: "repository method is called via resilience pattern"
        1 * transactionRepositoryMock.findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, accountNameOwner, true) >> transactions

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
        1 * transactionRepositoryMock.findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, accountNameOwner, true) >> []

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
        1 * transactionRepositoryMock.findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, categoryName, true) >> transactions

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
        1 * transactionRepositoryMock.findByOwnerAndDescriptionAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, descriptionName, true) >> transactions

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

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
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)

        and: "result is BusinessError for domain rule violation"
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("cannot update transactionstate")
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
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
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

        then: "result is BusinessError for domain rule violation"
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("reoccurring")
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

    // ===== findTransactionsByDateRange Tests =====

    def "findTransactionsByDateRangeStandardized should return Success with page of transactions"() {
        given: "a valid date range and pageable"
        LocalDate startDate = LocalDate.of(2023, 1, 1)
        LocalDate endDate = LocalDate.of(2023, 12, 31)
        Pageable pageable = PageRequest.of(0, 10)
        def transactions = [createTestTransaction()]
        Page<Transaction> page = new PageImpl<>(transactions, pageable, transactions.size())

        when: "findTransactionsByDateRangeStandardized is called"
        def result = standardizedTransactionService.findTransactionsByDateRangeStandardized(startDate, endDate, pageable)

        then: "repository method is called with date range and pageable"
        1 * transactionRepositoryMock.findByOwnerAndTransactionDateBetween(TEST_OWNER, startDate, endDate, pageable) >> page

        and: "result is Success with page"
        result instanceof ServiceResult.Success
        result.data == page
    }

    def "findTransactionsByDateRangeStandardized should return BusinessError when startDate after endDate"() {
        given: "an invalid date range"
        LocalDate startDate = LocalDate.of(2023, 12, 31)
        LocalDate endDate = LocalDate.of(2023, 1, 1)
        Pageable pageable = PageRequest.of(0, 10)

        when: "findTransactionsByDateRangeStandardized is called"
        def result = standardizedTransactionService.findTransactionsByDateRangeStandardized(startDate, endDate, pageable)

        then: "no repository interaction and BusinessError returned"
        0 * transactionRepositoryMock._
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("startdate must be before or equal to enddate")
    }

    def "findAllActive paginated should return paged transactions"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def transactions = [createTestTransaction()]
        def page = new PageImpl(transactions, pageable, 1)

        when:
        def result = standardizedTransactionService.findAllActive(pageable)

        then:
        1 * transactionRepositoryMock.findByOwnerAndActiveStatus(TEST_OWNER, true, _) >> page
        result instanceof ServiceResult.Success
        result.data.content == transactions
    }

    def "findByAccountNameOwnerOrderByTransactionDateStandardized paginated should return paged transactions"() {
        given:
        def accountNameOwner = "test_account"
        def pageable = PageRequest.of(0, 10)
        def transactions = [createTestTransaction()]
        def page = new PageImpl(transactions, pageable, 1)

        when:
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner, pageable)

        then:
        1 * transactionRepositoryMock.findByOwnerAndAccountNameOwnerAndActiveStatus(TEST_OWNER, accountNameOwner, true, _) >> page
        result instanceof ServiceResult.Success
        result.data.content == transactions
    }

    def "findByAccountNameOwnerOrderByTransactionDateStandardized paginated should log warning for empty page"() {
        given:
        def accountNameOwner = "unknown_account"
        def pageable = PageRequest.of(0, 10)
        def emptyPage = new PageImpl([], pageable, 0)

        when:
        def result = standardizedTransactionService.findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner, pageable)

        then:
        1 * transactionRepositoryMock.findByOwnerAndAccountNameOwnerAndActiveStatus(TEST_OWNER, accountNameOwner, true, _) >> emptyPage
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "findTransactionsByCategoryStandardized paginated should return paged transactions"() {
        given:
        def category = "food"
        def pageable = PageRequest.of(0, 5)
        def transactions = [createTestTransaction()]
        def page = new PageImpl(transactions, pageable, 1)

        when:
        def result = standardizedTransactionService.findTransactionsByCategoryStandardized(category, pageable)

        then:
        1 * transactionRepositoryMock.findByOwnerAndCategoryAndActiveStatus(TEST_OWNER, category, true, _) >> page
        result instanceof ServiceResult.Success
        result.data.content == transactions
    }

    def "findTransactionsByDescriptionStandardized paginated should return paged transactions"() {
        given:
        def description = "grocery run"
        def pageable = PageRequest.of(0, 5)
        def transactions = [createTestTransaction()]
        def page = new PageImpl(transactions, pageable, 1)

        when:
        def result = standardizedTransactionService.findTransactionsByDescriptionStandardized(description, pageable)

        then:
        1 * transactionRepositoryMock.findByOwnerAndDescriptionAndActiveStatus(TEST_OWNER, description, true, _) >> page
        result instanceof ServiceResult.Success
        result.data.content == transactions
    }

    def "updateTransactionStateStandardized should return NotFound when transaction does not exist"() {
        given:
        def guid = "non-existent-guid"

        when:
        def result = standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Cleared)

        then:
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Transaction not found: non-existent-guid")
    }

    def "updateTransactionStateStandardized should return BusinessError when clearing a future-dated transaction"() {
        given:
        def guid = "future-guid"
        def transaction = createTestTransaction()
        transaction.guid = guid
        transaction.transactionState = TransactionState.Outstanding
        transaction.transactionDate = LocalDate.now().plusDays(5)

        when:
        def result = standardizedTransactionService.updateTransactionStateStandardized(guid, TransactionState.Cleared)

        then:
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("future")
    }

    def "changeAccountNameOwnerStandardized should return BusinessError when account does not exist"() {
        given:
        def guid = "test-guid-123"
        def accountNameOwner = "nonexistent_account"
        def transaction = createTestTransaction()

        when:
        def result = standardizedTransactionService.changeAccountNameOwnerStandardized(accountNameOwner, guid)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, accountNameOwner) >> Optional.empty()
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
        result instanceof ServiceResult.BusinessError
        result.message.contains("guid='$guid'")
    }

    def "changeAccountNameOwnerStandardized should return BusinessError when transaction does not exist"() {
        given:
        def guid = "missing-guid"
        def accountNameOwner = "test_account"
        def account = createTestAccount()

        when:
        def result = standardizedTransactionService.changeAccountNameOwnerStandardized(accountNameOwner, guid)

        then:
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, accountNameOwner) >> Optional.of(account)
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()
        result instanceof ServiceResult.BusinessError
    }

    def "createFutureTransactionStandardized should calculate date for FortNightly reoccurring type"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.FortNightly
        def expectedDate = transaction.transactionDate.plusDays(14)

        when:
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then:
        result instanceof ServiceResult.Success
        result.data.transactionDate == expectedDate
        result.data.transactionState == TransactionState.Future
    }

    def "createFutureTransactionStandardized should calculate monthly date for Debit account"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.accountType = AccountType.Debit
        def expectedDate = transaction.transactionDate.plusMonths(1)

        when:
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then:
        result instanceof ServiceResult.Success
        result.data.transactionDate == expectedDate
    }

    def "createFutureTransactionStandardized should calculate yearly date for Credit account"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.accountType = AccountType.Credit
        def expectedDate = transaction.transactionDate.plusYears(1)

        when:
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then:
        result instanceof ServiceResult.Success
        result.data.transactionDate == expectedDate
    }

    def "createFutureTransactionStandardized should return BusinessError for Debit with unsupported reoccurring type"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Annually
        transaction.accountType = AccountType.Debit

        when:
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then:
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("reoccurring")
    }

    def "createFutureTransactionStandardized should propagate future due date calculation"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly
        transaction.accountType = AccountType.Credit
        transaction.dueDate = LocalDate.of(2023, 1, 15)

        when:
        def result = standardizedTransactionService.createFutureTransactionStandardized(transaction)

        then:
        result instanceof ServiceResult.Success
        result.data.dueDate == transaction.dueDate.plusYears(1)
    }

    def "createAndSaveFutureTransaction should return Success when future transaction created and saved"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Monthly
        def account = createTestAccount()
        def category = createTestCategory()
        def description = createTestDescription()
        def savedTransaction = createTestTransaction()
        savedTransaction.transactionState = TransactionState.Future

        when:
        def result = standardizedTransactionService.createAndSaveFutureTransaction(transaction)

        then:
        1 * validatorMock.validate(_) >> Collections.emptySet()
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, _) >> Optional.empty()
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transaction.accountNameOwner) >> Optional.of(account)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, _) >> Optional.of(category)
        1 * categoryTxRepositoryMock.countByOwnerAndCategoryName(TEST_OWNER, _) >> 0L
        1 * descriptionRepositoryMock.findByOwnerAndDescriptionName(TEST_OWNER, _) >> Optional.of(description)
        1 * transactionRepositoryMock.countByOwnerAndDescriptionName(TEST_OWNER, _) >> 0L
        1 * transactionRepositoryMock.saveAndFlush(_) >> savedTransaction
        result instanceof ServiceResult.Success
    }

    def "createAndSaveFutureTransaction should return error when createFutureTransaction fails"() {
        given:
        def transaction = createTestTransaction()
        transaction.reoccurringType = ReoccurringType.Undefined

        when:
        def result = standardizedTransactionService.createAndSaveFutureTransaction(transaction)

        then:
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("reoccurring")
        0 * transactionRepositoryMock.saveAndFlush(_)
    }

    def "deleteReceiptImageForTransactionByGuidStandardized should return NotFound when transaction missing"() {
        given:
        def guid = "missing-guid"

        when:
        def result = standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized(guid)

        then:
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.empty()
        result instanceof ServiceResult.BusinessError
    }

    def "deleteReceiptImageForTransactionByGuidStandardized should return BusinessError when transaction has no image"() {
        given:
        def guid = "no-image-guid"
        def transaction = createTestTransaction()
        transaction.receiptImageId = null

        when:
        def result = standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized(guid)

        then:
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
        result instanceof ServiceResult.BusinessError
    }

    def "deleteReceiptImageForTransactionByGuidStandardized should delete image when transaction has one"() {
        given:
        def guid = "has-image-guid"
        def transaction = createTestTransaction()
        transaction.receiptImageId = 99L
        def receiptImage = createTestReceiptImage()
        receiptImage.receiptImageId = 99L

        when:
        def result = standardizedTransactionService.deleteReceiptImageForTransactionByGuidStandardized(guid)

        then:
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, guid) >> Optional.of(transaction)
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> transaction
        1 * standardizedReceiptImageServiceMock.deleteById(99L) >> ServiceResult.Success.of(receiptImage)
        result instanceof ServiceResult.Success
    }

    def "save should create a new account when account does not exist"() {
        given:
        def transaction = createTestTransactionWithoutId()
        def newAccount = createTestAccount()
        newAccount.accountId = 99L
        def category = createTestCategory()
        def description = createTestDescription()
        def savedTransaction = createTestTransaction()

        when:
        def result = standardizedTransactionService.save(transaction)

        then:
        1 * validatorMock.validate(transaction) >> Collections.emptySet()
        1 * transactionRepositoryMock.findByOwnerAndGuid(TEST_OWNER, transaction.guid) >> Optional.empty()
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transaction.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.findByOwnerAndAccountNameOwner(TEST_OWNER, transaction.accountNameOwner) >> Optional.empty()
        1 * accountRepositoryMock.saveAndFlush(_) >> newAccount
        _ * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, transaction.category) >> Optional.of(category)
        _ * categoryTxRepositoryMock.countByOwnerAndCategoryName(TEST_OWNER, _) >> 0L
        _ * descriptionRepositoryMock.findByOwnerAndDescriptionName(TEST_OWNER, transaction.description) >> Optional.of(description)
        _ * transactionRepositoryMock.countByOwnerAndDescriptionName(TEST_OWNER, _) >> 0L
        1 * transactionRepositoryMock.saveAndFlush(transaction) >> savedTransaction
        result instanceof ServiceResult.Success
    }

}
