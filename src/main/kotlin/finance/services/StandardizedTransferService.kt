package finance.services

import finance.domain.*
import finance.repositories.TransferRepository
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*

/**
 * Standardized Transfer Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@org.springframework.context.annotation.Primary
class StandardizedTransferService(
    private val transferRepository: TransferRepository,
    private val transactionService: TransactionService,
    private val accountService: AccountService
) : StandardizedBaseService<Transfer, Long>(), ITransferService {

    override fun getEntityName(): String = "Transfer"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<Transfer>> {
        return handleServiceOperation("findAllActive", null) {
            transferRepository.findAll().sortedByDescending { transfer -> transfer.transactionDate }
        }
    }

    override fun findById(id: Long): ServiceResult<Transfer> {
        return handleServiceOperation("findById", id) {
            val optionalTransfer = transferRepository.findByTransferId(id)
            if (optionalTransfer.isPresent) {
                optionalTransfer.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
            }
        }
    }

    override fun save(entity: Transfer): ServiceResult<Transfer> {
        return handleServiceOperation("save", entity.transferId) {
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
    }

    override fun update(entity: Transfer): ServiceResult<Transfer> {
        return handleServiceOperation("update", entity.transferId) {
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
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalTransfer = transferRepository.findByTransferId(id)
            if (optionalTransfer.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("Transfer not found: $id")
            }
            transferRepository.delete(optionalTransfer.get())
            true
        }
    }

    // ===== Legacy Method Compatibility =====

    override fun findAllTransfers(): List<Transfer> {
        val result = findAllActive()
        return when (result) {
            is ServiceResult.Success -> result.data
            else -> emptyList()
        }
    }

    override fun insertTransfer(transfer: Transfer): Transfer {
        logger.info("Inserting new transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")
        val transactionSource = Transaction()
        val transactionDestination = Transaction()

        // Validate transfer
        val constraintViolations = validator.validate(transfer)
        if (constraintViolations.isNotEmpty()) {
            throw jakarta.validation.ConstraintViolationException("Validation failed", constraintViolations)
        }

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

        // Save transactions and transfer
        transactionService.insertTransaction(transactionSource)
        transactionService.insertTransaction(transactionDestination)

        transfer.guidSource = transactionSource.guid
        transfer.guidDestination = transactionDestination.guid
        logger.info("Creating transfer from ${transfer.sourceAccount} to ${transfer.destinationAccount}")
        val timestamp = Timestamp(System.currentTimeMillis())
        transfer.dateUpdated = timestamp
        transfer.dateAdded = timestamp

        val savedTransfer = transferRepository.saveAndFlush(transfer)
        logger.info("Successfully created transfer with ID: ${savedTransfer.transferId}")
        return savedTransfer
    }

    override fun updateTransfer(transfer: Transfer): Transfer {
        val result = update(transfer)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw RuntimeException("Transfer not updated as the transfer does not exist: ${transfer.transferId}.")
            else -> throw RuntimeException("Failed to update transfer: ${result}")
        }
    }

    override fun findByTransferId(transferId: Long): Optional<Transfer> {
        return transferRepository.findByTransferId(transferId)
    }

    override fun deleteByTransferId(transferId: Long): Boolean {
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
        accountName: String
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
        accountName: String
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