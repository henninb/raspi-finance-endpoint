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

//    override fun insertValidationAmount(
//        accountNameOwner: String,
//        validationAmount: ValidationAmount
//    ): ValidationAmount {
//        var accountId = 0L
//        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
//        if (accountOptional.isPresent) {
//            accountId = accountOptional.get().accountId
//        }
//
//        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
//        handleConstraintViolations(constraintViolations, meterService)
//        validationAmount.accountId = accountId
//        validationAmount.dateAdded = Timestamp(Calendar.getInstance().time.time)
//        validationAmount.dateUpdated = Timestamp(Calendar.getInstance().time.time)
//        return validationAmountRepository.saveAndFlush(validationAmount)
//    }



    override fun insertValidationAmount(
        accountNameOwner: String,
        validationAmount: ValidationAmount
    ): ValidationAmount {
        var accountId = 0L
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            accountId = accountOptional.get().accountId
        }

        val constraintViolations: Set<ConstraintViolation<ValidationAmount>> = validator.validate(validationAmount)
        handleConstraintViolations(constraintViolations, meterService)

        validationAmount.accountId = accountId
        validationAmount.dateAdded = Timestamp(Calendar.getInstance().time.time)
        validationAmount.dateUpdated = Timestamp(Calendar.getInstance().time.time)

        // Save the ValidationAmount
        val savedValidationAmount = validationAmountRepository.saveAndFlush(validationAmount)

        // Update the validationDate in the Account table
        if (accountOptional.isPresent) {
            val account = accountOptional.get()
            account.validationDate = validationAmount.dateUpdated
            account.dateUpdated = validationAmount.dateUpdated
            accountRepository.saveAndFlush(account)
        }

        return savedValidationAmount
    }

    @Timed
    override fun findValidationAmountByAccountNameOwner(
        accountNameOwner: String,
        traansactionState: TransactionState
    ): ValidationAmount {
        val accountOptional = accountRepository.findByAccountNameOwner(accountNameOwner)
        if (accountOptional.isPresent) {
            val validationAmountList = validationAmountRepository.findByTransactionStateAndAccountId(
                traansactionState,
                accountOptional.get().accountId
            )
            if (validationAmountList.isEmpty()) {
                logger.info("empty list")
                return ValidationAmount()
            }
            logger.info("found a row")
            return validationAmountList.sortedByDescending { it.validationDate }.first()
        }
        logger.info("no account found")
        return ValidationAmount()
    }
}
