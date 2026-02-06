package finance.domain

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
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    @param:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "pending_transaction_id")
    var pendingTransactionId: Long = 0L,
    @param:JsonProperty
    @param:Size(min = 3, max = 40)
    @param:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    var accountNameOwner: String,
    @param:JsonProperty
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: LocalDate,
    @param:JsonProperty
    @param:Size(min = 1, max = 75)
    @param:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    var description: String,
    @param:JsonProperty
    @param:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var amount: BigDecimal,
    @param:JsonProperty
    @Column(name = "review_status", nullable = false, columnDefinition = "TEXT DEFAULT 'pending'")
    var reviewStatus: String = "pending",
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_name_owner", referencedColumnName = "account_name_owner", insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null,
) {
    @get:JsonProperty
    @Column(name = "owner", nullable = false)
    @field:Size(max = 100, message = "Owner must be 100 characters or less")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String = ""

    constructor() : this(0L, "", LocalDate.of(1970, 1, 1), "", BigDecimal(0.00), "pending")

    @JsonIgnore
    @Column(name = "date_added", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
