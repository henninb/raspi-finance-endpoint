package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE
import finance.utils.Constants.MUST_BE_DOLLAR_MESSAGE
import finance.utils.ValidTimestamp
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Timestamp
import javax.persistence.*
import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity(name = "AccountEntity")
@Proxy(lazy = false)
@Table(name = "t_account", uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "account_name_owner", "account_type"])])
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonProperty
        @field:Min(value = 0L)
        @Column(name="account_id", nullable = false)
        var accountId: Long,

        @JsonProperty
        @Column(name="account_name_owner", unique = true, nullable = false)
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @JsonProperty
        @Column(name="account_type", nullable = false)
        @Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        @Column(name="active_status", nullable = false)
        var activeStatus: Boolean,

        @JsonProperty
        //@Size(min = 4, max = 4, message = "Must be 4 digits.")
        @field:Pattern(regexp = "^[0-9]{4}$", message = "Must be 4 digits.")
        @Column(name="moniker")
        var moniker: String,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        @Column(name="totals")
        var totals: BigDecimal,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        @Column(name="totals_balanced")
        var totalsBalanced: BigDecimal,

        @JsonProperty
        @Column(name="date_closed")
        var dateClosed: Timestamp,

        @JsonProperty
        @ValidTimestamp
        @Column(name="date_updated", nullable = false)
        var dateUpdated: Timestamp,

        @JsonProperty
        @ValidTimestamp
        @Column(name="date_added", nullable = false)
        var dateAdded: Timestamp) {

    constructor() : this(0L, "", AccountType.Credit, true,
            "0000", BigDecimal(0.0), BigDecimal(0.0), Timestamp(0),
            Timestamp(System.currentTimeMillis()), Timestamp(System.currentTimeMillis()))

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}