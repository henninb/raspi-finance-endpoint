package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.domain.Transfer
import finance.repositories.TransferRepository
import finance.utils.TenantContext
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

/**
 * Standardized Transfer Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class TransferService
    constructor(
        private val transferRepository: TransferRepository,
        private val transactionService: TransactionService,
        private val accountService: AccountService,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<Transfer, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "Transfer"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<Transfer>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                transferRepository.findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner, true, Pageable.unpaged()).content
            }

        override fun findById(id: Long): ServiceResult<Transfer> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransfer = transferRepository.findByOwnerAndTransferId(owner, id)
                if (optionalTransfer.isPresent) {
                    optionalTransfer.get()
                } else {
                    throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
                }
            }

        @org.springframework.transaction.annotation.Transactional
        override fun save(entity: Transfer): ServiceResult<Transfer> {
            val owner = TenantContext.getCurrentOwner()
            entity.owner = owner

            if (entity.transferId == 0L) {
                // Route new transfers through insertTransfer.
                // Explicit try/catch with setRollbackOnly ensures the outer @Transactional
                // is rolled back even though handleServiceOperation would otherwise swallow
                // the exception (internal calls bypass the AOP proxy).
                entity.guidSource = null
                entity.guidDestination = null
                logger.info("Detected new transfer creation - delegating to insertTransfer workflow")
                return try {
                    ServiceResult.Success.of(insertTransfer(entity))
                } catch (ex: jakarta.validation.ConstraintViolationException) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                    val errors =
                        ex.constraintViolations.associate {
                            (it.propertyPath?.toString() ?: "unknown") to (it.message ?: "Validation failed")
                        }
                    ServiceResult.ValidationError.of(errors.ifEmpty { mapOf("validation" to "Validation failed") })
                } catch (ex: DataIntegrityViolationException) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                    ServiceResult.BusinessError.of(ex.message ?: "Data integrity error", "DATA_INTEGRITY_VIOLATION")
                } catch (ex: RuntimeException) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                    ServiceResult.SystemError.of(ex)
                }
            }

            return handleServiceOperation("save", entity.transferId) {
                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }
                val timestamp = Timestamp(System.currentTimeMillis())
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp
                transferRepository.save(entity)
            }
        }

        @org.springframework.transaction.annotation.Transactional
        override fun update(entity: Transfer): ServiceResult<Transfer> =
            handleServiceOperation("update", entity.transferId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                val existingTransfer = transferRepository.findByOwnerAndTransferId(owner, entity.transferId)
                if (existingTransfer.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transfer not found: ${entity.transferId}")
                }

                if (entity.sourceAccount == entity.destinationAccount) {
                    throw IllegalArgumentException("Source and destination accounts must be different: ${entity.sourceAccount}")
                }

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                val existing = existingTransfer.get()
                entity.guidSource = existing.guidSource
                entity.guidDestination = existing.guidDestination

                syncTransferTransactions(entity)

                entity.dateUpdated = Timestamp(System.currentTimeMillis())

                transferRepository.save(entity)
            }

        @Transactional
        override fun deleteById(id: Long): ServiceResult<Transfer> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransfer = transferRepository.findByOwnerAndTransferId(owner, id)
                if (optionalTransfer.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
                }
                val transfer = optionalTransfer.get()

                // Save GUIDs before removing the transfer
                val savedGuidSource = transfer.guidSource
                val savedGuidDestination = transfer.guidDestination

                // Step 1: Delete the transfer first to break FK references to t_transaction
                transferRepository.delete(transfer)
                transferRepository.flush()
                logger.info("Transfer deleted (flushed) to allow cascade transaction deletes: $id")

                // Step 2: Delete associated transactions (cascade delete)
                val transactionsDeleted = deleteAssociatedTransactions(savedGuidSource, savedGuidDestination)
                logger.info(
                    "Deleted $transactionsDeleted transaction(s) for transfer $id: " +
                        "source=$savedGuidSource, destination=$savedGuidDestination",
                )

                transfer
            }

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
                    logger.warn("$label transaction not found (stale reference): $guid")
                    0
                }

                is ServiceResult.BusinessError -> {
                    throw DataIntegrityViolationException(
                        "Cannot delete transfer because $label transaction $guid could not be deleted: ${result.message}",
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

        // ===== Paginated ServiceResult Methods =====

        /**
         * Find all active transfers with pagination.
         * Sorted by transactionDate descending.
         */
        fun findAllActive(pageable: Pageable): ServiceResult<Page<Transfer>> =
            handleServiceOperation("findAllActive-paginated", null) {
                val owner = TenantContext.getCurrentOwner()
                transferRepository.findByOwnerAndActiveStatusOrderByTransactionDateDesc(owner, true, pageable)
            }

        // ===== Legacy Method Compatibility =====

        fun findAllTransfers(): List<Transfer> {
            val result = findAllActive()
            return when (result) {
                is ServiceResult.Success -> result.data
                else -> emptyList()
            }
        }

        @org.springframework.transaction.annotation.Transactional
        fun insertTransfer(transfer: Transfer): Transfer {
            val owner = TenantContext.getCurrentOwner()
            transfer.owner = owner
            logger.info("Inserting new transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")

            if (transfer.sourceAccount == transfer.destinationAccount) {
                throw IllegalArgumentException("Source and destination accounts must be different: ${transfer.sourceAccount}")
            }

            // Validate source account exists and is a debit account
            val optionalSourceAccount = accountService.account(transfer.sourceAccount)
            if (!optionalSourceAccount.isPresent) {
                logger.error("Source account not found: ${transfer.sourceAccount}")
                throw RuntimeException("Source account not found: ${transfer.sourceAccount}")
            }
            val sourceAccount = optionalSourceAccount.get()
            if (!sourceAccount.accountType.isAsset) {
                logger.error("Source account is not an asset account: ${transfer.sourceAccount} (type: ${sourceAccount.accountType})")
                throw IllegalArgumentException("Source account must be an asset account: ${transfer.sourceAccount} (type: ${sourceAccount.accountType})")
            }

            // Validate destination account exists and is an asset account
            val optionalDestinationAccount = accountService.account(transfer.destinationAccount)
            if (!optionalDestinationAccount.isPresent) {
                logger.error("Destination account not found: ${transfer.destinationAccount}")
                throw RuntimeException("Destination account not found: ${transfer.destinationAccount}")
            }
            val destinationAccount = optionalDestinationAccount.get()
            if (!destinationAccount.accountType.isAsset) {
                logger.error("Destination account is not an asset account: ${transfer.destinationAccount} (type: ${destinationAccount.accountType})")
                throw IllegalArgumentException("Destination account must be an asset account: ${transfer.destinationAccount} (type: ${destinationAccount.accountType})")
            }

            logger.info("Creating source and destination transactions for transfer")
            val transactionSource =
                buildTransferTransaction(
                    transfer = transfer,
                    accountName = transfer.sourceAccount,
                    description = "transfer withdrawal",
                    notes = "Transfer to ${transfer.destinationAccount}",
                    amount = transfer.amount.negate(),
                    accountType = AccountType.Debit,
                )
            val transactionDestination =
                buildTransferTransaction(
                    transfer = transfer,
                    accountName = transfer.destinationAccount,
                    description = "transfer deposit",
                    notes = "Transfer from ${transfer.sourceAccount}",
                    amount = transfer.amount,
                    accountType = AccountType.Credit,
                )

            // Create source transaction using ServiceResult pattern
            val sourceResult = transactionService.save(transactionSource)
            when (sourceResult) {
                is ServiceResult.Success -> {
                    transfer.guidSource = sourceResult.data.guid
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

            // Create destination transaction using ServiceResult pattern
            val destinationResult = transactionService.save(transactionDestination)
            when (destinationResult) {
                is ServiceResult.Success -> {
                    transfer.guidDestination = destinationResult.data.guid
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

            // Persist the transfer record directly — do NOT call save() to avoid re-entering
            // the new-transfer detection logic (save routes transferId=0 back here).
            val violations = validator.validate(transfer)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Transfer validation failed", violations)
            }
            val timestamp = Timestamp(System.currentTimeMillis())
            transfer.dateAdded = timestamp
            transfer.dateUpdated = timestamp
            return transferRepository.save(transfer)
        }

        fun updateTransfer(transfer: Transfer): Transfer {
            val result = update(transfer)
            return when (result) {
                is ServiceResult.Success -> result.data
                is ServiceResult.NotFound -> throw RuntimeException("Transfer not updated as the transfer does not exist: ${transfer.transferId}.")
                else -> throw RuntimeException("Failed to update transfer: $result")
            }
        }

        fun findByTransferId(transferId: Long): Optional<Transfer> {
            val owner = TenantContext.getCurrentOwner()
            return transferRepository.findByOwnerAndTransferId(owner, transferId)
        }

        fun deleteByTransferId(transferId: Long): Boolean {
            val result = deleteById(transferId)
            return when (result) {
                is ServiceResult.Success -> true
                is ServiceResult.NotFound -> false
                is ServiceResult.SystemError -> throw result.exception
                else -> throw RuntimeException("Failed to delete transfer $transferId: $result")
            }
        }

        // ===== Helper Methods for Transfer Processing =====

        private fun syncTransferTransactions(transfer: Transfer) {
            val guidSource = transfer.guidSource
            val guidDestination = transfer.guidDestination
            if (guidSource.isNullOrBlank() || guidDestination.isNullOrBlank()) {
                logger.warn("Cannot sync transfer transactions: missing GUIDs on transfer ${transfer.transferId}")
                return
            }

            val sourceResult = transactionService.findById(guidSource)
            if (sourceResult !is ServiceResult.Success) {
                throw IllegalStateException("Source transaction not found for transfer ${transfer.transferId}: $guidSource")
            }
            val sourceTx = sourceResult.data
            sourceTx.amount = transfer.amount.negate()
            sourceTx.transactionDate = transfer.transactionDate
            sourceTx.accountNameOwner = transfer.sourceAccount
            sourceTx.notes = "Transfer to ${transfer.destinationAccount}"
            val sourceUpdateResult = transactionService.update(sourceTx)
            if (sourceUpdateResult !is ServiceResult.Success) {
                throw IllegalStateException("Failed to sync source transaction $guidSource: $sourceUpdateResult")
            }

            val destResult = transactionService.findById(guidDestination)
            if (destResult !is ServiceResult.Success) {
                throw IllegalStateException("Destination transaction not found for transfer ${transfer.transferId}: $guidDestination")
            }
            val destTx = destResult.data
            destTx.amount = transfer.amount
            destTx.transactionDate = transfer.transactionDate
            destTx.accountNameOwner = transfer.destinationAccount
            destTx.notes = "Transfer from ${transfer.sourceAccount}"
            val destUpdateResult = transactionService.update(destTx)
            if (destUpdateResult !is ServiceResult.Success) {
                throw IllegalStateException("Failed to sync destination transaction $guidDestination: $destUpdateResult")
            }
        }

        private fun buildTransferTransaction(
            transfer: Transfer,
            accountName: String,
            description: String,
            notes: String,
            amount: BigDecimal,
            accountType: AccountType,
        ): Transaction {
            val timestamp = nowTimestamp()
            return Transaction().apply {
                guid = UUID.randomUUID().toString()
                transactionDate = transfer.transactionDate
                this.description = description
                category = "transfer"
                this.notes = notes
                this.amount = amount
                transactionState = TransactionState.Outstanding
                reoccurringType = ReoccurringType.Onetime
                transactionType = TransactionType.Transfer
                this.accountType = accountType
                accountNameOwner = accountName
                dateUpdated = timestamp
                dateAdded = timestamp
            }
        }
    }
