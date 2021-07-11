package finance.services

import finance.domain.ValidationAmount

interface IValidationAmountService {
    fun insertValidationAmount(validationAmount: ValidationAmount)
}
