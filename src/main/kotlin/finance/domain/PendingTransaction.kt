package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.LowerCaseConverter
import finance.utils.ValidDate
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Calendar

@Entity
@Table(name = "t_pending_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PendingTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_pending_transaction_pending_transaction_id_seq")
    @field:Min(value = 0L)
    @Column(name = "pending_transaction_id")
    var pendingTransactionId: Long = 0L,
    @field:Size(min = 3, max = 40)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    var accountNameOwner: String,
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: LocalDate,
    @field:Size(min = 1, max = 75)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    var description: String,
    @field:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var amount: BigDecimal,
    @Column(name = "review_status", nullable = false, columnDefinition = "TEXT DEFAULT 'pending'")
    var reviewStatus: String = "pending",
    @Column(name = "owner", nullable = false)
    @field:Size(max = 100, message = "Owner must be 100 characters or less")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String = "",
    @jakarta.persistence.Transient
    @JsonIgnore
    var account: Account? = null,
) {
    @JsonCreator
    constructor() : this(0L, "", LocalDate.of(1970, 1, 1), "", BigDecimal(0.00), "pending", "", null)

    @JsonIgnore
    @Column(name = "date_added", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
