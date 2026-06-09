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
import finance.domain.TransactionType
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
        resilienceComponents: ResilienceComponents,
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
                // #10: owner must be populated before we write any data
                check(owner.isNotBlank()) { "Tenant owner context is empty — cannot save payment" }
                entity.owner = owner

                if (entity.sourceAccount == entity.destinationAccount) {
                    throw IllegalStateException("Source and destination accounts must differ: ${entity.sourceAccount}")
                }

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                // #7: GUIDs must be all-present or all-absent; a partial state means the
                // linked transactions are out of sync with this payment record.
                val hasGuidSource = !entity.guidSource.isNullOrBlank()
                val hasGuidDest = !entity.guidDestination.isNullOrBlank()
                check(hasGuidSource == hasGuidDest) {
                    "guidSource and guidDestination must both be set or both be absent — partial GUID state detected"
                }

                // If GUIDs are not set, we need to create transactions first
                // This prevents foreign key constraint violations
                if (!hasGuidSource && !hasGuidDest) {
                    logger.info("Creating transactions for payment: ${entity.sourceAccount} -> ${entity.destinationAccount}")

                    // Fail fast if either account does not exist — auto-creating with a wrong type
                    // would silently corrupt behavior inference downstream
                    val destinationAccount = requirePaymentAccount(entity.destinationAccount)
                    val sourceAccount = requirePaymentAccount(entity.sourceAccount)

                    // Infer payment behavior from account types; UNDEFINED means an unsupported
                    // account-type combination — fail early rather than produce wrong amounts
                    val behavior =
                        PaymentBehavior.inferBehavior(
                            sourceAccount.accountType,
                            destinationAccount.accountType,
                        )
                    if (behavior == PaymentBehavior.UNDEFINED) {
                        throw IllegalStateException(
                            "Cannot create payment: unsupported account type combination " +
                                "${sourceAccount.accountType} -> ${destinationAccount.accountType}",
                        )
                    }
                    logger.info("Payment behavior inferred: $behavior (${sourceAccount.accountType} -> ${destinationAccount.accountType})")

                    // Create destination transaction
                    val transactionDestination = buildDestinationTransaction(entity, entity.sourceAccount, destinationAccount.accountType, behavior)
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

                    // #1: Create source transaction; compensate by deleting the destination
                    // transaction if source creation fails so we don't leave an orphan.
                    val transactionSource = buildSourceTransaction(entity, entity.sourceAccount, sourceAccount.accountType, behavior)
                    val sourceResult = transactionService.save(transactionSource)
                    when (sourceResult) {
                        is ServiceResult.Success -> {
                            entity.guidSource = sourceResult.data.guid
                            logger.debug("Source transaction created: ${sourceResult.data.guid}")
                        }

                        is ServiceResult.ValidationError -> {
                            compensateDestinationTransaction(entity)
                            throw jakarta.validation.ConstraintViolationException("Source transaction validation failed: ${sourceResult.errors}", emptySet())
                        }

                        is ServiceResult.BusinessError -> {
                            compensateDestinationTransaction(entity)
                            throw org.springframework.dao.DataIntegrityViolationException("Source transaction business error: ${sourceResult.message}")
                        }

                        is ServiceResult.NotFound -> {
                            compensateDestinationTransaction(entity)
                            throw RuntimeException("Unexpected not-found saving source transaction: ${sourceResult.message}")
                        }

                        is ServiceResult.SystemError -> {
                            compensateDestinationTransaction(entity)
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
                // #10: owner must be present before we touch any data
                check(owner.isNotBlank()) { "Tenant owner context is empty — cannot update payment" }

                val existingPayment = paymentRepository.findByOwnerAndPaymentId(owner, entity.paymentId)
                if (existingPayment.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Payment not found: ${entity.paymentId}")
                }

                val paymentToUpdate = existingPayment.get()

                // #3: If either account is being changed, verify the new combination still
                // maps to a supported PaymentBehavior before we commit the update.
                if (entity.sourceAccount != paymentToUpdate.sourceAccount ||
                    entity.destinationAccount != paymentToUpdate.destinationAccount
                ) {
                    val srcOpt = accountService.account(entity.sourceAccount)
                    val dstOpt = accountService.account(entity.destinationAccount)
                    if (srcOpt.isEmpty || dstOpt.isEmpty) {
                        throw IllegalStateException(
                            "Cannot update payment: one or both accounts not found " +
                                "(source=${entity.sourceAccount}, destination=${entity.destinationAccount})",
                        )
                    }
                    val behavior = PaymentBehavior.inferBehavior(srcOpt.get().accountType, dstOpt.get().accountType)
                    if (behavior == PaymentBehavior.UNDEFINED) {
                        throw IllegalStateException(
                            "Cannot update payment: unsupported account type combination " +
                                "${srcOpt.get().accountType} -> ${dstOpt.get().accountType}",
                        )
                    }
                }

                paymentToUpdate.sourceAccount = entity.sourceAccount
                paymentToUpdate.destinationAccount = entity.destinationAccount
                paymentToUpdate.amount = entity.amount
                paymentToUpdate.transactionDate = entity.transactionDate
                paymentToUpdate.activeStatus = entity.activeStatus
                // GUIDs are system-managed — never overwrite them from the incoming payload
                // to avoid orphaning the associated transactions
                paymentToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

                val violations = validator.validate(paymentToUpdate)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                paymentRepository.saveAndFlush(paymentToUpdate)
            }

        @Transactional
        override fun deleteById(id: Long): ServiceResult<Payment> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                // #10: owner must be present before we touch any data
                check(owner.isNotBlank()) { "Tenant owner context is empty — cannot delete payment" }
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
         * #1: Compensation helper — deletes an already-persisted destination transaction
         * when source transaction creation fails, preventing orphaned records.
         */
        private fun compensateDestinationTransaction(payment: Payment) {
            val guid = payment.guidDestination
            if (!guid.isNullOrBlank()) {
                logger.warn("Compensating: deleting orphaned destination transaction $guid due to source creation failure")
                try {
                    transactionService.deleteByIdInternal(guid)
                } catch (e: Exception) {
                    logger.error("Compensation failed: could not delete orphaned destination transaction $guid", e)
                }
                payment.guidDestination = null
            }
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
            if (!guidSource.isNullOrBlank()) deletedCount += deleteTransactionByGuid(guidSource, "source")
            if (!guidDestination.isNullOrBlank()) deletedCount += deleteTransactionByGuid(guidDestination, "destination")
            return deletedCount
        }

        private fun deleteTransactionByGuid(
            guid: String,
            label: String,
        ): Int =
            when (val result = transactionService.deleteByIdInternal(guid)) {
                is ServiceResult.Success -> {
                    logger.info("Deleted $label transaction: $guid")
                    1
                }

                is ServiceResult.NotFound -> {
                    logger.warn("$label transaction not found: $guid")
                    0
                }

                is ServiceResult.BusinessError -> {
                    throw DataIntegrityViolationException(
                        "Cannot delete payment because $label transaction $guid could not be deleted: ${result.message}",
                    )
                }

                is ServiceResult.ValidationError -> {
                    throw DataIntegrityViolationException(
                        "Validation error deleting $label transaction $guid",
                    )
                }

                is ServiceResult.SystemError -> {
                    throw RuntimeException(
                        "System error deleting $label transaction: $guid",
                        result.exception,
                    )
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

        /** Resolves the account by name; throws if it does not exist so callers get a clear error. */
        private fun requirePaymentAccount(accountNameOwner: String): Account {
            logger.debug("Resolving payment account: $accountNameOwner")
            val accountOptional = accountService.account(accountNameOwner)
            if (accountOptional.isPresent) {
                logger.info("Using existing account for payment: $accountNameOwner (accountId: ${accountOptional.get().accountId})")
                return accountOptional.get()
            }
            logger.error("Account not found for payment: $accountNameOwner")
            throw IllegalStateException("Account not found: $accountNameOwner")
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
                PaymentBehavior.UNDEFINED -> throw IllegalStateException("calculateSourceAmount called with UNDEFINED behavior")
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
                PaymentBehavior.UNDEFINED -> throw IllegalStateException("calculateDestinationAmount called with UNDEFINED behavior")
            }

        // ===== Transaction Factory Methods =====

        private fun buildSourceTransaction(
            payment: Payment,
            sourceAccountNameOwner: String,
            sourceAccountType: AccountType,
            behavior: PaymentBehavior,
        ): Transaction {
            val timestamp = nowTimestamp()
            return Transaction().apply {
                guid = UUID.randomUUID().toString()
                transactionDate = payment.transactionDate
                description = "payment"
                category = behavior.category
                notes = "to ${payment.destinationAccount}"
                amount = calculateSourceAmount(payment.amount, behavior)
                transactionState = TransactionState.Outstanding
                reoccurringType = ReoccurringType.Onetime
                transactionType = TransactionType.Transfer
                accountType = sourceAccountType
                accountNameOwner = sourceAccountNameOwner
                dateUpdated = timestamp
                dateAdded = timestamp
            }
        }

        private fun buildDestinationTransaction(
            payment: Payment,
            sourceAccountNameOwner: String,
            destinationAccountType: AccountType,
            behavior: PaymentBehavior,
        ): Transaction {
            val timestamp = nowTimestamp()
            return Transaction().apply {
                guid = UUID.randomUUID().toString()
                transactionDate = payment.transactionDate
                description = "payment"
                category = behavior.category
                notes = "from $sourceAccountNameOwner"
                amount = calculateDestinationAmount(payment.amount, behavior)
                transactionState = TransactionState.Outstanding
                reoccurringType = ReoccurringType.Onetime
                transactionType = TransactionType.Transfer
                accountType = destinationAccountType
                accountNameOwner = payment.destinationAccount
                dateUpdated = timestamp
                dateAdded = timestamp
            }
        }
    }
