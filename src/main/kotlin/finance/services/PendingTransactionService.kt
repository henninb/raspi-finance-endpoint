package finance.services

import finance.domain.PendingTransaction
import finance.domain.ServiceResult
import finance.repositories.PendingTransactionRepository
import finance.utils.TenantContext
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.util.Calendar
import java.util.Optional

/**
 * Standardized PendingTransaction Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class PendingTransactionService(
    private val pendingTransactionRepository: PendingTransactionRepository,
) : CrudBaseService<PendingTransaction, Long>() {
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

    override fun deleteById(id: Long): ServiceResult<Boolean> =
        handleServiceOperation("deleteById", id) {
            val owner = TenantContext.getCurrentOwner()
            val optionalTransaction = pendingTransactionRepository.findByOwnerAndPendingTransactionIdOrderByTransactionDateDesc(owner, id)
            if (optionalTransaction.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("PendingTransaction not found: $id")
            }
            pendingTransactionRepository.delete(optionalTransaction.get())
            true
        }

    // ===== Business-Specific ServiceResult Methods =====

    fun deleteAll(): ServiceResult<Boolean> =
        handleServiceOperation("deleteAll", null) {
            val owner = TenantContext.getCurrentOwner()
            pendingTransactionRepository.deleteAllByOwner(owner)
            true
        }

    // ===== Legacy Method Compatibility (Deprecated - Use ServiceResult methods instead) =====

    @Deprecated("Use save() with ServiceResult instead", ReplaceWith("save(pendingTransaction)"))
    fun insertPendingTransaction(pendingTransaction: PendingTransaction): PendingTransaction {
        val result = save(pendingTransaction)
        return when (result) {
            is ServiceResult.Success -> {
                result.data
            }

            is ServiceResult.ValidationError -> {
                val violations =
                    result.errors
                        .map { (field, message) ->
                            object : jakarta.validation.ConstraintViolation<PendingTransaction> {
                                override fun getMessage(): String = message

                                override fun getMessageTemplate(): String = message

                                override fun getRootBean(): PendingTransaction = pendingTransaction

                                override fun getRootBeanClass(): Class<PendingTransaction> = PendingTransaction::class.java

                                override fun getLeafBean(): Any = pendingTransaction

                                override fun getExecutableParameters(): Array<Any> = emptyArray()

                                override fun getExecutableReturnValue(): Any? = null

                                override fun getPropertyPath(): jakarta.validation.Path =
                                    object : jakarta.validation.Path {
                                        override fun toString(): String = field

                                        override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                                    }

                                override fun getInvalidValue(): Any? = null

                                override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null

                                override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                            }
                        }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }

            else -> {
                throw RuntimeException("Failed to insert pending transaction: $result")
            }
        }
    }

    @Deprecated("Use deleteById() with ServiceResult instead", ReplaceWith("deleteById(pendingTransactionId)"))
    fun deletePendingTransaction(pendingTransactionId: Long): Boolean {
        val result = deleteById(pendingTransactionId)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.NotFound -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "PendingTransaction not found: $pendingTransactionId")
            else -> throw RuntimeException("Failed to delete pending transaction: $result")
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
