package finance.controllers.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.sql.Date

data class TransferInputDto(
    val transferId: Long? = null,
    @field:NotBlank
    val sourceAccount: String,
    @field:NotBlank
    val destinationAccount: String,
    @field:NotNull
    val transactionDate: Date,
    @field:NotNull
    @field:DecimalMin("0.01")
    val amount: BigDecimal,
    val activeStatus: Boolean? = null
)

