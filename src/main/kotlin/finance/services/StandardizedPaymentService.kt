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
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
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
class StandardizedPaymentService(
    private val paymentRepository: PaymentRepository,
    private val transactionService: StandardizedTransactionService,
    private val accountService: StandardizedAccountService,
) : StandardizedBaseService<Payment, Long>() {
    override fun getEntityName(): String = "Payment"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Payment>> =
        handleServiceOperation("findAllActive", null) {
            paymentRepository.findAll().sortedByDescending { payment -> payment.transactionDate }
        }

    override fun findById(id: Long): ServiceResult<Payment> =
        handleServiceOperation("findById", id) {
            val optionalPayment = paymentRepository.findByPaymentId(id)
            if (optionalPayment.isPresent) {
                optionalPayment.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Payment not found: $id")
            }
        }

    override fun save(entity: Payment): ServiceResult<Payment> =
        handleServiceOperation("save", entity.paymentId) {
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
                    else -> throw RuntimeException("Failed to create destination transaction: $destinationResult")
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
                    else -> throw RuntimeException("Failed to create source transaction: $sourceResult")
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
            val existingPayment = paymentRepository.findByPaymentId(entity.paymentId!!)
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
            val optionalPayment = paymentRepository.findByPaymentId(id)
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
    fun insertPayment(payment: Payment): Payment {
        logger.info("Inserting new payment to destination account: ${payment.destinationAccount}")

        // Process destination account - create if missing
        processPaymentAccount(payment.destinationAccount)

        // Process source account - create if missing
        processPaymentAccount(payment.sourceAccount)

        // Retrieve account types for behavior inference
        val sourceAccount = accountService.account(payment.sourceAccount).get()
        val destinationAccount = accountService.account(payment.destinationAccount).get()

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
            else -> throw RuntimeException("Failed to create destination transaction: $destinationResult")
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
            else -> throw RuntimeException("Failed to create source transaction: $sourceResult")
        }

        // Use the standardized save method and handle ServiceResult
        // GUIDs are now set, so save() won't try to create transactions again
        val result = save(payment)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Validation failed: ${result.errors}", emptySet())
            }
            is ServiceResult.BusinessError -> {
                // Handle data integrity violations (e.g., duplicate payments)
                throw org.springframework.dao.DataIntegrityViolationException(result.message)
            }
            else -> throw RuntimeException("Failed to insert payment: $result")
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

    fun findByPaymentId(paymentId: Long): Optional<Payment> = paymentRepository.findByPaymentId(paymentId)

    fun deleteByPaymentId(paymentId: Long): Boolean {
        val optionalPayment = paymentRepository.findByPaymentId(paymentId)
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
        logger.info("Finding account: $accountNameOwner")
        val accountOptional = accountService.account(accountNameOwner)
        if (accountOptional.isPresent) {
            logger.info("Using existing account: $accountNameOwner")
        } else {
            logger.info("Creating new account: $accountNameOwner")
            val account = createDefaultAccount(accountNameOwner, AccountType.Credit)
            val savedAccount = accountService.insertAccount(account)
            logger.info("Created new account: $accountNameOwner with ID: ${savedAccount.accountId}")
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
     *
     * @param amount The absolute payment amount (always positive)
     * @param behavior The payment behavior determining sign logic
     * @return The signed transaction amount for the source account
     */
    private fun calculateSourceAmount(
        amount: BigDecimal,
        behavior: PaymentBehavior,
    ): BigDecimal =
        when (behavior) {
            PaymentBehavior.BILL_PAYMENT -> -amount.abs() // Asset decreases
            PaymentBehavior.TRANSFER -> -amount.abs() // Asset decreases
            PaymentBehavior.CASH_ADVANCE -> amount.abs() // Liability increases (more debt)
            PaymentBehavior.BALANCE_TRANSFER -> -amount.abs() // Liability decreases (debt moved)
            else -> -amount.abs() // Default: negative (safest)
        }

    /**
     * Calculates the transaction amount for the destination account based on payment behavior.
     *
     * @param amount The absolute payment amount (always positive)
     * @param behavior The payment behavior determining sign logic
     * @return The signed transaction amount for the destination account
     */
    private fun calculateDestinationAmount(
        amount: BigDecimal,
        behavior: PaymentBehavior,
    ): BigDecimal =
        when (behavior) {
            PaymentBehavior.BILL_PAYMENT -> -amount.abs() // Liability decreases (debt paid)
            PaymentBehavior.TRANSFER -> amount.abs() // Asset increases
            PaymentBehavior.CASH_ADVANCE -> amount.abs() // Asset increases (cash received)
            PaymentBehavior.BALANCE_TRANSFER -> amount.abs() // Liability increases (debt received)
            else -> -amount.abs() // Default: negative (safest)
        }

    // ===== Transaction Population Methods =====

    /**
     * Populates the source transaction for a payment.
     * Uses payment behavior to determine correct transaction amount sign.
     *
     * @param transactionSource The transaction object to populate
     * @param payment The payment entity
     * @param sourceAccountNameOwner The source account name
     * @param sourceAccountType The actual account type of the source account
     * @param behavior The payment behavior determining amount logic
     */
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

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use populateSourceTransaction instead
     */
    @Deprecated("Use populateSourceTransaction with behavior parameter")
    fun populateDebitTransaction(
        transactionDebit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        // Legacy behavior: assume BILL_PAYMENT (asset to liability)
        populateSourceTransaction(
            transactionDebit,
            payment,
            paymentAccountNameOwner,
            AccountType.Debit,
            PaymentBehavior.BILL_PAYMENT,
        )
    }

    /**
     * Populates the destination transaction for a payment.
     * Uses payment behavior to determine correct transaction amount sign.
     *
     * @param transactionDestination The transaction object to populate
     * @param payment The payment entity
     * @param sourceAccountNameOwner The source account name (for notes)
     * @param destinationAccountType The actual account type of the destination account
     * @param behavior The payment behavior determining amount logic
     */
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

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use populateDestinationTransaction instead
     */
    @Deprecated("Use populateDestinationTransaction with behavior parameter")
    fun populateCreditTransaction(
        transactionCredit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        // Legacy behavior: assume BILL_PAYMENT (asset to liability)
        populateDestinationTransaction(
            transactionCredit,
            payment,
            paymentAccountNameOwner,
            AccountType.Credit,
            PaymentBehavior.BILL_PAYMENT,
        )
    }
}
