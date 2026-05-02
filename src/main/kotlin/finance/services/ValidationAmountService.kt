package finance.services

import finance.configurations.ResilienceComponents
import finance.domain.ServiceResult
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import finance.utils.TenantContext
import finance.utils.orThrowNotFound
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.springframework.stereotype.Service

/**
 * Standardized ValidationAmount Service implementing ServiceResult pattern
 * Provides both new standardized methods and legacy compatibility
 */
@Service
class ValidationAmountService
    constructor(
        private val validationAmountRepository: ValidationAmountRepository,
        private val accountRepository: AccountRepository,
        meterService: MeterService,
        validator: Validator,
        resilienceComponents: ResilienceComponents,
    ) : CrudBaseService<ValidationAmount, Long>(meterService, validator, resilienceComponents) {
        override fun getEntityName(): String = "ValidationAmount"

        // ===== New Standardized ServiceResult Methods =====

        override fun findAllActive(): ServiceResult<List<ValidationAmount>> =
            handleServiceOperation("findAllActive", null) {
                val owner = TenantContext.getCurrentOwner()
                validationAmountRepository.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(owner)
            }

        /**
         * Find active validation amounts with optional filtering
         * @param accountNameOwner Optional account name filter
         * @param transactionState Optional transaction state filter
         * @return ServiceResult containing filtered list of validation amounts
         */
        fun findAllActiveFiltered(
            accountNameOwner: String? = null,
            transactionState: TransactionState? = null,
        ): ServiceResult<List<ValidationAmount>> =
            handleServiceOperation("findAllActiveFiltered", null) {
                val owner = TenantContext.getCurrentOwner()
                val allActive = validationAmountRepository.findByOwnerAndActiveStatusTrueOrderByValidationDateDesc(owner)

                // Apply filters if provided
                var filtered = allActive

                if (accountNameOwner != null) {
                    val account = accountRepository.findByOwnerAndAccountNameOwner(owner, accountNameOwner)
                    if (account.isPresent) {
                        filtered = filtered.filter { it.accountId == account.get().accountId }
                    } else {
                        // Account not found - return empty list
                        return@handleServiceOperation emptyList()
                    }
                }

                if (transactionState != null) {
                    filtered = filtered.filter { it.transactionState == transactionState }
                }

                filtered
            }

        override fun findById(id: Long): ServiceResult<ValidationAmount> =
            handleServiceOperation("findById", id) {
                val owner = TenantContext.getCurrentOwner()
                validationAmountRepository
                    .findByOwnerAndValidationIdAndActiveStatusTrue(owner, id)
                    .orThrowNotFound("ValidationAmount", id)
            }

        override fun save(entity: ValidationAmount): ServiceResult<ValidationAmount> =
            handleServiceOperation("save", entity.validationId) {
                val owner = TenantContext.getCurrentOwner()
                entity.owner = owner
                validateOrThrow(entity)
                val timestamp = nowTimestamp()
                entity.dateAdded = timestamp
                entity.dateUpdated = timestamp
                val saved = validationAmountRepository.saveAndFlush(entity)
                try {
                    accountRepository.updateValidationDateForAccountByOwner(saved.accountId, owner)
                } catch (ex: Exception) {
                    logger.warn("Failed to refresh account.validation_date for accountId=${saved.accountId}: ${ex.message}")
                }
                saved
            }

        override fun update(entity: ValidationAmount): ServiceResult<ValidationAmount> =
            handleServiceOperation("update", entity.validationId) {
                val owner = TenantContext.getCurrentOwner()
                val validationAmountToUpdate =
                    validationAmountRepository
                        .findByOwnerAndValidationIdAndActiveStatusTrue(owner, entity.validationId)
                        .orThrowNotFound("ValidationAmount", entity.validationId)
                validationAmountToUpdate.accountId = entity.accountId
                validationAmountToUpdate.amount = entity.amount
                validationAmountToUpdate.transactionState = entity.transactionState
                validationAmountToUpdate.validationDate = entity.validationDate
                validationAmountToUpdate.activeStatus = entity.activeStatus
                validationAmountToUpdate.dateUpdated = nowTimestamp()
                val updated = validationAmountRepository.saveAndFlush(validationAmountToUpdate)
                try {
                    accountRepository.updateValidationDateForAccountByOwner(updated.accountId, owner)
                } catch (ex: Exception) {
                    logger.warn("Failed to refresh account.validation_date for accountId=${updated.accountId}: ${ex.message}")
                }
                updated
            }

        override fun deleteById(id: Long): ServiceResult<ValidationAmount> =
            handleServiceOperation("deleteById", id) {
                val owner = TenantContext.getCurrentOwner()
                val validationAmount =
                    validationAmountRepository
                        .findByOwnerAndValidationIdAndActiveStatusTrue(owner, id)
                        .orThrowNotFound("ValidationAmount", id)
                validationAmountRepository.delete(validationAmount)
                validationAmount
            }

        // ===== Legacy methods still needed by controller =====

        fun findValidationAmountByAccountNameOwner(
            accountNameOwner: String,
            transactionState: TransactionState,
        ): ValidationAmount {
            val owner = TenantContext.getCurrentOwner()
            // Find account by name owner
            val account =
                accountRepository.findByOwnerAndAccountNameOwner(owner, accountNameOwner).orElseThrow {
                    jakarta.persistence.EntityNotFoundException("Account not found: $accountNameOwner")
                }

            // Find validation amount by account ID and transaction state
            val validationAmounts = validationAmountRepository.findByOwnerAndTransactionStateAndAccountId(owner, transactionState, account.accountId)
            if (validationAmounts.isEmpty()) {
                throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found for account: $accountNameOwner and transaction state: $transactionState")
            }
            // Return the LATEST validation amount by date (newest first)
            return validationAmounts.maxByOrNull { it.validationDate }
                ?: throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found for account: $accountNameOwner and transaction state: $transactionState")
        }

        fun insertValidationAmount(
            accountNameOwner: String,
            validationAmount: ValidationAmount,
        ): ValidationAmount {
            val owner = TenantContext.getCurrentOwner()
            logger.info("Inserting validation amount for account: $accountNameOwner")

            // Determine the target accountId using request payload first, then fallback to path variable
            val providedAccountId = validationAmount.accountId
            val resolvedAccountId: Long =
                when {
                    providedAccountId > 0L -> {
                        // Prefer explicit accountId from payload when valid
                        providedAccountId
                    }

                    else -> {
                        val byOwner = accountRepository.findByOwnerAndAccountNameOwner(owner, accountNameOwner)
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
                    "Unable to resolve account for validation amount",
                )
            }

            // Set the resolved accountId
            validationAmount.accountId = resolvedAccountId

            // Use the standard save method and handle ServiceResult
            val result = save(validationAmount)
            return when (result) {
                is ServiceResult.Success -> {
                    result.data
                }

                is ServiceResult.ValidationError -> {
                    throw ValidationException("Validation failed: ${result.errors}")
                }

                is ServiceResult.BusinessError -> {
                    throw RuntimeException("Business error inserting validation amount: ${result.message}")
                }

                is ServiceResult.NotFound -> {
                    throw jakarta.persistence.EntityNotFoundException("ValidationAmount not found")
                }

                is ServiceResult.SystemError -> {
                    throw RuntimeException("Failed to insert validation amount", result.exception)
                }
            }
        }
    }
