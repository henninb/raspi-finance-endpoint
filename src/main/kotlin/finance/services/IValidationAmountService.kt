package finance.services

import finance.domain.TransactionState
import finance.domain.ValidationAmount

interface IValidationAmountService {
    fun insertValidationAmount(accountNameOwner: String, validationAmount: ValidationAmount) : ValidationAmount
    fun findValidationAmountByAccountNameOwner(accountNameOwner: String, traansactionState: TransactionState): ValidationAmount
}
