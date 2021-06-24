package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE
import finance.utils.Constants.MUST_BE_DOLLAR_MESSAGE
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(
    name = "t_account",
    uniqueConstraints = [UniqueConstraint(
        columnNames = ["account_name_owner", "account_type"],
        name = "uk_id_account_type"
    )]
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_account_account_id_seq")
    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @JsonProperty
    @Column(name = "account_name_owner", unique = true, nullable = false)
    @field:Size(min = 3, max = 40)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
    var accountNameOwner: String,

    @JsonProperty
    @Column(name = "account_type", nullable = false)
    @Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @JsonProperty
    @field:Pattern(regexp = "^[0-9]{4}$", message = "Must be 4 digits.")
    @Column(name = "moniker", columnDefinition = "TEXT DEFAULT '0000'")
    var moniker: String,

    @JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
    @Column(name = "outstanding", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var outstanding: BigDecimal,

    @JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
    @Column(name = "future", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var future: BigDecimal,

    @JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
    @Column(name = "cleared", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var cleared: BigDecimal,

    @JsonProperty
    @Column(name = "date_closed")
    var dateClosed: Timestamp
) {

    constructor() : this(
        0L, "", AccountType.Undefined, true,
        "0000", BigDecimal(0.0), BigDecimal(0.0), BigDecimal(0.0), Timestamp(0)
    )

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        //mapper.setTimeZone(TimeZone.getDefault())
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}