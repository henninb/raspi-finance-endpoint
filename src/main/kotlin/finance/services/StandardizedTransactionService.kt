package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.AccountValidationException
import finance.domain.Category
import finance.domain.Description
import finance.domain.ImageFormatType
import finance.domain.InvalidReoccurringTypeException
import finance.domain.InvalidTransactionStateException
import finance.domain.ReceiptImage
import finance.domain.ReceiptImageException
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Totals
import finance.domain.Transaction
import finance.domain.TransactionNotFoundException
import finance.domain.TransactionState
import finance.domain.TransactionValidationException
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.sql.Date
import java.sql.Timestamp
import java.util.Base64
import java.util.Calendar
import java.util.UUID

/**
 * Standardized Transaction Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 * Uses extracted services from Phase 1: ImageProcessingService and CalculationService
 */
@Service
@Primary
class StandardizedTransactionService(
    private val transactionRepository: TransactionRepository,
    private val accountService: StandardizedAccountService,
    private val categoryService: StandardizedCategoryService,
    private val descriptionService: StandardizedDescriptionService,
    private val standardizedReceiptImageService: StandardizedReceiptImageService,
    private val imageProcessingService: ImageProcessingService,
    private val calculationService: CalculationService,
    private val paymentRepository: PaymentRepository,
) : StandardizedBaseService<Transaction, String>() {
    override fun getEntityName(): String = "Transaction"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Transaction>> =
        handleServiceOperation("findAllActive", null) {
            transactionRepository.findAll().filter { it.activeStatus }
        }

    override fun findById(id: String): ServiceResult<Transaction> =
        handleServiceOperation("findById", id) {
            val optionalTransaction = transactionRepository.findByGuid(id)
            if (optionalTransaction.isPresent) {
                optionalTransaction.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
            }
        }

    override fun save(entity: Transaction): ServiceResult<Transaction> =
        handleServiceOperation("save", entity.guid) {
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

    override fun update(entity: Transaction): ServiceResult<Transaction> =
        handleServiceOperation("update", entity.guid) {
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

    override fun deleteById(id: String): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalTransaction = transactionRepository.findByGuid(id)
            if (optionalTransaction.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
            }

            val transaction = optionalTransaction.get()

            // Check if transaction is referenced by any payments
            val referencingPayments = paymentRepository.findByGuidSourceOrGuidDestination(transaction.guid, transaction.guid)

            if (referencingPayments.isNotEmpty()) {
                val paymentCount = referencingPayments.size
                val paymentWord = if (paymentCount == 1) "payment" else "payments"
                throw org.springframework.dao.DataIntegrityViolationException(
                    "Cannot delete transaction ${transaction.guid} because it is referenced by " +
                        "$paymentCount $paymentWord. Please delete the related payments first.",
                )
            }

            transactionRepository.delete(transaction)
            true
        }

    // ===== Business-Specific ServiceResult Methods =====

    fun findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner: String): ServiceResult<List<Transaction>> {
        return handleServiceOperation("findByAccountNameOwnerOrderByTransactionDate", accountNameOwner) {
            val transactions =
                executeWithResilienceSync(
                    operation = {
                        transactionRepository.findByAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(accountNameOwner)
                    },
                    operationName = "findByAccountNameOwnerOrderByTransactionDate-$accountNameOwner",
                    timeoutSeconds = 60,
                )

            if (transactions.isEmpty()) {
                logger.error("No active transactions found for account owner: $accountNameOwner")
                meterService.incrementAccountListIsEmpty("non-existent-accounts")
                return@handleServiceOperation emptyList<Transaction>()
            }

            transactions.sortedWith(
                compareByDescending<Transaction> { it.transactionState }
                    .thenByDescending { it.transactionDate },
            )
        }
    }

    fun findTransactionsByCategoryStandardized(categoryName: String): ServiceResult<List<Transaction>> =
        handleServiceOperation("findTransactionsByCategory", categoryName) {
            val transactions = transactionRepository.findByCategoryAndActiveStatusOrderByTransactionDateDesc(categoryName)
            transactions.ifEmpty { emptyList() }
        }

    fun findTransactionsByDescriptionStandardized(descriptionName: String): ServiceResult<List<Transaction>> =
        handleServiceOperation("findTransactionsByDescription", descriptionName) {
            val transactions = transactionRepository.findByDescriptionAndActiveStatusOrderByTransactionDateDesc(descriptionName)
            transactions.ifEmpty { emptyList() }
        }

    fun findTransactionsByDateRangeStandardized(
        startDate: Date,
        endDate: Date,
        pageable: Pageable,
    ): ServiceResult<Page<Transaction>> =
        handleServiceOperation("findTransactionsByDateRange", null) {
            if (startDate.after(endDate)) {
                throw IllegalStateException("startDate must be before or equal to endDate")
            }
            transactionRepository.findByTransactionDateBetween(startDate, endDate, pageable)
        }

    fun updateTransactionStateStandardized(
        guid: String,
        transactionState: TransactionState,
    ): ServiceResult<Transaction> =
        handleServiceOperation("updateTransactionState", guid) {
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

    fun changeAccountNameOwnerStandardized(
        accountNameOwner: String,
        guid: String,
    ): ServiceResult<Transaction> =
        handleServiceOperation("changeAccountNameOwner", guid) {
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

    fun updateTransactionReceiptImageByGuidStandardized(
        guid: String,
        imageBase64Payload: String,
    ): ServiceResult<ReceiptImage> {
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
                val receiptImageResult = standardizedReceiptImageService.findById(transaction.receiptImageId!!)
                when (receiptImageResult) {
                    is ServiceResult.Success -> {
                        val existingReceiptImage = receiptImageResult.data
                        existingReceiptImage.thumbnail = thumbnail
                        existingReceiptImage.image = rawImage
                        existingReceiptImage.imageFormatType = imageFormatType
                        val updateResult = standardizedReceiptImageService.save(existingReceiptImage)
                        return@handleServiceOperation when (updateResult) {
                            is ServiceResult.Success -> updateResult.data
                            else -> throw ReceiptImageException("Failed to update receipt image for transaction ${transaction.guid}: $updateResult")
                        }
                    }
                    else -> throw ReceiptImageException("Failed to find receipt image for transaction ${transaction.guid}: $receiptImageResult")
                }
            }

            val receiptImage = ReceiptImage()
            receiptImage.transactionId = transaction.transactionId
            receiptImage.image = rawImage
            receiptImage.thumbnail = thumbnail
            receiptImage.imageFormatType = imageFormatType
            val insertResult = standardizedReceiptImageService.save(receiptImage)
            val response =
                when (insertResult) {
                    is ServiceResult.Success -> insertResult.data
                    else -> throw ReceiptImageException("Failed to insert receipt image for transaction ${transaction.guid}: $insertResult")
                }
            transaction.receiptImageId = response.receiptImageId
            transaction.dateUpdated = Timestamp(System.currentTimeMillis())
            transactionRepository.saveAndFlush(transaction)
            meterService.incrementTransactionReceiptImageInserted(transaction.accountNameOwner)
            response
        }
    }

    fun createFutureTransactionStandardized(transaction: Transaction): ServiceResult<Transaction> =
        handleServiceOperation("createFutureTransaction", transaction.guid) {
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

    // ===== Legacy Method Compatibility =====

    fun deleteReceiptImage(transaction: Transaction): Boolean {
        val receiptImageId = transaction.receiptImageId
        if (receiptImageId == null) {
            logger.warn("No receipt image ID found for transaction GUID: ${transaction.guid}")
            return false
        }
        logger.info("Deleting receipt image with ID: $receiptImageId for transaction GUID: ${transaction.guid}")
        val receiptImageResult = standardizedReceiptImageService.findById(receiptImageId)
        when (receiptImageResult) {
            is ServiceResult.Success -> {
                val deleteResult = standardizedReceiptImageService.deleteById(receiptImageId)
                when (deleteResult) {
                    is ServiceResult.Success -> {
                        meterService.incrementExceptionThrownCounter("ReceiptImageDeleted")
                        logger.info("Successfully deleted receipt image for transaction GUID: ${transaction.guid}")
                        return true
                    }
                    else -> {
                        logger.warn("Failed to delete receipt image with ID: $receiptImageId: $deleteResult")
                        return false
                    }
                }
            }
            else -> {
                logger.warn("Receipt image not found with ID: $receiptImageId: $receiptImageResult")
                return false
            }
        }
    }

    fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals = calculationService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

    fun findByAccountNameOwnerOrderByTransactionDate(accountNameOwner: String): List<Transaction> {
        val result = findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun masterTransactionUpdater(
        transactionFromDatabase: Transaction,
        transaction: Transaction,
    ): Transaction {
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

    fun updateTransactionReceiptImageByGuid(
        guid: String,
        imageBase64Payload: String,
    ): ReceiptImage {
        val result = updateTransactionReceiptImageByGuidStandardized(guid, imageBase64Payload)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw TransactionNotFoundException("Cannot save a image for a transaction that does not exist with guid = '$guid'.")
            is ServiceResult.BusinessError -> throw org.springframework.dao.DataIntegrityViolationException(result.message)
            else -> throw RuntimeException("Failed to update receipt image: $result")
        }
    }

    fun changeAccountNameOwner(map: Map<String, String>): Transaction {
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
            is ServiceResult.BusinessError -> throw org.springframework.dao.DataIntegrityViolationException(result.message)
            else -> throw RuntimeException("Failed to change account name owner: $result")
        }
    }

    fun updateTransactionState(
        guid: String,
        transactionState: TransactionState,
    ): Transaction {
        val result = updateTransactionStateStandardized(guid, transactionState)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw TransactionNotFoundException("Cannot update transaction - the transaction is not found with guid = '$guid'")
            is ServiceResult.BusinessError -> throw org.springframework.dao.DataIntegrityViolationException(result.message)
            else -> throw RuntimeException("Failed to update transaction state: $result")
        }
    }

    fun createThumbnail(
        rawImage: ByteArray,
        imageFormatType: ImageFormatType,
    ): ByteArray = imageProcessingService.createThumbnail(rawImage, imageFormatType)

    fun getImageFormatType(rawImage: ByteArray): ImageFormatType = imageProcessingService.getImageFormatType(rawImage)

    fun createFutureTransaction(transaction: Transaction): Transaction {
        val result = createFutureTransactionStandardized(transaction)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.BusinessError -> throw org.springframework.dao.DataIntegrityViolationException(result.message)
            else -> throw RuntimeException("Failed to create future transaction: $result")
        }
    }

    fun findTransactionsByCategory(categoryName: String): List<Transaction> {
        val result = findTransactionsByCategoryStandardized(categoryName)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun findTransactionsByDescription(descriptionName: String): List<Transaction> {
        val result = findTransactionsByDescriptionStandardized(descriptionName)
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun findTransactionsByDateRange(
        startDate: Date,
        endDate: Date,
        pageable: Pageable,
    ): Page<Transaction> {
        val result = findTransactionsByDateRangeStandardized(startDate, endDate, pageable)
        return when (result) {
            is ServiceResult.Success -> result.data
            else ->
                org.springframework.data.domain.Page
                    .empty(pageable)
        }
    }

    // ===== Preserved Business Logic Methods =====

    fun processAccount(transaction: Transaction) {
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

    fun processCategory(transaction: Transaction) {
        when {
            transaction.category != "" -> {
                when (val result = categoryService.findByCategoryNameStandardized(transaction.category)) {
                    is ServiceResult.Success -> {
                        transaction.categories.add(result.data)
                        logger.info("Using existing category: ${transaction.category}")
                    }
                    else -> {
                        logger.info("Creating new category: ${transaction.category}")
                        val category = createDefaultCategory(transaction.category)
                        when (val saveResult = categoryService.save(category)) {
                            is ServiceResult.Success -> {
                                logger.info("Created new category: ${transaction.category}")
                                transaction.categories.add(saveResult.data)
                            }
                            else -> {
                                logger.error("Failed to create category: ${transaction.category}")
                            }
                        }
                    }
                }
            }
        }
    }

    fun processDescription(transaction: Transaction) {
        when {
            transaction.description != "" -> {
                when (val findResult = descriptionService.findByDescriptionNameStandardized(transaction.description)) {
                    is ServiceResult.Success -> {
                        // Found existing description; nothing to do here
                    }
                    else -> {
                        logger.info("Creating new description: ${transaction.description}")
                        val description = createDefaultDescription(transaction.description)
                        val saveResult = descriptionService.save(description)
                        if (saveResult is ServiceResult.Success) {
                            logger.info("Created new description: ${transaction.description}")
                        } else {
                            logger.warn("Failed to create description: ${transaction.description} -> $saveResult")
                        }
                    }
                }
            }
        }
    }

    fun createDefaultCategory(categoryName: String): Category {
        val category = Category()
        category.categoryName = categoryName
        return category
    }

    fun createDefaultDescription(descriptionName: String): Description {
        val description = Description()
        description.descriptionName = descriptionName
        return description
    }

    fun createDefaultAccount(
        accountNameOwner: String,
        accountType: AccountType,
    ): Account {
        val account = Account()
        account.accountNameOwner = accountNameOwner
        account.moniker = "0000"
        account.accountType = accountType
        account.activeStatus = true
        return account
    }

    // ===== Private Helper Methods =====

    private fun calculateFutureDate(
        transaction: Transaction,
        calendar: Calendar,
    ) {
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
