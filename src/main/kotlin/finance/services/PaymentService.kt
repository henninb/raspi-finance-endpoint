package finance.services

import finance.configurations.ResilienceComponents
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
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

@Service
class PaymentService
    constructor(
        private val paymentRepository: PaymentRepository,
        private val transactionService: TransactionService,
        private val accountService: AccountService,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents? = null,
    ) : CrudBaseService<Payment, Long>(meterService, validator, resilienceComponents) {
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

        @Transactional
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
                    val sourceAccount =
                        accountService
                            .account(entity.sourceAccount)
                            .orElseThrow { jakarta.persistence.EntityNotFoundException("Source account not found after creation: ${entity.sourceAccount}") }
                    val destinationAccount =
                        accountService
                            .account(entity.destinationAccount)
                            .orElseThrow { jakarta.persistence.EntityNotFoundException("Destination account not found after creation: ${entity.destinationAccount}") }

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

                        is ServiceResult.NotFound -> {
                            throw RuntimeException("Unexpected not-found saving destination transaction: ${destinationResult.message}")
                        }

                        is ServiceResult.SystemError -> {
                            throw destinationResult.exception
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

                        is ServiceResult.NotFound -> {
                            throw RuntimeException("Unexpected not-found saving source transaction: ${sourceResult.message}")
                        }

                        is ServiceResult.SystemError -> {
                            throw sourceResult.exception
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
        override fun deleteById(id: Long): ServiceResult<Payment> =
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

                payment
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
                        throw DataIntegrityViolationException(
                            "Cannot delete payment because source transaction $guidSource could not be deleted: ${result.message}",
                        )
                    }

                    is ServiceResult.ValidationError -> {
                        throw DataIntegrityViolationException("Validation error deleting source transaction $guidSource")
                    }

                    is ServiceResult.SystemError -> {
                        throw RuntimeException("System error deleting source transaction: $guidSource", result.exception)
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
                        throw DataIntegrityViolationException(
                            "Cannot delete payment because destination transaction $guidDestination could not be deleted: ${result.message}",
                        )
                    }

                    is ServiceResult.ValidationError -> {
                        throw DataIntegrityViolationException("Validation error deleting destination transaction $guidDestination")
                    }

                    is ServiceResult.SystemError -> {
                        throw RuntimeException("System error deleting destination transaction: $guidDestination", result.exception)
                    }
                }
            }

            return deletedCount
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
                is ServiceResult.ValidationError -> throw RuntimeException("Validation error updating payment $paymentId: ${result.errors}")
                is ServiceResult.BusinessError -> throw RuntimeException("Business error updating payment $paymentId: ${result.message}")
                is ServiceResult.SystemError -> throw result.exception
            }
        }

        fun findByPaymentId(paymentId: Long): Optional<Payment> {
            val owner = TenantContext.getCurrentOwner()
            return paymentRepository.findByOwnerAndPaymentId(owner, paymentId)
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

        private fun populateSourceTransaction(
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

        private fun populateDestinationTransaction(
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
    }
