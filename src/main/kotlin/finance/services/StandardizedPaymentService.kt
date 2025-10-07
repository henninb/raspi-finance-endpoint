package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
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

                // Validate destination account type
                val destinationAccount = accountService.account(entity.destinationAccount).get()
                if (destinationAccount.accountType == AccountType.Debit) {
                    throw ValidationException("Account cannot make a payment to a debit account: ${entity.destinationAccount}")
                }

                // Create credit transaction
                val transactionCredit = Transaction()
                populateCreditTransaction(transactionCredit, entity, entity.sourceAccount)
                val creditResult = transactionService.save(transactionCredit)
                when (creditResult) {
                    is ServiceResult.Success -> {
                        entity.guidDestination = creditResult.data.guid
                        logger.debug("Credit transaction created: ${creditResult.data.guid}")
                    }
                    is ServiceResult.ValidationError -> {
                        throw jakarta.validation.ConstraintViolationException("Credit transaction validation failed: ${creditResult.errors}", emptySet())
                    }
                    is ServiceResult.BusinessError -> {
                        throw org.springframework.dao.DataIntegrityViolationException("Credit transaction business error: ${creditResult.message}")
                    }
                    else -> throw RuntimeException("Failed to create credit transaction: $creditResult")
                }

                // Create debit transaction
                val transactionDebit = Transaction()
                populateDebitTransaction(transactionDebit, entity, entity.sourceAccount)
                val debitResult = transactionService.save(transactionDebit)
                when (debitResult) {
                    is ServiceResult.Success -> {
                        entity.guidSource = debitResult.data.guid
                        logger.debug("Debit transaction created: ${debitResult.data.guid}")
                    }
                    is ServiceResult.ValidationError -> {
                        throw jakarta.validation.ConstraintViolationException("Debit transaction validation failed: ${debitResult.errors}", emptySet())
                    }
                    is ServiceResult.BusinessError -> {
                        throw org.springframework.dao.DataIntegrityViolationException("Debit transaction business error: ${debitResult.message}")
                    }
                    else -> throw RuntimeException("Failed to create debit transaction: $debitResult")
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

    fun insertPayment(payment: Payment): Payment {
        logger.info("Inserting new payment to destination account: ${payment.destinationAccount}")
        val transactionCredit = Transaction()
        val transactionDebit = Transaction()

        // Process destination account - create if missing
        processPaymentAccount(payment.destinationAccount)

        // Process source account - create if missing
        processPaymentAccount(payment.sourceAccount)

        // Validate destination account type after ensuring it exists
        val destinationAccount = accountService.account(payment.destinationAccount).get()
        if (destinationAccount.accountType == AccountType.Debit) {
            logger.error("Account cannot make a payment to a debit account: ${payment.destinationAccount}")
            throw ValidationException("Account cannot make a payment to a debit account: ${payment.destinationAccount}")
        }

        val paymentAccountNameOwner = payment.sourceAccount
        populateCreditTransaction(transactionCredit, payment, paymentAccountNameOwner)
        populateDebitTransaction(transactionDebit, payment, paymentAccountNameOwner)

        logger.info("Creating debit and credit transactions for payment")

        // Create credit transaction using ServiceResult pattern
        val creditResult = transactionService.save(transactionCredit)
        when (creditResult) {
            is ServiceResult.Success -> {
                payment.guidDestination = creditResult.data.guid
                logger.debug("Credit transaction created successfully: ${creditResult.data.guid}")
            }
            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Credit transaction validation failed: ${creditResult.errors}", emptySet())
            }
            is ServiceResult.BusinessError -> {
                throw org.springframework.dao.DataIntegrityViolationException("Credit transaction business error: ${creditResult.message}")
            }
            else -> throw RuntimeException("Failed to create credit transaction: $creditResult")
        }

        // Create debit transaction using ServiceResult pattern
        val debitResult = transactionService.save(transactionDebit)
        when (debitResult) {
            is ServiceResult.Success -> {
                payment.guidSource = debitResult.data.guid
                logger.debug("Debit transaction created successfully: ${debitResult.data.guid}")
            }
            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Debit transaction validation failed: ${debitResult.errors}", emptySet())
            }
            is ServiceResult.BusinessError -> {
                throw org.springframework.dao.DataIntegrityViolationException("Debit transaction business error: ${debitResult.message}")
            }
            else -> throw RuntimeException("Failed to create debit transaction: $debitResult")
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

    // ===== Transaction Population Methods =====

    fun populateDebitTransaction(
        transactionDebit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        transactionDebit.guid = UUID.randomUUID().toString()
        transactionDebit.transactionDate = payment.transactionDate
        transactionDebit.description = "payment"
        transactionDebit.category = "bill_pay"
        transactionDebit.notes = "to ${payment.destinationAccount}"
        if (payment.amount > BigDecimal(0.0)) {
            transactionDebit.amount = payment.amount * BigDecimal(-1.0)
        } else {
            transactionDebit.amount = payment.amount
        }
        transactionDebit.transactionState = TransactionState.Outstanding
        transactionDebit.reoccurringType = ReoccurringType.Onetime
        transactionDebit.accountType = AccountType.Debit
        transactionDebit.accountNameOwner = paymentAccountNameOwner
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionDebit.dateUpdated = timestamp
        transactionDebit.dateAdded = timestamp
    }

    fun populateCreditTransaction(
        transactionCredit: finance.domain.Transaction,
        payment: Payment,
        paymentAccountNameOwner: String,
    ) {
        transactionCredit.guid = UUID.randomUUID().toString()
        transactionCredit.transactionDate = payment.transactionDate
        transactionCredit.description = "payment"
        transactionCredit.category = "bill_pay"
        transactionCredit.notes = "from $paymentAccountNameOwner"
        when {
            payment.amount > BigDecimal(0.0) -> {
                transactionCredit.amount = payment.amount * BigDecimal(-1.0)
            }
            else -> {
                transactionCredit.amount = payment.amount
            }
        }

        transactionCredit.transactionState = TransactionState.Outstanding
        transactionCredit.reoccurringType = ReoccurringType.Onetime
        transactionCredit.accountType = AccountType.Credit
        transactionCredit.accountNameOwner = payment.destinationAccount
        val timestamp = Timestamp(System.currentTimeMillis())
        transactionCredit.dateUpdated = timestamp
        transactionCredit.dateAdded = timestamp
    }
}
