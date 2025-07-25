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
        var accountId = 0L
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            accountId = accountOptional.get().accountId
        } else {
            logger.warn("Account not found: $accountNameOwner")
        }

        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        handleConstraintViolations(constraintViolations, meterService)

        validationAmount.accountId = accountId
        val timestamp = Timestamp(System.currentTimeMillis())
        validationAmount.dateAdded = timestamp
        validationAmount.dateUpdated = timestamp

        // Save the ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table
        if (accountOptional.isPresent) {
            val account = accountOptional.get()
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
            logger.info("Updated validation date for account: $accountNameOwner")
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
}
