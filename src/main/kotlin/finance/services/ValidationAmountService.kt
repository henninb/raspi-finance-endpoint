package finance.services

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import jakarta.validation.ConstraintViolation

@Service
open class ValidationAmountService(
    private var validationAmountRepository: ValidationAmountRepository,
    private var accountRepository: AccountRepository
) : IValidationAmountService, BaseService() {




    override fun insertValidationAmount(
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

        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        handleConstraintViolations(constraintViolations, meterService)

        // Ensure the entity references the resolved accountId
        validationAmount.accountId = resolvedAccountId

        val timestamp = Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = timestamp
        validationAmount.dateUpdated = timestamp

        // Save the ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table using the resolved accountId
        accountRepository.findByAccountId(resolvedAccountId).ifPresent { account ->
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
            logger.info("Updated validation date for accountId: $resolvedAccountId")
        }

        logger.info("Successfully inserted validation amount with ID: ${savedValidationAmount.validationId}")
        return savedValidationAmount
    }

    @Timed
    override fun findValidationAmountByAccountNameOwner(
        accountNameOwner: String,
        traansactionState: TransactionState
    ): ValidationAmount {
        logger.info("Finding validation amount for account: $accountNameOwner, state: $traansactionState")
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            val validationAmountList = validationAmountRepository.findByTransactionStateAndAccountId(
                traansactionState,
                accountOptional.get().accountId
            )
            if (validationAmountList.isEmpty()) {
                logger.info("No validation amounts found for account: $accountNameOwner")
                return ValidationAmount()
            }
            val latestValidation = validationAmountList.sortedByDescending { it.validationDate }.first()
            logger.info("Found validation amount for account: $accountNameOwner")
            return latestValidation
        }
        logger.warn("Account not found: $accountNameOwner")
        return ValidationAmount()
    }

    // ===== STANDARDIZED CRUD METHODS =====

    @Timed
    override fun findAllActiveValidationAmounts(): List<ValidationAmount> {
        logger.info("Finding all active validation amounts")
        val validationAmounts = validationAmountRepository.findByActiveStatusTrueOrderByValidationDateDesc()
        logger.info("Found ${validationAmounts.size} active validation amounts")
        return validationAmounts
    }

    @Timed
    override fun findValidationAmountById(validationId: Long): Optional<ValidationAmount> {
        logger.info("Finding validation amount by ID: $validationId")
        val validationAmount = validationAmountRepository.findByValidationIdAndActiveStatusTrue(validationId)
        if (validationAmount.isPresent) {
            logger.info("Found validation amount: $validationId")
        } else {
            logger.warn("Validation amount not found: $validationId")
        }
        return validationAmount
    }

    @Timed
    override fun insertValidationAmount(validationAmount: ValidationAmount): ValidationAmount {
        logger.info("Inserting validation amount (standardized)")

        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        handleConstraintViolations(constraintViolations, meterService)

        val timestamp = Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = timestamp
        validationAmount.dateUpdated = timestamp

        // Save the ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table using the accountId
        accountRepository.findByAccountId(validationAmount.accountId).ifPresent { account ->
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
            logger.info("Updated validation date for accountId: ${validationAmount.accountId}")
        }

        logger.info("Successfully inserted validation amount with ID: ${savedValidationAmount.validationId}")
        return savedValidationAmount
    }

    @Timed
    override fun updateValidationAmount(validationAmount: ValidationAmount): ValidationAmount {
        logger.info("Updating validation amount: ${validationAmount.validationId}")

        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        handleConstraintViolations(constraintViolations, meterService)

        val timestamp = Timestamp(System.currentTimeMillis())
        validationAmount.dateUpdated = timestamp

        // Save the updated ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table
        accountRepository.findByAccountId(validationAmount.accountId).ifPresent { account ->
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
            logger.info("Updated validation date for accountId: ${validationAmount.accountId}")
        }

        logger.info("Successfully updated validation amount: ${validationAmount.validationId}")
        return savedValidationAmount
    }

    @Timed
    override fun deleteValidationAmount(validationId: Long) {
        logger.info("Deleting validation amount: $validationId")

        val validationAmount = validationAmountRepository.findByValidationIdAndActiveStatusTrue(validationId)
            .orElseThrow {
                logger.warn("Validation amount not found for deletion: $validationId")
                org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Validation amount not found: $validationId"
                )
            }

        // Soft delete by setting activeStatus to false
        validationAmount.activeStatus = false
        validationAmount.dateUpdated = Timestamp(System.currentTimeMillis())
        validationAmountRepository.saveAndFlush(validationAmount)

        logger.info("Successfully deleted validation amount: $validationId")
    }
}
