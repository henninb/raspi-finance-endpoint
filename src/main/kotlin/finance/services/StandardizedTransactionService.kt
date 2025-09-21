package finance.services

import finance.domain.*
import finance.repositories.TransactionRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

/**
 * Standardized Transaction Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 * Uses extracted services from Phase 1: ImageProcessingService and CalculationService
 */
@Service
class StandardizedTransactionService(
    private val transactionRepository: TransactionRepository,
    private val accountService: IAccountService,
    private val categoryService: ICategoryService,
    private val descriptionService: IDescriptionService,
    private val receiptImageService: ReceiptImageService,
    private val imageProcessingService: ImageProcessingService,
    private val calculationService: CalculationService
) : StandardizedBaseService<Transaction, String>(), ITransactionService {

    override fun getEntityName(): String = "Transaction"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Transaction>> {
        return handleServiceOperation("findAllActive", null) {
            transactionRepository.findAll().filter { it.activeStatus }
        }
    }

    override fun findById(id: String): ServiceResult<Transaction> {
        return handleServiceOperation("findById", id) {
            val optionalTransaction = transactionRepository.findByGuid(id)
            if (optionalTransaction.isPresent) {
                optionalTransaction.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
            }
        }
    }

    override fun save(entity: Transaction): ServiceResult<Transaction> {
        return handleServiceOperation("save", entity.guid) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Check for existing transaction
            val existingOptional = transactionRepository.findByGuid(entity.guid)
            if (existingOptional.isPresent) {
                throw org.springframework.dao.DataIntegrityViolationException("Transaction already exists: ${entity.guid}")
            }

            // Process related entities
            processAccount(entity)
            processCategory(entity)
            processDescription(entity)

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateUpdated = timestamp
            entity.dateAdded = timestamp

            transactionRepository.saveAndFlush(entity)
        }
    }

    override fun update(entity: Transaction): ServiceResult<Transaction> {
        return handleServiceOperation("update", entity.guid) {
            val existingTransaction = transactionRepository.findByGuid(entity.guid)
            if (existingTransaction.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: ${entity.guid}")
            }

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Use existing masterTransactionUpdater logic
            masterTransactionUpdater(existingTransaction.get(), entity)
        }
    }

    override fun deleteById(id: String): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalTransaction = transactionRepository.findByGuid(id)
            if (optionalTransaction.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
            }
            transactionRepository.delete(optionalTransaction.get())
            true
        }
    }

    // ===== Business-Specific ServiceResult Methods =====

    fun findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner: String): ServiceResult<List<Transaction>> {
        return handleServiceOperation("findByAccountNameOwnerOrderByTransactionDate", accountNameOwner) {
            val transactions = executeWithResilienceSync(
                operation = {
                    transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner)
                },
                operationName = "findByAccountNameOwnerOrderByTransactionDate-$accountNameOwner",
                timeoutSeconds = 60
            )

            if (transactions.isEmpty()) {
                logger.error("No active transactions found for account owner: $accountNameOwner")
                meterService.incrementAccountListIsEmpty("non-existent-accounts")
                return@handleServiceOperation emptyList<Transaction>()
            }

            transactions.sortedWith(
                compareByDescending<Transaction> { it.transactionState }
                    .thenByDescending { it.transactionDate }
            )
        }
    }

    fun findTransactionsByCategoryStandardized(categoryName: String): ServiceResult<List<Transaction>> {
        return handleServiceOperation("findTransactionsByCategory", categoryName) {
            val transactions = transactionRepository.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName)
            transactions.ifEmpty { emptyList() }
        }
    }

    fun findTransactionsByDescriptionStandardized(descriptionName: String): ServiceResult<List<Transaction>> {
        return handleServiceOperation("findTransactionsByDescription", descriptionName) {
            val transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName)
            transactions.ifEmpty { emptyList() }
        }
    }

    fun updateTransactionStateStandardized(guid: String, transactionState: TransactionState): ServiceResult<Transaction> {
        return handleServiceOperation("updateTransactionState", guid) {
            val transactionOptional = transactionRepository.findByGuid(guid)
            if (transactionOptional.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: $guid")
            }

            val transaction = transactionOptional.get()
            if (transactionState == transaction.transactionState) {
                throw InvalidTransactionStateException("Cannot update transactionState to the same for guid = '$guid'")
            }

            if (transactionState == TransactionState.Cleared &&
                transaction.transactionDate > Date(Calendar.getInstance().timeInMillis)
            ) {
                throw InvalidTransactionStateException("Cannot set cleared status on a future dated transaction: ${transaction.transactionDate}.")
            }

            meterService.incrementTransactionUpdateClearedCounter(transaction.accountNameOwner)
            transaction.transactionState = transactionState
            transaction.dateUpdated = Timestamp(System.currentTimeMillis())
            transactionRepository.saveAndFlush(transaction)
        }
    }

    fun changeAccountNameOwnerStandardized(accountNameOwner: String, guid: String): ServiceResult<Transaction> {
        return handleServiceOperation("changeAccountNameOwner", guid) {
            val accountOptional = accountService.account(accountNameOwner)
            val transactionOptional = transactionRepository.findByGuid(guid)

            if (transactionOptional.isEmpty || accountOptional.isEmpty) {
                throw AccountValidationException("Cannot change accountNameOwner for a transaction that does not exist, guid='$guid'.")
            }

            val account = accountOptional.get()
            val transaction = transactionOptional.get()
            transaction.accountNameOwner = account.accountNameOwner
            transaction.accountId = account.accountId
            transaction.dateUpdated = Timestamp(System.currentTimeMillis())
            transactionRepository.saveAndFlush(transaction)
        }
    }

    fun updateTransactionReceiptImageByGuidStandardized(guid: String, imageBase64Payload: String): ServiceResult<ReceiptImage> {
        return handleServiceOperation("updateTransactionReceiptImage", guid) {
            val imageBase64String = imageBase64Payload.replace("^data:image/[a-z]+;base64,[ ]?".toRegex(), "")
            val rawImage = Base64.getDecoder().decode(imageBase64String)
            val imageFormatType = imageProcessingService.getImageFormatType(rawImage)
            val thumbnail = imageProcessingService.createThumbnail(rawImage, imageFormatType)

            val optionalTransaction = transactionRepository.findByGuid(guid)
            if (optionalTransaction.isEmpty) {
                throw TransactionNotFoundException("Cannot save a image for a transaction that does not exist with guid = '$guid'.")
            }

            val transaction = optionalTransaction.get()

            if (transaction.receiptImageId != null) {
                val receiptImageOptional = receiptImageService.findByReceiptImageId(transaction.receiptImageId!!)
                if (receiptImageOptional.isPresent) {
                    val existingReceiptImage = receiptImageOptional.get()
                    existingReceiptImage.thumbnail = thumbnail
                    existingReceiptImage.image = rawImage
                    existingReceiptImage.imageFormatType = imageFormatType
                    return@handleServiceOperation receiptImageService.insertReceiptImage(existingReceiptImage)
                }
                throw ReceiptImageException("Failed to update receipt image for transaction ${transaction.guid}")
            }

            val receiptImage = ReceiptImage()
            receiptImage.transactionId = transaction.transactionId
            receiptImage.image = rawImage
            receiptImage.thumbnail = thumbnail
            receiptImage.imageFormatType = imageFormatType
            val response = receiptImageService.insertReceiptImage(receiptImage)
            transaction.receiptImageId = response.receiptImageId
            transaction.dateUpdated = Timestamp(System.currentTimeMillis())
            transactionRepository.saveAndFlush(transaction)
            meterService.incrementTransactionReceiptImageInserted(transaction.accountNameOwner)
            response
        }
    }

    fun createFutureTransactionStandardized(transaction: Transaction): ServiceResult<Transaction> {
        return handleServiceOperation("createFutureTransaction", transaction.guid) {
            val calendarTransactionDate = Calendar.getInstance()
            val calendarDueDate = Calendar.getInstance()
            calendarTransactionDate.time = transaction.transactionDate
            calculateFutureDate(transaction, calendarTransactionDate)
            val transactionFuture = Transaction()

            if (transaction.dueDate != null) {
                calendarDueDate.time = transaction.dueDate
                calculateFutureDate(transaction, calendarDueDate)
                transactionFuture.dueDate = Date(calendarDueDate.timeInMillis)
            }

            transactionFuture.guid = UUID.randomUUID().toString()
            transactionFuture.account = transaction.account
            transactionFuture.accountId = transaction.accountId
            transactionFuture.accountNameOwner = transaction.accountNameOwner
            transactionFuture.accountType = transaction.accountType
            transactionFuture.activeStatus = transaction.activeStatus
            transactionFuture.amount = transaction.amount
            transactionFuture.category = transaction.category
            transactionFuture.description = transaction.description
            transactionFuture.receiptImageId = null
            transactionFuture.notes = ""
            transactionFuture.reoccurringType = transaction.reoccurringType
            transactionFuture.transactionState = TransactionState.Future
            transactionFuture.transactionDate = Date(calendarTransactionDate.timeInMillis)
            val futureTimestamp = Timestamp(System.currentTimeMillis())
            transactionFuture.dateUpdated = futureTimestamp
            transactionFuture.dateAdded = futureTimestamp

            if (transactionFuture.reoccurringType == ReoccurringType.Undefined) {
                throw InvalidReoccurringTypeException("TransactionState cannot be undefined for reoccurring transactions.")
            }
            transactionFuture
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun deleteTransactionByGuid(guid: String): Boolean {
        val result = deleteById(guid)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> false
            else -> throw RuntimeException("Failed to delete transaction: $result")
        }
    }

    override fun deleteReceiptImage(transaction: Transaction): Boolean {
        val receiptImageId = transaction.receiptImageId
        if (receiptImageId == null) {
            logger.warn("No receipt image ID found for transaction GUID: ${transaction.guid}")
            return false
        }
        logger.info("Deleting receipt image with ID: $receiptImageId for transaction GUID: ${transaction.guid}")
        val receiptImageOptional = receiptImageService.findByReceiptImageId(receiptImageId)
        if (receiptImageOptional.isPresent) {
            receiptImageService.deleteReceiptImage(receiptImageOptional.get())
            meterService.incrementExceptionThrownCounter("ReceiptImageDeleted")
            logger.info("Successfully deleted receipt image for transaction GUID: ${transaction.guid}")
            return true
        }
        logger.warn("Receipt image not found with ID: $receiptImageId")
        return false
    }

    override fun insertTransaction(transaction: Transaction): Transaction {
        // Validate the transaction first
        val constraintViolations: Set<ConstraintViolation<Transaction>> = validator.validate(transaction)
        handleConstraintViolations(constraintViolations, meterService)

        // Check if transaction already exists by GUID
        val transactionOptional = findTransactionByGuid(transaction.guid)
        if (transactionOptional.isPresent) {
            val transactionFromDatabase = transactionOptional.get()
            meterService.incrementTransactionAlreadyExistsCounter(transactionFromDatabase.accountNameOwner)
            return masterTransactionUpdater(transactionFromDatabase, transaction)
        }

        // Create new transaction
        processAccount(transaction)
        processCategory(transaction)
        processDescription(transaction)
        val timestamp = Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = timestamp
        transaction.dateAdded = timestamp
        val response: Transaction = transactionRepository.saveAndFlush(transaction)
        meterService.incrementTransactionSuccessfullyInsertedCounter(transaction.accountNameOwner)
        logger.info("Inserted transaction into the database successfully, guid = ${transaction.guid}")
        return response
    }

    override fun findTransactionByGuid(guid: String): Optional<Transaction> {
        val result = findById(guid)
        return when (result) {
            is ServiceResult.Success -> Optional.of(result.data)
            else -> Optional.empty()
        }
    }

    override fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals {
        return calculationService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)
    }

    override fun findByAccountNameOwnerOrderByTransactionDate(accountNameOwner: String): List<Transaction> {
        val result = findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun updateTransaction(transaction: Transaction): Transaction {
        val result = update(transaction)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw TransactionValidationException("cannot update a transaction without a valid guid.")
            else -> throw RuntimeException("Failed to update transaction: $result")
        }
    }

    override fun masterTransactionUpdater(transactionFromDatabase: Transaction, transaction: Transaction): Transaction {
        if (transactionFromDatabase.guid == transaction.guid) {
            processCategory(transaction)
            val account = accountService.account(transaction.accountNameOwner).get()
            transaction.accountId = account.accountId
            transaction.dateAdded = transactionFromDatabase.dateAdded
            transaction.dateUpdated = Timestamp(System.currentTimeMillis())
            processDescription(transaction)
            return transactionRepository.saveAndFlush(transaction)
        }
        logger.warn("guid did not match any database records to update ${transaction.guid}.")
        throw TransactionValidationException("guid did not match any database records to update ${transaction.guid}.")
    }

    override fun updateTransactionReceiptImageByGuid(guid: String, imageBase64Payload: String): ReceiptImage {
        val result = updateTransactionReceiptImageByGuidStandardized(guid, imageBase64Payload)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw TransactionNotFoundException("Cannot save a image for a transaction that does not exist with guid = '$guid'.")
            else -> throw RuntimeException("Failed to update receipt image: $result")
        }
    }

    override fun changeAccountNameOwner(map: Map<String, String>): Transaction {
        val accountNameOwner = map["accountNameOwner"]
        val guid = map["guid"]

        if (guid == null || accountNameOwner == null) {
            logger.error("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
            meterService.incrementExceptionThrownCounter("AccountValidationException")
            throw AccountValidationException("Cannot change accountNameOwner for an input that has a null 'accountNameOwner' or a null 'guid'")
        }

        val result = changeAccountNameOwnerStandardized(accountNameOwner, guid)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw AccountValidationException("Cannot change accountNameOwner for a transaction that does not exist, guid='$guid'.")
            else -> throw RuntimeException("Failed to change account name owner: $result")
        }
    }

    override fun updateTransactionState(guid: String, transactionState: TransactionState): Transaction {
        val result = updateTransactionStateStandardized(guid, transactionState)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw TransactionNotFoundException("Cannot update transaction - the transaction is not found with guid = '$guid'")
            else -> throw RuntimeException("Failed to update transaction state: $result")
        }
    }

    override fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray {
        return imageProcessingService.createThumbnail(rawImage, imageFormatType)
    }

    override fun getImageFormatType(rawImage: ByteArray): ImageFormatType {
        return imageProcessingService.getImageFormatType(rawImage)
    }

    override fun createFutureTransaction(transaction: Transaction): Transaction {
        val result = createFutureTransactionStandardized(transaction)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> throw RuntimeException("Failed to create future transaction: $result")
        }
    }

    override fun findTransactionsByCategory(categoryName: String): List<Transaction> {
        val result = findTransactionsByCategoryStandardized(categoryName)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun findTransactionsByDescription(descriptionName: String): List<Transaction> {
        val result = findTransactionsByDescriptionStandardized(descriptionName)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    // ===== Preserved Business Logic Methods =====

    override fun processAccount(transaction: Transaction) {
        var accountOptional = accountService.account(transaction.accountNameOwner)
        if (accountOptional.isPresent) {
            val existingAccount = accountOptional.get()
            transaction.accountId = existingAccount.accountId
            transaction.accountType = existingAccount.accountType
            meterService.incrementTransactionAlreadyExistsCounter(transaction.accountNameOwner)
            logger.info("Using existing account: ${transaction.accountNameOwner}")
        } else {
            logger.info("Creating new account: ${transaction.accountNameOwner}")
            val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
            val savedAccount = accountService.insertAccount(account)
            meterService.incrementExceptionThrownCounter("AccountCreated")
            logger.info("Created new account: ${transaction.accountNameOwner} with ID: ${savedAccount.accountId}")
            transaction.accountId = savedAccount.accountId
            transaction.accountType = savedAccount.accountType
        }
    }

    override fun processCategory(transaction: Transaction) {
        when {
            transaction.category != "" -> {
                val optionalCategory = categoryService.category(transaction.category)
                if (optionalCategory.isPresent) {
                    transaction.categories.add(optionalCategory.get())
                    logger.info("Using existing category: ${transaction.category}")
                } else {
                    logger.info("Creating new category: ${transaction.category}")
                    val category = createDefaultCategory(transaction.category)
                    val savedCategory = categoryService.insertCategory(category)
                    logger.info("Created new category: ${transaction.category}")
                    transaction.categories.add(savedCategory)
                }
            }
        }
    }

    override fun processDescription(transaction: Transaction) {
        when {
            transaction.description != "" -> {
                val optionalDescription = descriptionService.description(transaction.description)
                if (!optionalDescription.isPresent) {
                    logger.info("Creating new description: ${transaction.description}")
                    val description = createDefaultDescription(transaction.description)
                    descriptionService.insertDescription(description)
                    logger.info("Created new description: ${transaction.description}")
                }
            }
        }
    }

    override fun createDefaultCategory(categoryName: String): Category {
        val category = Category()
        category.categoryName = categoryName
        return category
    }

    override fun createDefaultDescription(descriptionName: String): Description {
        val description = Description()
        description.descriptionName = descriptionName
        return description
    }

    override fun createDefaultAccount(accountNameOwner: String, accountType: AccountType): Account {
        val account = Account()
        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        return account
    }

    // ===== Private Helper Methods =====

    private fun calculateFutureDate(transaction: Transaction, calendar: Calendar) {
        if (transaction.reoccurringType == ReoccurringType.FortNightly) {
            calendar.add(Calendar.DATE, 14)
        } else {
            if (transaction.accountType == AccountType.Debit) {
                if (transaction.reoccurringType == ReoccurringType.Monthly) {
                    calendar.add(Calendar.MONTH, 1)
                } else {
                    logger.warn("debit transaction ReoccurringType needs to be configured.")
                    throw InvalidReoccurringTypeException("debit transaction ReoccurringType needs to be configured.")
                }
            } else {
                calendar.add(Calendar.YEAR, 1)
            }
        }
    }
}