package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.LowerCaseConverter
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(
    name = "t_transfer",
    uniqueConstraints = [UniqueConstraint(columnNames = ["source_account", "destination_account", "transaction_date", "amount"])]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transfer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_transaction_transaction_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "transfer_id", nullable = false)
    var transferId: Long,

    @JsonProperty
    @Column(name = "source_account", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Size(min = 3, max = 40)
    @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var sourceAccount: String,

    @JsonProperty
    @Column(name = "destination_account", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Size(min = 3, max = 40)
    @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var destinationAccount: String,

    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    @JsonProperty
    var transactionDate: Date,

    @JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var amount: BigDecimal,

    @JsonProperty
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    @Column(name = "guid_source", nullable = false)
    var guidSource: String?,

    @JsonProperty
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    @Column(name = "guid_destination", nullable = false)
    var guidDestination: String?,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true
) {

    constructor() : this(0L, "", "", Date(0), BigDecimal(0.00), "", "")

    @JsonGetter("transactionDate")
    fun jsonGetterTransferDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        return simpleDateFormat.format(this.transactionDate)
    }

    @JsonSetter("transactionDate")
    fun jsonSetterTransfertDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
