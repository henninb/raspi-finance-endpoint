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
@Table(name = "t_account")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @JsonProperty
        @field:Min(value = 0L)
        var accountId: Long,

        @JsonProperty
        @Column(unique = true)
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @JsonProperty
        @Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        var activeStatus: Boolean,

        @JsonProperty
        //@Size(min = 4, max = 4, message = "Must be 4 digits.")
        @field:Pattern(regexp = "^[0-9]{4}$", message = "Must be 4 digits.")
        var moniker: String,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        var totals: BigDecimal,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        var totalsBalanced: BigDecimal,

        @JsonProperty
        //@ValidTimestamp
        var dateClosed: Timestamp,

        @JsonProperty
        @ValidTimestamp
        var dateUpdated: Timestamp,

        @JsonProperty
        @ValidTimestamp
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