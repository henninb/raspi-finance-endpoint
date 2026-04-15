package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.PendingTransaction
import finance.domain.ServiceResult
import finance.repositories.PendingTransactionRepository
import finance.utils.TenantContext
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.util.Calendar
import java.util.Optional

/**
 * Standardized PendingTransaction Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class PendingTransactionService
    constructor(
        private val pendingTransactionRepository: PendingTransactionRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<PendingTransaction, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "PendingTransaction"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<PendingTransaction>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                pendingTransactionRepository.findAllByOwner(owner)
            }

        override fun findById(id: Long): ServiceResult<PendingTransaction> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalPendingTransaction = pendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(owner, id)
                if (optionalPendingTransaction.isPresent) {
                    optionalPendingTransaction.get()
                } else {
                    throw jakarta.persistence.EntityNotFoundException("PendingTransaction not found: $id")
                }
            }

        override fun save(entity: PendingTransaction): ServiceResult<PendingTransaction> =
            handleServiceOperation("save", entity.pendingTransactionId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner

                val violations = validator.validate(entity)
                if (violations.isNotEmpty()) {
                    throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
                }

                // Set dateAdded for new transactions (dateAdded has default value in constructor)
                entity.dateAdded = Timestamp(Calendar.getInstance().time.time)

                pendingTransactionRepository.saveAndFlush(entity)
            }

        override fun update(entity: PendingTransaction): ServiceResult<PendingTransaction> =
            handleServiceOperation("update", entity.pendingTransactionId) {
                val owner = TenantContext.getCurrentOwner()
                val existingTransaction = pendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(owner, entity.pendingTransactionId)
                if (existingTransaction.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("PendingTransaction not found: ${entity.pendingTransactionId}")
                }

                // Update fields from the provided entity
                val transactionToUpdate = existingTransaction.get()
                transactionToUpdate.accountNameOwner = entity.accountNameOwner
                transactionToUpdate.transactionDate = entity.transactionDate
                transactionToUpdate.description = entity.description
                transactionToUpdate.amount = entity.amount
                transactionToUpdate.reviewStatus = entity.reviewStatus

                pendingTransactionRepository.saveAndFlush(transactionToUpdate)
            }

        @Transactional
        override fun deleteById(id: Long): ServiceResult<PendingTransaction> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val optionalTransaction = pendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(owner, id)
                if (optionalTransaction.isEmpty) {
                    throw jakarta.persistence.EntityNotFoundException("PendingTransaction not found: $id")
                }
                val pendingTransaction = optionalTransaction.get()
                pendingTransactionRepository.delete(pendingTransaction)
                pendingTransaction
            }

        // ===== Business-Specific ServiceResult Methods =====

        @Transactional
        fun deleteAll(): ServiceResult<Boolean> =
            handleServiceOperation("deleteAll", null) {
                val owner = TenantContext.getCurrentOwner()
                pendingTransactionRepository.deleteAllByOwner(owner)
                true
            }

        // ===== Legacy Method Compatibility (Deprecated - Use ServiceResult methods instead) =====

        @Deprecated("Use save() with ServiceResult instead", ReplaceWith("save(pendingTransaction)"))
        fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction =
            when (val result = save(pendingTransaction)) {
                is ServiceResult.Success -> {
                    result.data
                }

                is ServiceResult.ValidationError -> {
                    throw ValidationException("Validation failed: ${result.errors}")
                }

                is ServiceResult.BusinessError -> {
                    throw RuntimeException("Business error inserting pending transaction: ${result.message}")
                }

                is ServiceResult.NotFound -> {
                    throw jakarta.persistence.EntityNotFoundException("PendingTransaction not found")
                }

                is ServiceResult.SystemError -> {
                    throw RuntimeException("Failed to insert pending transaction", result.exception)
                }
            }

        @Deprecated("Use deleteById() with ServiceResult instead", ReplaceWith("deleteById(pendingTransactionId)"))
        fun deletePendingTransaction(pendingTransactionId: Long): Boolean {
            val result = deleteById(pendingTransactionId)
            return when (result) {
                is ServiceResult.Success -> true
                is ServiceResult.NotFound -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "PendingTransaction not found: $pendingTransactionId")
                is ServiceResult.ValidationError -> throw RuntimeException("Validation error deleting pending transaction: ${result.errors}")
                is ServiceResult.BusinessError -> throw RuntimeException("Business error deleting pending transaction: ${result.message}")
                is ServiceResult.SystemError -> throw RuntimeException("Failed to delete pending transaction", result.exception)
            }
        }

        @Deprecated("Use findAllActive() with ServiceResult instead", ReplaceWith("findAllActive()"))
        fun getAllPendingTransactions(): List<PendingTransaction> {
            val result = findAllActive()
            return when (result) {
                is ServiceResult.Success -> result.data
                else -> emptyList()
            }
        }

        @Deprecated("Use deleteAll() with ServiceResult instead", ReplaceWith("deleteAll()"))
        fun deleteAllPendingTransactions(): Boolean {
            val result = deleteAll()
            return when (result) {
                is ServiceResult.Success -> result.data
                else -> throw RuntimeException("Failed to delete all pending transactions: $result")
            }
        }

        @Deprecated("Use findById() with ServiceResult instead", ReplaceWith("findById(pendingTransactionId)"))
        fun findByPendingTransactionId(pendingTransactionId: Long): Optional<PendingTransaction> {
            val result = findById(pendingTransactionId)
            return when (result) {
                is ServiceResult.Success -> Optional.of(result.data)
                else -> Optional.empty()
            }
        }

        @Deprecated("Use update() with ServiceResult instead", ReplaceWith("update(pendingTransaction)"))
        fun updatePendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
            val result = update(pendingTransaction)
            return when (result) {
                is ServiceResult.Success -> result.data
                is ServiceResult.NotFound -> throw RuntimeException("PendingTransaction not found: ${pendingTransaction.pendingTransactionId}")
                else -> throw RuntimeException("Failed to update pending transaction: $result")
            }
        }
    }
