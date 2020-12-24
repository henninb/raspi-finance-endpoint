package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.LowerCaseConverter
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_payment", uniqueConstraints = [UniqueConstraint(columnNames = ["account_name_owner", "transaction_date", "amount"])])
@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SequenceGenerator(name = "t_payment_payment_id_seq")
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "payment_id", nullable = false)
        var paymentId: Long,

        @JsonProperty
        @Column(name = "account_name_owner", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Digits(integer = 8, fraction = 2, message = Constants.MUST_BE_DOLLAR_MESSAGE)
        @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
        var amount: BigDecimal,

        @JsonProperty
        @field:Pattern(regexp = UUID_PATTERN, message = MUST_BE_UUID_MESSAGE)
        @Column(name = "guid_source", nullable = false)
        var guidSource: String?,

        @JsonProperty
        @field:Pattern(regexp = UUID_PATTERN, message = MUST_BE_UUID_MESSAGE)
        @Column(name = "guid_destination", nullable = false)
        var guidDestination: String?,

        @JsonProperty
        @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var activeStatus: Boolean = true
) {

    constructor() : this(0L, "", Date(0), BigDecimal(0.00), "", "")

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        mapper.setTimeZone(TimeZone.getDefault())
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
