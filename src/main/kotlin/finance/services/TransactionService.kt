package finance.services

import finance.configurations.ResilienceComponents
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
import finance.utils.TenantContext
import jakarta.validation.Validator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

@Service
class TransactionService
    constructor(
        private val transactionRepository: TransactionRepository,
        private val accountService: AccountService,
        private val categoryService: CategoryService,
        private val descriptionService: DescriptionService,
        private val receiptImageService: ReceiptImageService,
        private val imageProcessingService: ImageProcessingService,
        private val calculationService: CalculationService,
        private val paymentRepository: PaymentRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents? = null,
    ) : CrudBaseService<Transaction, String>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Transaction"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<Transaction>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                transactionRepository.findByOwnerAndActiveStatus(owner, true, Pageable.unpaged()).content
            }

        override fun findById(id: String): ServiceResult<Transaction> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransaction = transactionRepository.findByOwnerAndGuid(owner, id)
                if (optionalTransaction.isPresent) {
                    optionalTransaction.get()
                } else {
                    throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
                }
            }

        override fun save(entity: Transaction): ServiceResult<Transaction> =
            handleServiceOperation("save", entity.guid) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                // Check for existing transaction
                val existingOptional = transactionRepository.findByOwnerAndGuid(owner, entity.guid)
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
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                val existingTransaction = transactionRepository.findByOwnerAndGuid(owner, entity.guid)
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

        override fun deleteById(id: String): ServiceResult<Transaction> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransaction = transactionRepository.findByOwnerAndGuid(owner, id)
                if (optionalTransaction.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
                }

                val transaction = optionalTransaction.get()

                // Check if transaction is referenced by any payments
                val referencingPayments = paymentRepository.findByOwnerAndGuidSourceOrOwnerAndGuidDestination(owner, transaction.guid, owner, transaction.guid)

                if (referencingPayments.isNotEmpty()) {
                    val paymentCount = referencingPayments.size
                    val paymentWord = if (paymentCount == 1) "payment" else "payments"
                    throw org.springframework.dao.DataIntegrityViolationException(
                        "Cannot delete transaction ${transaction.guid} because it is referenced by " +
                            "$paymentCount $paymentWord. Please delete the related payments first.",
                    )
                }

                transactionRepository.delete(transaction)
                transaction
            }

        /**
         * Internal delete method for cascade operations - bypasses payment reference check
         * This method should only be called from payment cascade delete logic
         */
        fun deleteByIdInternal(id: String): ServiceResult<Transaction> =
            handleServiceOperation("deleteByIdInternal", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransaction = transactionRepository.findByOwnerAndGuid(owner, id)
                if (optionalTransaction.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
                }

                val transaction = optionalTransaction.get()
                transactionRepository.delete(transaction)
                // Flush to surface FK violations immediately and ensure operation order
                transactionRepository.flush()
                logger.info("Transaction deleted (cascade): ${transaction.guid}")
                transaction
            }

        // ===== Business-Specific ServiceResult Methods =====

        fun findByAccountNameOwnerOrderByTransactionDateStandardized(accountNameOwner: String): ServiceResult<List<Transaction>> {
            return handleServiceOperation("findByAccountNameOwnerOrderByTransactionDate", accountNameOwner) {
                val owner = TenantContext.getCurrentOwner()
                val transactions =
                    executeWithResilienceSync(
                        operation = {
                            transactionRepository.findByOwnerAndAccountNameOwnerAndActiveStatusOrderByTransactionDateDesc(owner, accountNameOwner)
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
                val owner = TenantContext.getCurrentOwner()
                val transactions = transactionRepository.findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc(owner, categoryName)
                transactions.ifEmpty { emptyList() }
            }

        fun findTransactionsByDescriptionStandardized(descriptionName: String): ServiceResult<List<Transaction>> =
            handleServiceOperation("findTransactionsByDescription", descriptionName) {
                val owner = TenantContext.getCurrentOwner()
                val transactions = transactionRepository.findByOwnerAndDescriptionAndActiveStatusOrderByTransactionDateDesc(owner, descriptionName)
                transactions.ifEmpty { emptyList() }
            }

        fun findTransactionsByDateRangeStandardized(
            startDate: LocalDate,
            endDate: LocalDate,
            pageable: Pageable,
        ): ServiceResult<Page<Transaction>> =
            handleServiceOperation("findTransactionsByDateRange", null) {
                val owner = TenantContext.getCurrentOwner()
                if (startDate.isAfter(endDate)) {
                    throw IllegalStateException("startDate must be before or equal to endDate")
                }
                transactionRepository.findByOwnerAndTransactionDateBetween(owner, startDate, endDate, pageable)
            }

        // ===== Paginated ServiceResult Methods =====

        /**
         * Find all active transactions with pagination.
         * Applies two-tier sorting: transactionState DESC, transactionDate DESC.
         */
        fun findAllActive(pageable: Pageable): ServiceResult<Page<Transaction>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                val sortedPageable = applyTransactionSort(pageable)
                transactionRepository.findByOwnerAndActiveStatus(owner, true, sortedPageable)
            }

        /**
         * Find transactions by account name owner with pagination.
         * Applies two-tier sorting: transactionState DESC, transactionDate DESC.
         */
        fun findByAccountNameOwnerOrderByTransactionDateStandardized(
            accountNameOwner: String,
            pageable: Pageable,
        ): ServiceResult<Page<Transaction>> =
            handleServiceOperation("findByAccountNameOwner-paginated", accountNameOwner) {
                val owner = TenantContext.getCurrentOwner()
                val sortedPageable = applyTransactionSort(pageable)
                val page =
                    executeWithResilienceSync(
                        operation = {
                            transactionRepository.findByOwnerAndAccountNameOwnerAndActiveStatus(
                                owner,
                                accountNameOwner,
                                true,
                                sortedPageable,
                            )
                        },
                        operationName = "findByAccountNameOwner-paginated-$accountNameOwner",
                        timeoutSeconds = 60,
                    )

                if (page.isEmpty) {
                    logger.warn("No active transactions found for account owner: $accountNameOwner")
                    meterService.incrementAccountListIsEmpty("non-existent-accounts")
                }
                page
            }

        /**
         * Find transactions by category with pagination.
         * Applies two-tier sorting: transactionState DESC, transactionDate DESC.
         */
        fun findTransactionsByCategoryStandardized(
            categoryName: String,
            pageable: Pageable,
        ): ServiceResult<Page<Transaction>> =
            handleServiceOperation("findTransactionsByCategory-paginated", categoryName) {
                val owner = TenantContext.getCurrentOwner()
                val sortedPageable = applyTransactionSort(pageable)
                transactionRepository.findByOwnerAndCategoryAndActiveStatus(owner, categoryName, true, sortedPageable)
            }

        /**
         * Find transactions by description with pagination.
         * Applies two-tier sorting: transactionState DESC, transactionDate DESC.
         */
        fun findTransactionsByDescriptionStandardized(
            descriptionName: String,
            pageable: Pageable,
        ): ServiceResult<Page<Transaction>> =
            handleServiceOperation("findTransactionsByDescription-paginated", descriptionName) {
                val owner = TenantContext.getCurrentOwner()
                val sortedPageable = applyTransactionSort(pageable)
                transactionRepository.findByOwnerAndDescriptionAndActiveStatus(owner, descriptionName, true, sortedPageable)
            }

        /**
         * Apply two-tier sorting to Pageable for transactions.
         * Enforces: transactionState DESC, transactionDate DESC
         * This preserves the existing transaction ordering logic at database level.
         */
        private fun applyTransactionSort(pageable: Pageable): Pageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(
                    Sort.Order.desc("transactionState"),
                    Sort.Order.desc("transactionDate"),
                ),
            )

        fun updateTransactionStateStandardized(
            guid: String,
            transactionState: TransactionState,
        ): ServiceResult<Transaction> =
            handleServiceOperation("updateTransactionState", guid) {
                val owner = TenantContext.getCurrentOwner()
                val transactionOptional = transactionRepository.findByOwnerAndGuid(owner, guid)
                if (transactionOptional.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transaction not found: $guid")
                }

                val transaction = transactionOptional.get()
                if (transactionState == transaction.transactionState) {
                    throw InvalidTransactionStateException("Cannot update transactionState to the same for guid = '$guid'")
                }

                if (transactionState == TransactionState.Cleared &&
                    transaction.transactionDate.isAfter(LocalDate.now())
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
                val owner = TenantContext.getCurrentOwner()
                val accountOptional = accountService.account(accountNameOwner)
                val transactionOptional = transactionRepository.findByOwnerAndGuid(owner, guid)

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
                val owner = TenantContext.getCurrentOwner()
                val imageBase64String = imageBase64Payload.replace("^data:image/[a-z]+;base64,[ ]?".toRegex(), "")
                val rawImage = Base64.getDecoder().decode(imageBase64String)
                val imageFormatType = imageProcessingService.getImageFormatType(rawImage)
                val thumbnail = imageProcessingService.createThumbnail(rawImage, imageFormatType)

                val optionalTransaction = transactionRepository.findByOwnerAndGuid(owner, guid)
                if (optionalTransaction.isEmpty) {
                    throw TransactionNotFoundException("Cannot save a image for a transaction that does not exist with guid = '$guid'.")
                }

                val transaction = optionalTransaction.get()

                if (transaction.receiptImageId != null) {
                    val receiptImageResult = receiptImageService.findById(transaction.receiptImageId!!)
                    when (receiptImageResult) {
                        is ServiceResult.Success -> {
                            val existingReceiptImage = receiptImageResult.data
                            existingReceiptImage.thumbnail = thumbnail
                            existingReceiptImage.image = rawImage
                            existingReceiptImage.imageFormatType = imageFormatType
                            val updateResult = receiptImageService.save(existingReceiptImage)
                            return@handleServiceOperation when (updateResult) {
                                is ServiceResult.Success -> updateResult.data
                                else -> throw ReceiptImageException("Failed to update receipt image for transaction ${transaction.guid}: $updateResult")
                            }
                        }

                        else -> {
                            throw ReceiptImageException("Failed to find receipt image for transaction ${transaction.guid}: $receiptImageResult")
                        }
                    }
                }

                val receiptImage = ReceiptImage()
                receiptImage.transactionId = transaction.transactionId
                receiptImage.image = rawImage
                receiptImage.thumbnail = thumbnail
                receiptImage.imageFormatType = imageFormatType
                val insertResult = receiptImageService.save(receiptImage)
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
                val owner = TenantContext.getCurrentOwner()
                // Calculate future transaction date using LocalDate
                val futureTransactionDate = calculateFutureLocalDate(transaction, transaction.transactionDate)

                val transactionFuture = Transaction()
                transactionFuture.owner = owner

                // Calculate future due date if present
                if (transaction.dueDate != null) {
                    transactionFuture.dueDate = calculateFutureLocalDate(transaction, transaction.dueDate!!)
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
                transactionFuture.transactionDate = futureTransactionDate
                val futureTimestamp = Timestamp(System.currentTimeMillis())
                transactionFuture.dateUpdated = futureTimestamp
                transactionFuture.dateAdded = futureTimestamp

                if (transactionFuture.reoccurringType == ReoccurringType.Undefined) {
                    throw InvalidReoccurringTypeException("TransactionState cannot be undefined for reoccurring transactions.")
                }
                transactionFuture
            }

        // ===== Legacy Method Compatibility =====

        fun calculateActiveTotalsByAccountNameOwner(accountNameOwner: String): Totals = calculationService.calculateActiveTotalsByAccountNameOwner(accountNameOwner)

        fun masterTransactionUpdater(
            transactionFromDatabase: Transaction,
            transaction: Transaction,
        ): Transaction {
            logger.debug("Updating transaction: ${transaction.guid}")
            if (transactionFromDatabase.guid == transaction.guid) {
                processCategory(transaction)
                val accountOptional = accountService.account(transaction.accountNameOwner)
                if (accountOptional.isEmpty) {
                    logger.error("Account not found for transaction update: ${transaction.accountNameOwner}")
                    meterService.incrementExceptionThrownCounter("AccountNotFoundOnUpdate")
                    throw AccountValidationException("Account not found: ${transaction.accountNameOwner}")
                }
                val account = accountOptional.get()
                transaction.accountId = account.accountId
                transaction.dateAdded = transactionFromDatabase.dateAdded
                transaction.dateUpdated = Timestamp(System.currentTimeMillis())
                processDescription(transaction)
                logger.info("Successfully updated transaction: ${transaction.guid}")
                return transactionRepository.saveAndFlush(transaction)
            }
            logger.warn("guid did not match any database records to update ${transaction.guid}.")
            meterService.incrementExceptionThrownCounter("TransactionGuidMismatch")
            throw TransactionValidationException("guid did not match any database records to update ${transaction.guid}.")
        }

        fun createThumbnail(
            rawImage: ByteArray,
            imageFormatType: ImageFormatType,
        ): ByteArray = imageProcessingService.createThumbnail(rawImage, imageFormatType)

        fun getImageFormatType(rawImage: ByteArray): ImageFormatType = imageProcessingService.getImageFormatType(rawImage)

        // ===== Preserved Business Logic Methods =====

        fun processAccount(transaction: Transaction) {
            logger.debug("Processing account for transaction: ${transaction.guid}, accountNameOwner: ${transaction.accountNameOwner}")
            var accountOptional = accountService.account(transaction.accountNameOwner)
            if (accountOptional.isPresent) {
                val existingAccount = accountOptional.get()
                transaction.accountId = existingAccount.accountId
                transaction.accountType = existingAccount.accountType
                meterService.incrementTransactionAlreadyExistsCounter(transaction.accountNameOwner)
                logger.info("Using existing account: ${transaction.accountNameOwner} with accountId: ${existingAccount.accountId}")
            } else {
                logger.info("Account not found, creating new account: ${transaction.accountNameOwner}")
                try {
                    val account = createDefaultAccount(transaction.accountNameOwner, transaction.accountType)
                    val savedAccount = accountService.insertAccount(account)
                    meterService.incrementExceptionThrownCounter("AccountCreated")
                    logger.info("Created new account: ${transaction.accountNameOwner} with ID: ${savedAccount.accountId}")
                    transaction.accountId = savedAccount.accountId
                    transaction.accountType = savedAccount.accountType
                } catch (ex: Exception) {
                    logger.error("Failed to create account: ${transaction.accountNameOwner}", ex)
                    meterService.incrementExceptionCaughtCounter("AccountCreationFailed")
                    throw AccountValidationException("Failed to create account: ${transaction.accountNameOwner}: ${ex.message}")
                }
            }
        }

        fun processCategory(transaction: Transaction) {
            if (transaction.category.isNotEmpty()) {
                when (val result = categoryService.findByCategoryNameStandardized(transaction.category)) {
                    is ServiceResult.Success -> {
                        transaction.categories.add(result.data)
                        logger.info("Using existing category: ${transaction.category}")
                    }

                    is ServiceResult.NotFound, is ServiceResult.BusinessError, is ServiceResult.SystemError, is ServiceResult.ValidationError -> {
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

        fun processDescription(transaction: Transaction) {
            if (transaction.description.isNotEmpty()) {
                when (val findResult = descriptionService.findByDescriptionNameStandardized(transaction.description)) {
                    is ServiceResult.Success -> {
                        // Found existing description; nothing to do here
                    }

                    is ServiceResult.NotFound, is ServiceResult.BusinessError, is ServiceResult.SystemError, is ServiceResult.ValidationError -> {
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

        // ===== Private Helper Methods =====

        private fun calculateFutureLocalDate(
            transaction: Transaction,
            date: LocalDate,
        ): LocalDate =
            if (transaction.reoccurringType == ReoccurringType.FortNightly) {
                date.plusDays(14)
            } else {
                if (transaction.accountType == AccountType.Debit) {
                    if (transaction.reoccurringType == ReoccurringType.Monthly) {
                        date.plusMonths(1)
                    } else {
                        logger.warn("debit transaction ReoccurringType needs to be configured.")
                        throw InvalidReoccurringTypeException("debit transaction ReoccurringType needs to be configured.")
                    }
                } else {
                    date.plusYears(1)
                }
            }
    }
