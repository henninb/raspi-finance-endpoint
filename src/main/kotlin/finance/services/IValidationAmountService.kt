package finance.services

import finance.domain.ValidationAmount
import java.util.*

interface IValidationAmountService {
    fun insertValidationAmount(accountNameOwner: String, validationAmount: ValidationAmount) : ValidationAmount
    fun findValidationAmountByAccountNameOwner(accountNameOwner: String): ValidationAmount
}
