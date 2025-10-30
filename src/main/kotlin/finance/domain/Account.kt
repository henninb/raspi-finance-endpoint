package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_FOUR_DIGITS_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import finance.utils.LowerCaseConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.Calendar

@Entity
@Table(
    name = "t_account",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["account_name_owner", "account_type"],
            name = "uk_id_account_type",
        ),
    ],
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_account_account_id_seq")
    @field:Min(value = 0L)
    @field:JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,
    @param:JsonProperty
    @Column(name = "account_name_owner", unique = true, nullable = false)
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var accountNameOwner: String,
    @param:JsonProperty
    // @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType,
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
    @param:JsonProperty
    @field:Pattern(regexp = "^[0-9]{4}$", message = FIELD_MUST_BE_FOUR_DIGITS_MESSAGE)
    @Column(name = "moniker", columnDefinition = "TEXT DEFAULT '0000'")
    var moniker: String,
    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "outstanding", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var outstanding: BigDecimal,
    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "future", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var future: BigDecimal,
    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "cleared", precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var cleared: BigDecimal,
) {
    @JsonIgnore
    @Column(name = "date_closed")
    var dateClosed: Timestamp = Timestamp(0)

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "validation_date", nullable = false)
    var validationDate: Timestamp = Timestamp(System.currentTimeMillis())

    constructor() : this(
        0L,
        "",
        AccountType.Undefined,
        true,
        "0000",
        BigDecimal(0.0),
        BigDecimal(0.0),
        BigDecimal(0.0),
    )

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        // mapper.setTimeZone(TimeZone.getDefault())
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper =
            ObjectMapper().apply {
                setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                findAndRegisterModules()
            }
    }
}
