package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import finance.utils.Constants
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import javax.persistence.*
import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size


@Entity(name = "PaymentEntity")
@Proxy(lazy = false)
@Table(name = "t_payment")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @field:Min(value = 0L)
        @JsonProperty
        var paymentId: Long,

        @JsonProperty
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(columnDefinition = "DATE")
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = Constants.MUST_BE_DOLLAR_MESSAGE)
        var amount: BigDecimal
) {
    constructor() : this(0L, "", Date(0), BigDecimal(0.00))
}
