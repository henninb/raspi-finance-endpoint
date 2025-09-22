package finance.services

import finance.domain.TransactionState
import finance.domain.ValidationAmount
import java.util.*

interface IValidationAmountService {
    // Legacy methods (backward compatibility)
    fun insertValidationAmount(accountNameOwner: String, validationAmount: ValidationAmount): ValidationAmount
    fun findValidationAmountByAccountNameOwner(
        accountNameOwner: String,
        transactionState: TransactionState
    ): ValidationAmount

    // Standardized CRUD methods
    fun findAllActiveValidationAmounts(): List<ValidationAmount>
    fun findValidationAmountById(validationId: Long): Optional<ValidationAmount>
    fun insertValidationAmount(validationAmount: ValidationAmount): ValidationAmount
    fun updateValidationAmount(validationAmount: ValidationAmount): ValidationAmount
    fun deleteValidationAmount(validationId: Long)
}
