package finance.services

import finance.domain.ValidationAmount
import java.util.*

interface IValidationAmountService {
    fun insertValidationAmount(validationAmount: ValidationAmount) : ValidationAmount
    fun findByAccountId(accountId: Long): Optional<ValidationAmount>
}
