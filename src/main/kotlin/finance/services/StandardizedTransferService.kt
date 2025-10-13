package finance.services

import finance.domain.AccountType
import finance.domain.ReoccurringType
import finance.domain.ServiceResult
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.Transfer
import finance.repositories.TransferRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

/**
 * Standardized Transfer Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
class StandardizedTransferService(
    private val transferRepository: TransferRepository,
    private val transactionService: StandardizedTransactionService,
    private val accountService: StandardizedAccountService,
) : StandardizedBaseService<Transfer, Long>() {
    override fun getEntityName(): String = "Transfer"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Transfer>> =
        handleServiceOperation("findAllActive", null) {
            transferRepository.findAll().sortedByDescending { transfer -> transfer.transactionDate }
        }

    override fun findById(id: Long): ServiceResult<Transfer> =
        handleServiceOperation("findById", id) {
            val optionalTransfer = transferRepository.findByTransferId(id)
            if (optionalTransfer.isPresent) {
                optionalTransfer.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
            }
        }

    override fun save(entity: Transfer): ServiceResult<Transfer> =
        handleServiceOperation("save", entity.transferId) {
            // Detect new transfers: if transferId is 0 or null AND guidSource/guidDestination are missing
            // then use the full insertTransfer workflow to create transactions
            val isNewTransfer = (entity.transferId == null || entity.transferId == 0L)
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
            val existingTransfer = transferRepository.findByTransferId(entity.transferId!!)
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

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val optionalTransfer = transferRepository.findByTransferId(id)
            if (optionalTransfer.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
            }
            transferRepository.delete(optionalTransfer.get())
            true
        }

    // ===== Legacy Method Compatibility =====

    fun findAllTransfers(): List<Transfer> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    fun insertTransfer(transfer: Transfer): Transfer {
        logger.info("Inserting new transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")
        val transactionSource = Transaction()
        val transactionDestination = Transaction()

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

        // Populate source and destination transactions
        populateSourceTransaction(transactionSource, transfer, transfer.sourceAccount)
        populateDestinationTransaction(transactionDestination, transfer, transfer.destinationAccount)

        logger.info("Creating source and destination transactions for transfer")

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
            else -> throw RuntimeException("Failed to create source transaction: $sourceResult")
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
            else -> throw RuntimeException("Failed to create destination transaction: $destinationResult")
        }

        // Use the standardized save method and handle ServiceResult
        val result = save(transfer)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                throw jakarta.validation.ConstraintViolationException("Validation failed: ${result.errors}", emptySet())
            }
            is ServiceResult.BusinessError -> {
                // Handle data integrity violations (e.g., duplicate transfers)
                throw org.springframework.dao.DataIntegrityViolationException(result.message)
            }
            else -> throw RuntimeException("Failed to insert transfer: $result")
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

    fun findByTransferId(transferId: Long): Optional<Transfer> = transferRepository.findByTransferId(transferId)

    fun deleteByTransferId(transferId: Long): Boolean {
        val optionalTransfer = transferRepository.findByTransferId(transferId)
        if (optionalTransfer.isPresent) {
            transferRepository.delete(optionalTransfer.get())
            return true
        }
        return false
    }

    // ===== Helper Methods for Transfer Processing =====

    private fun populateSourceTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String,
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer withdrawal"
        transaction.category = "transfer"
        transaction.notes = "Transfer to ${transfer.destinationAccount}"
        transaction.amount = transfer.amount.negate()
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Debit
        transaction.accountNameOwner = accountName
        val timestamp = Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = timestamp
        transaction.dateAdded = timestamp
    }

    private fun populateDestinationTransaction(
        transaction: Transaction,
        transfer: Transfer,
        accountName: String,
    ) {
        transaction.guid = UUID.randomUUID().toString()
        transaction.transactionDate = transfer.transactionDate
        transaction.description = "transfer deposit"
        transaction.category = "transfer"
        transaction.notes = "Transfer from ${transfer.sourceAccount}"
        transaction.amount = transfer.amount
        transaction.transactionState = TransactionState.Outstanding
        transaction.reoccurringType = ReoccurringType.Onetime
        transaction.accountType = AccountType.Credit
        transaction.accountNameOwner = accountName
        val timestamp = Timestamp(System.currentTimeMillis())
        transaction.dateUpdated = timestamp
        transaction.dateAdded = timestamp
    }
}
