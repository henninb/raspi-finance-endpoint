package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.PaymentBehavior
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.repositories.PaymentRepository
import finance.utils.TenantContext
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

/**
 * Standardized Payment Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val transactionService: TransactionService,
    private val accountService: AccountService,
) : CrudBaseService<Payment, Long>() {
    override fun getEntityName(): String = "Payment"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Payment>> =
        handleServiceOperation("findAllActive", null) {
            val owner = TenantContext.getCurrentOwner()
            paymentRepository.findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner, true, Pageable.unpaged()).content
        }

    override fun findById(id: Long): ServiceResult<Payment> =
        handleServiceOperation("findById", id) {
            val owner = TenantContext.getCurrentOwner()
            val optionalPayment = paymentRepository.findByOwnerAndPaymentId(owner, id)
            if (optionalPayment.isPresent) {
                optionalPayment.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Payment not found: $id")
            }
        }

    override fun save(entity: Payment): ServiceResult<Payment> =
        handleServiceOperation("save", entity.paymentId) {
            val owner = TenantContext.getCurrentOwner()
            entity.owner = owner

            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // If GUIDs are not set, we need to create transactions first
            // This prevents foreign key constraint violations
            if (entity.guidSource.isNullOrBlank() || entity.guidDestination.isNullOrBlank()) {
                logger.info("Creating transactions for payment: ${entity.sourceAccount} -> ${entity.destinationAccount}")

                // Process accounts (create if missing)
                processPaymentAccount(entity.destinationAccount)
                processPaymentAccount(entity.sourceAccount)

                // Retrieve account types for behavior inference
                val sourceAccount = accountService.account(entity.sourceAccount).get()
                val destinationAccount = accountService.account(entity.destinationAccount).get()

                // Infer payment behavior from account types
                val behavior =
                    PaymentBehavior.inferBehavior(
                        sourceAccount.accountType,
                        destinationAccount.accountType,
                    )
                logger.info("Payment behavior inferred: $behavior (${sourceAccount.accountType} -> ${destinationAccount.accountType})")

                // Create destination transaction
                val transactionDestination = Transaction()
                populateDestinationTransaction(
                    transactionDestination,
                    entity,
                    entity.sourceAccount,
                    destinationAccount.accountType,
                    behavior,
                )
                val destinationResult = transactionService.save(transactionDestination)
                when (destinationResult) {
                    is ServiceResult.Success -> {
                        entity.guidDestination = destinationResult.data.guid
                        logger.debug("Destination transaction created: ${destinationResult.data.guid}")
                    }

                    is ServiceResult.ValidationError -> {
                        throw jakarta.validation.ConstraintViolationException("Destination transaction validation failed: ${destinationResult.errors}", emptySet())
                    }

                    is ServiceResult.BusinessError -> {
                        throw org.springframework.dao.DataIntegrityViolationException("Destination transaction business error: ${destinationResult.message}")
                    }

                    else -> {
                        throw RuntimeException("Failed to create destination transaction: $destinationResult")
                    }
                }

                // Create source transaction
                val transactionSource = Transaction()
                populateSourceTransaction(
                    transactionSource,
                    entity,
                    entity.sourceAccount,
                    sourceAccount.accountType,
                    behavior,
                )
                val sourceResult = transactionService.save(transactionSource)
                when (sourceResult) {
                    is ServiceResult.Success -> {
                        entity.guidSource = sourceResult.data.guid
                        logger.debug("Source transaction created: ${sourceResult.data.guid}")
                    }

                    is ServiceResult.ValidationError -> {
                        throw jakarta.validation.ConstraintViolationException("Source transaction validation failed: ${sourceResult.errors}", emptySet())
                    }

                    is ServiceResult.BusinessError -> {
                        throw org.springframework.dao.DataIntegrityViolationException("Source transaction business error: ${sourceResult.message}")
                    }

                    else -> {
                        throw RuntimeException("Failed to create source transaction: $sourceResult")
                    }
                }
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            paymentRepository.saveAndFlush(entity)
        }

    override fun update(entity: Payment): ServiceResult<Payment> =
        handleServiceOperation("update", entity.paymentId) {
            val owner = TenantContext.getCurrentOwner()
            val existingPayment = paymentRepository.findByOwnerAndPaymentId(owner, entity.paymentId)
            if (existingPayment.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Payment not found: ${entity.paymentId}")
            }

            // Update fields from the provided entity
            val paymentToUpdate = existingPayment.get()
            paymentToUpdate.sourceAccount = entity.sourceAccount
            paymentToUpdate.destinationAccount = entity.destinationAccount
            paymentToUpdate.amount = entity.amount
            paymentToUpdate.transactionDate = entity.transactionDate
            paymentToUpdate.guidSource = entity.guidSource
            paymentToUpdate.guidDestination = entity.guidDestination
            paymentToUpdate.activeStatus = entity.activeStatus
            paymentToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

            paymentRepository.saveAndFlush(paymentToUpdate)
        }

    @Transactional
    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val owner = TenantContext.getCurrentOwner()
            val optionalPayment = paymentRepository.findByOwnerAndPaymentId(owner, id)
            if (optionalPayment.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Payment not found: $id")
            }

            val payment = optionalPayment.get()

            // Save GUIDs before removing the payment
            val savedGuidSource = payment.guidSource
            val savedGuidDestination = payment.guidDestination

            // Step 1: Delete the payment first to break FK references
            paymentRepository.delete(payment)
            paymentRepository.flush()
            logger.info("Payment deleted (flushed) to allow cascade transaction deletes: $id")

            // Step 2: Delete associated transactions (cascade delete)
            val transactionsDeleted = deleteAssociatedTransactions(savedGuidSource, savedGuidDestination)
            logger.info(
                "Deleted $transactionsDeleted transaction(s) for payment $id: " +
                    "source=$savedGuidSource, destination=$savedGuidDestination",
            )

            true
        }

    // ===== Paginated ServiceResult Methods =====

    /**
     * Find all active payments with pagination.
     * Sorted by transactionDate descending.
     */
    fun findAllActive(pageable: Pageable): ServiceResult<Page<Payment>> =
        handleServiceOperation("findAllActive-paginated", null) {
            val owner = TenantContext.getCurrentOwner()
            paymentRepository.findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner, true, pageable)
        }

    /**
     * Delete transactions associated with a payment (cascade delete helper)
     * Returns the number of transactions successfully deleted
     */
    private fun deleteAssociatedTransactions(
        guidSource: String?,
        guidDestination: String?,
    ): Int {
        var deletedCount = 0

        // Delete source transaction
        if (!guidSource.isNullOrBlank()) {
            when (val result = transactionService.deleteByIdInternal(guidSource)) {
                is ServiceResult.Success -> {
                    deletedCount++
                    logger.info("Deleted source transaction: $guidSource")
                }

                is ServiceResult.NotFound -> {
                    logger.warn("Source transaction not found: $guidSource")
                }

                is ServiceResult.BusinessError -> {
                    logger.error("Failed to delete source transaction: ${result.message}")
                    throw org.springframework.dao.DataIntegrityViolationException(
                        "Cannot delete payment because source transaction $guidSource could not be deleted: ${result.message}",
                    )
                }

                else -> {
                    throw RuntimeException("Unexpected error deleting source transaction: $result")
                }
            }
        }

        // Delete destination transaction
        if (!guidDestination.isNullOrBlank()) {
            when (val result = transactionService.deleteByIdInternal(guidDestination)) {
                is ServiceResult.Success -> {
                    deletedCount++
                    logger.info("Deleted destination transaction: $guidDestination")
                }

                is ServiceResult.NotFound -> {
                    logger.warn("Destination transaction not found: $guidDestination")
                }

                is ServiceResult.BusinessError -> {
                    logger.error("Failed to delete destination transaction: ${result.message}")
                    throw org.springframework.dao.DataIntegrityViolationException(
                        "Cannot delete payment because destination transaction $guidDestination could not be deleted: ${result.message}",
                    )
                }

                else -> {
                    throw RuntimeException("Unexpected error deleting destination transaction: $result")
                }
            }
        }

        return deletedCount
    }

    // ===== Legacy Method Compatibility =====

    fun findAllPayments(): List<Payment> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    /**
     * Legacy insertPayment method - now uses new behavior-aware logic.
     * @deprecated Consider using save() method instead
     */
    @org.springframework.transaction.annotation.Transactional
    fun insertPayment(payment: Payment): Payment {
        val owner = TenantContext.getCurrentOwner()
        payment.owner = owner
        logger.info("Inserting new payment to destination account: ${payment.destinationAccount}")

        // Process destination account - create if missing
        processPaymentAccount(payment.destinationAccount)

        // Process source account - create if missing
        processPaymentAccount(payment.sourceAccount)

        // Retrieve account types for behavior inference
        val sourceAccountOpt = accountService.account(payment.sourceAccount)
        if (sourceAccountOpt.isEmpty) {
            logger.error("Source account not found after creation attempt: ${payment.sourceAccount}")
            meterService.incrementExceptionThrownCounter("PaymentSourceAccountNotFound")
            throw IllegalStateException("Source account not found: ${payment.sourceAccount}")
        }
        val destinationAccountOpt = accountService.account(payment.destinationAccount)
        if (destinationAccountOpt.isEmpty) {
            logger.error("Destination account not found after creation attempt: ${payment.destinationAccount}")
            meterService.incrementExceptionThrownCounter("PaymentDestinationAccountNotFound")
            throw IllegalStateException("Destination account not found: ${payment.destinationAccount}")
        }

        val sourceAccount = sourceAccountOpt.get()
        val destinationAccount = destinationAccountOpt.get()

        // Infer payment behavior from account types
        val behavior =
            PaymentBehavior.inferBehavior(
                sourceAccount.accountType,
                destinationAccount.accountType,
            )
        logger.info("Payment behavior inferred: $behavior (${sourceAccount.accountType} -> ${destinationAccount.accountType})")

        // Create transactions
        val transactionDestination = Transaction()
        val transactionSource = Transaction()

        val paymentAccountNameOwner = payment.sourceAccount
        populateDestinationTransaction(
            transactionDestination,
            payment,
            paymentAccountNameOwner,
            destinationAccount.accountType,
            behavior,
        )
        populateSourceTransaction(
            transactionSource,
            payment,
            paymentAccountNameOwner,
            sourceAccount.accountType,
            behavior,
        )

        logger.info("Creating source and destination transactions for payment")

        // Create destination transaction using ServiceResult pattern
        val destinationResult = transactionService.save(transactionDestination)
        when (destinationResult) {
            is ServiceResult.Success -> {
                payment.guidDestination = destinationResult.data.guid
                logger.debug("Destination transaction created successfully: ${destinationResult.data.guid}")
            }

            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Destination transaction validation failed: ${destinationResult.errors}", emptySet())
            }

            is ServiceResult.BusinessError -> {
                throw org.springframework.dao.DataIntegrityViolationException("Destination transaction business error: ${destinationResult.message}")
            }

            else -> {
                throw RuntimeException("Failed to create destination transaction: $destinationResult")
            }
        }

        // Create source transaction using ServiceResult pattern
        val sourceResult = transactionService.save(transactionSource)
        when (sourceResult) {
            is ServiceResult.Success -> {
                payment.guidSource = sourceResult.data.guid
                logger.debug("Source transaction created successfully: ${sourceResult.data.guid}")
            }

            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Source transaction validation failed: ${sourceResult.errors}", emptySet())
            }

            is ServiceResult.BusinessError -> {
                throw org.springframework.dao.DataIntegrityViolationException("Source transaction business error: ${sourceResult.message}")
            }

            else -> {
                throw RuntimeException("Failed to create source transaction: $sourceResult")
            }
        }

        // Use the standardized save method and handle ServiceResult
        // GUIDs are now set, so save() won't try to create transactions again
        val result = save(payment)
        return when (result) {
            is ServiceResult.Success -> {
                result.data
            }

            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Validation failed: ${result.errors}", emptySet())
            }

            is ServiceResult.BusinessError -> {
                // Handle data integrity violations (e.g., duplicate payments)
                throw org.springframework.dao.DataIntegrityViolationException(result.message)
            }

            else -> {
                throw RuntimeException("Failed to insert payment: $result")
            }
        }
    }

    fun updatePayment(
        paymentId: Long,
        patch: Payment,
    ): Payment {
        // Set the ID for the update operation
        patch.paymentId = paymentId
        val result = update(patch)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw RuntimeException("Payment not updated as the payment does not exist: $paymentId.")
            else -> throw RuntimeException("Failed to update payment: $result")
        }
    }

    fun findByPaymentId(paymentId: Long): Optional<Payment> {
        val owner = TenantContext.getCurrentOwner()
        return paymentRepository.findByOwnerAndPaymentId(owner, paymentId)
    }

    fun deleteByPaymentId(paymentId: Long): Boolean {
        val owner = TenantContext.getCurrentOwner()
        val optionalPayment = paymentRepository.findByOwnerAndPaymentId(owner, paymentId)
        if (optionalPayment.isPresent) {
            paymentRepository.delete(optionalPayment.get())
            return true
        }
        return false
    }

    // ===== Helper Methods for Payment Processing =====

    /**
     * Process payment account - create if missing (similar to TransactionService.processAccount)
     */
    private fun processPaymentAccount(accountNameOwner: String) {
        logger.debug("Processing payment account: $accountNameOwner")
        val accountOptional = accountService.account(accountNameOwner)
        if (accountOptional.isPresent) {
            logger.info("Using existing account for payment: $accountNameOwner (accountId: ${accountOptional.get().accountId})")
        } else {
            logger.info("Account not found for payment, creating new account: $accountNameOwner")
            try {
                val account = createDefaultAccount(accountNameOwner, AccountType.Credit)
                val savedAccount = accountService.insertAccount(account)
                logger.info("Created new account for payment: $accountNameOwner with ID: ${savedAccount.accountId}")
            } catch (ex: Exception) {
                logger.error("Failed to create account for payment: $accountNameOwner", ex)
                meterService.incrementExceptionCaughtCounter("PaymentAccountCreationFailed")
                throw org.springframework.dao.DataIntegrityViolationException("Failed to create account: $accountNameOwner: ${ex.message}", ex)
            }
        }
    }

    /**
     * Create a default account (similar to TransactionService.createDefaultAccount)
     */
    private fun createDefaultAccount(
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

    // ===== Payment Behavior and Amount Calculation Methods =====

    /**
     * Calculates the transaction amount for the source account based on payment behavior.
     */
    private fun calculateSourceAmount(
        amount: BigDecimal,
        behavior: PaymentBehavior,
    ): BigDecimal =
        when (behavior) {
            PaymentBehavior.BILL_PAYMENT -> -amount.abs()
            PaymentBehavior.TRANSFER -> -amount.abs()
            PaymentBehavior.CASH_ADVANCE -> amount.abs()
            PaymentBehavior.BALANCE_TRANSFER -> amount.abs()
            else -> -amount.abs()
        }

    /**
     * Calculates the transaction amount for the destination account based on payment behavior.
     */
    private fun calculateDestinationAmount(
        amount: BigDecimal,
        behavior: PaymentBehavior,
    ): BigDecimal =
        when (behavior) {
            PaymentBehavior.BILL_PAYMENT -> -amount.abs()
            PaymentBehavior.TRANSFER -> amount.abs()
            PaymentBehavior.CASH_ADVANCE -> amount.abs()
            PaymentBehavior.BALANCE_TRANSFER -> -amount.abs()
            else -> -amount.abs()
        }

    // ===== Transaction Population Methods =====

    fun populateSourceTransaction(
        transactionSource: finance.domain.Transaction,
        payment: Payment,
        sourceAccountNameOwner: String,
        sourceAccountType: AccountType,
        behavior: PaymentBehavior,
    ) {
        transactionSource.guid = UUID.randomUUID().toString()
        transactionSource.transactionDate = payment.transactionDate
        transactionSource.description = "payment"
        transactionSource.category = "bill_pay"
        transactionSource.notes = "to ${payment.destinationAccount}"
        transactionSource.amount = calculateSourceAmount(payment.amount, behavior)
        transactionSource.transactionState = TransactionState.Outstanding
        transactionSource.reoccurringType = ReoccurringType.Onetime
        transactionSource.accountType = sourceAccountType
        transactionSource.accountNameOwner = sourceAccountNameOwner
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionSource.dateUpdated = timestamp
        transactionSource.dateAdded = timestamp
    }

    @Deprecated("Use populateSourceTransaction with behavior parameter")
    fun populateDebitTransaction(
        transactionDebit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        populateSourceTransaction(
            transactionDebit,
            payment,
            paymentAccountNameOwner,
            AccountType.Debit,
            PaymentBehavior.BILL_PAYMENT,
        )
    }

    fun populateDestinationTransaction(
        transactionDestination: finance.domain.Transaction,
        payment: Payment,
        sourceAccountNameOwner: String,
        destinationAccountType: AccountType,
        behavior: PaymentBehavior,
    ) {
        transactionDestination.guid = UUID.randomUUID().toString()
        transactionDestination.transactionDate = payment.transactionDate
        transactionDestination.description = "payment"
        transactionDestination.category = "bill_pay"
        transactionDestination.notes = "from $sourceAccountNameOwner"
        transactionDestination.amount = calculateDestinationAmount(payment.amount, behavior)
        transactionDestination.transactionState = TransactionState.Outstanding
        transactionDestination.reoccurringType = ReoccurringType.Onetime
        transactionDestination.accountType = destinationAccountType
        transactionDestination.accountNameOwner = payment.destinationAccount
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionDestination.dateUpdated = timestamp
        transactionDestination.dateAdded = timestamp
    }

    @Deprecated("Use populateDestinationTransaction with behavior parameter")
    fun populateCreditTransaction(
        transactionCredit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        populateDestinationTransaction(
            transactionCredit,
            payment,
            paymentAccountNameOwner,
            AccountType.Credit,
            PaymentBehavior.BILL_PAYMENT,
        )
    }
}
