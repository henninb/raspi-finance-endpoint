package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.Transfer
import finance.repositories.TransferRepository
import finance.utils.TenantContext
import jakarta.validation.Validator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
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

        override fun save(entity: Transfer): ServiceResult<Transfer> =
            handleServiceOperation("save", entity.transferId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                // Detect new transfers: if transferId is 0 AND guidSource/guidDestination are missing
                // then use the full insertTransfer workflow to create transactions
                val isNewTransfer = (entity.transferId == 0L)
                val needsTransactionCreation = (entity.guidSource.isNullOrBlank() || entity.guidDestination.isNullOrBlank())

                if (isNewTransfer && needsTransactionCreation) {
                    logger.info("Detected new transfer creation - delegating to insertTransfer workflow")
                    // Use insertTransfer for the full workflow (creates transactions, sets GUIDs)
                    // This will recursively call save() again, but with GUIDs populated
                    return@handleServiceOperation insertTransfer(entity)
                }

                // For updates or transfers that already have GUIDs, just save directly
                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                // Set timestamps
                val timestamp = Timestamp(System.currentTimeMillis())
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp

                transferRepository.save(entity)
            }

        override fun update(entity: Transfer): ServiceResult<Transfer> =
            handleServiceOperation("update", entity.transferId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                val existingTransfer = transferRepository.findByOwnerAndTransferId(owner, entity.transferId)
                if (existingTransfer.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transfer not found: ${entity.transferId}")
                }

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                // Update timestamp
                entity.dateUpdated = Timestamp(System.currentTimeMillis())

                transferRepository.save(entity)
            }

        override fun deleteById(id: Long): ServiceResult<Transfer> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransfer = transferRepository.findByOwnerAndTransferId(owner, id)
                if (optionalTransfer.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
                }
                val transfer = optionalTransfer.get()
                transferRepository.delete(transfer)
                transfer
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

            // Validate source account
            val optionalSourceAccount = accountService.account(transfer.sourceAccount)
            if (!optionalSourceAccount.isPresent) {
                logger.error("Source account not found: ${transfer.sourceAccount}")
                throw RuntimeException("Source account not found: ${transfer.sourceAccount}")
            }

            // Validate destination account
            val optionalDestinationAccount = accountService.account(transfer.destinationAccount)
            if (!optionalDestinationAccount.isPresent) {
                logger.error("Destination account not found: ${transfer.destinationAccount}")
                throw RuntimeException("Destination account not found: ${transfer.destinationAccount}")
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

            // Use the standardized save method and handle ServiceResult
            val result = save(transfer)
            return when (result) {
                is ServiceResult.Success -> {
                    result.data
                }

                is ServiceResult.ValidationError -> {
                    throw jakarta.validation.ConstraintViolationException("Validation failed: ${result.errors}", emptySet())
                }

                is ServiceResult.BusinessError -> {
                    // Handle data integrity violations (e.g., duplicate transfers)
                    throw org.springframework.dao.DataIntegrityViolationException(result.message)
                }

                else -> {
                    throw RuntimeException("Failed to insert transfer: $result")
                }
            }
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
            val owner = TenantContext.getCurrentOwner()
            val optionalTransfer = transferRepository.findByOwnerAndTransferId(owner, transferId)
            if (optionalTransfer.isPresent) {
                transferRepository.delete(optionalTransfer.get())
                return true
            }
            return false
        }

        // ===== Helper Methods for Transfer Processing =====

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
                this.accountType = accountType
                accountNameOwner = accountName
                dateUpdated = timestamp
                dateAdded = timestamp
            }
        }
    }
