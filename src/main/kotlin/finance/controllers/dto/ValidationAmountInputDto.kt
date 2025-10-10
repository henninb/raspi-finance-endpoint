package finance.controllers.dto

import finance.domain.TransactionState
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Timestamp

data class ValidationAmountInputDto(
    val validationId: Long? = null,
    @field:NotNull(message = "Account ID is required")
    val accountId: Long,
    @field:NotNull(message = "Validation date is required")
    val validationDate: Timestamp,
    val activeStatus: Boolean? = null,
    @field:NotNull(message = "Transaction state is required")
    val transactionState: TransactionState,
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    val amount: BigDecimal,
)
