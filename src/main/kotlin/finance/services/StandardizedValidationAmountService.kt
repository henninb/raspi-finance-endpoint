package finance.services

import finance.domain.ValidationAmount
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.repositories.ValidationAmountRepository
import finance.repositories.AccountRepository
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*

/**
 * Standardized ValidationAmount Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
@Primary
class StandardizedValidationAmountService(
    private val validationAmountRepository: ValidationAmountRepository,
    private val accountRepository: AccountRepository
) : StandardizedBaseService<ValidationAmount, Long>() {

    override fun getEntityName(): String = "ValidationAmount"

    // ===== New Standardized ServiceResult Methods =====

    override fun findAllActive(): ServiceResult<List<ValidationAmount>> {
        return handleServiceOperation("findAllActive", null) {
            validationAmountRepository.findByActiveStatusTrueOrderByValidationDateDesc()
        }
    }

    override fun findById(id: Long): ServiceResult<ValidationAmount> {
        return handleServiceOperation("findById", id) {
            val optionalValidationAmount = validationAmountRepository.findByValidationIdAndActiveStatusTrue(id)
            if (optionalValidationAmount.isPresent) {
                optionalValidationAmount.get()
            } else {
                throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found: $id")
            }
        }
    }

    override fun save(entity: ValidationAmount): ServiceResult<ValidationAmount> {
        return handleServiceOperation("save", entity.validationId) {
            val violations = validator.validate(entity)
            if (violations.isNotEmpty()) {
                throw jakarta.validation.ConstraintViolationException("Validation failed", violations)
            }

            // Set timestamps
            val timestamp = Timestamp(System.currentTimeMillis())
            entity.dateAdded = timestamp
            entity.dateUpdated = timestamp

            val saved = validationAmountRepository.saveAndFlush(entity)
            // Keep Account.validationDate in sync with newest ValidationAmount row
            try {
                accountRepository.updateValidationDateForAccount(saved.accountId)
            } catch (ex: Exception) {
                logger.warn("Failed to refresh account.validation_date for accountId=${saved.accountId}: ${ex.message}")
            }
            saved
        }
    }

    override fun update(entity: ValidationAmount): ServiceResult<ValidationAmount> {
        return handleServiceOperation("update", entity.validationId) {
            val existingValidationAmount = validationAmountRepository.findByValidationIdAndActiveStatusTrue(entity.validationId!!)
            if (existingValidationAmount.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found: ${entity.validationId}")
            }

            // Update fields from the provided entity
            val validationAmountToUpdate = existingValidationAmount.get()
            validationAmountToUpdate.accountId = entity.accountId
            validationAmountToUpdate.amount = entity.amount
            validationAmountToUpdate.transactionState = entity.transactionState
            validationAmountToUpdate.validationDate = entity.validationDate
            validationAmountToUpdate.activeStatus = entity.activeStatus
            validationAmountToUpdate.dateUpdated = Timestamp(System.currentTimeMillis())

            val updated = validationAmountRepository.saveAndFlush(validationAmountToUpdate)
            // Refresh Account.validationDate projection
            try {
                accountRepository.updateValidationDateForAccount(updated.accountId)
            } catch (ex: Exception) {
                logger.warn("Failed to refresh account.validation_date for accountId=${updated.accountId}: ${ex.message}")
            }
            updated
        }
    }

    override fun deleteById(id: Long): ServiceResult<Boolean> {
        return handleServiceOperation("deleteById", id) {
            val optionalValidationAmount = validationAmountRepository.findByValidationIdAndActiveStatusTrue(id)
            if (optionalValidationAmount.isEmpty) {
                throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found: $id")
            }
            validationAmountRepository.delete(optionalValidationAmount.get())
            true
        }
    }

    // ===== Legacy methods still needed by controller =====

    fun findValidationAmountByAccountNameOwner(
        accountNameOwner: String,
        transactionState: TransactionState
    ): ValidationAmount {
        // Find account by name owner
        val account = accountRepository.findByAccountNameOwner(accountNameOwner).orElseThrow {
            jakarta.persistence.EntityNotFoundException("Account not found: $accountNameOwner")
        }

        // Find validation amount by account ID and transaction state
        val validationAmounts = validationAmountRepository.findByTransactionStateAndAccountId(transactionState, account.accountId)
        if (validationAmounts.isEmpty()) {
            throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found for account: $accountNameOwner and transaction state: $transactionState")
        }
        // Return the LATEST validation amount by date (newest first)
        return validationAmounts.maxByOrNull { it.validationDate }
            ?: throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found for account: $accountNameOwner and transaction state: $transactionState")
    }

    fun insertValidationAmount(
        accountNameOwner: String,
        validationAmount: ValidationAmount
    ): ValidationAmount {
        logger.info("Inserting validation amount for account: $accountNameOwner")

        // Determine the target accountId using request payload first, then fallback to path variable
        val providedAccountId = validationAmount.accountId
        val resolvedAccountId: Long = when {
            providedAccountId > 0L -> {
                // Prefer explicit accountId from payload when valid
                providedAccountId
            }
            else -> {
                val byOwner = accountRepository.findByAccountNameOwner(accountNameOwner)
                if (byOwner.isPresent) {
                    byOwner.get().accountId
                } else {
                    logger.warn("Account not found by owner: $accountNameOwner and no valid accountId provided in payload")
                    0L
                }
            }
        }

        if (resolvedAccountId <= 0L) {
            throw org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Unable to resolve account for validation amount"
            )
        }

        // Set the resolved accountId
        validationAmount.accountId = resolvedAccountId

        // Use the standard save method and handle ServiceResult
        val result = save(validationAmount)
        return when (result) {
            is ServiceResult.Success -> result.data
            is ServiceResult.ValidationError -> {
                val violations = result.errors.map { (field, message) ->
                    object : jakarta.validation.ConstraintViolation<ValidationAmount> {
                        override fun getMessage(): String = message
                        override fun getMessageTemplate(): String = message
                        override fun getRootBean(): ValidationAmount = validationAmount
                        override fun getRootBeanClass(): Class<ValidationAmount> = ValidationAmount::class.java
                        override fun getLeafBean(): Any = validationAmount
                        override fun getExecutableParameters(): Array<Any> = emptyArray()
                        override fun getExecutableReturnValue(): Any? = null
                        override fun getPropertyPath(): jakarta.validation.Path {
                            return object : jakarta.validation.Path {
                                override fun toString(): String = field
                                override fun iterator(): MutableIterator<jakarta.validation.Path.Node> = mutableListOf<jakarta.validation.Path.Node>().iterator()
                            }
                        }
                        override fun getInvalidValue(): Any? = null
                        override fun getConstraintDescriptor(): jakarta.validation.metadata.ConstraintDescriptor<*>? = null
                        override fun <U : Any?> unwrap(type: Class<U>?): U = throw UnsupportedOperationException()
                    }
                }.toSet()
                throw ValidationException(jakarta.validation.ConstraintViolationException("Validation failed", violations))
            }
            else -> throw RuntimeException("Failed to insert validation amount: ${result}")
        }
    }
}
