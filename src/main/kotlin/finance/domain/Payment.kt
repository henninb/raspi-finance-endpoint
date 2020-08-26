package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
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
@Table(name = "t_payment", uniqueConstraints = [UniqueConstraint(columnNames = ["account_name_owner", "transaction_date", "amount"])])
@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name="payment_id")
        var paymentId: Long,

        @JsonProperty
        @Column(name="account_name_owner", nullable = false)
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(name="transaction_date", columnDefinition = "DATE", nullable = false)
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = Constants.MUST_BE_DOLLAR_MESSAGE)
        @Column(name="amount", nullable = false)
        var amount: BigDecimal,

        @JsonProperty
        //TODO: add feature
        //@field:Pattern(regexp = Constants.UUID_PATTERN, message = Constants.MUST_BE_UUID_MESSAGE)
        @Column(name="guid_source")
        var guidSource: String?,

        @JsonProperty
        //TODO: add feature
        //@field:Pattern(regexp = Constants.UUID_PATTERN, message = Constants.MUST_BE_UUID_MESSAGE)
        @Column(name="guid_destination")
        var guidDestination: String?
) {
    constructor() : this(0L, "", Date(0), BigDecimal(0.00), "", "")

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
