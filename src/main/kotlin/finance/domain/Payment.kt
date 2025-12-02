package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import finance.utils.Constants.UUID_PATTERN
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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Calendar

@Entity
@Table(
    name = "t_payment",
    uniqueConstraints = [UniqueConstraint(columnNames = ["destination_account", "transaction_date", "amount"])],
)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_payment_payment_id_seq")
    @field:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "payment_id", nullable = false)
    var paymentId: Long,
    @param:JsonProperty
    // @Transient
    @Column(name = "source_account", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var sourceAccount: String,
    @param:JsonProperty
    // @Transient
    @Column(name = "destination_account", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = Constants.ALPHA_UNDERSCORE_PATTERN, message = Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    var destinationAccount: String,
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    @param:JsonProperty
    var transactionDate: LocalDate,
    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var amount: BigDecimal,
    @param:JsonProperty
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    @Column(name = "guid_source", nullable = true)
    var guidSource: String?,
    @param:JsonProperty
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    @Column(name = "guid_destination", nullable = true)
    var guidDestination: String?,
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
) {
    constructor() : this(0L, "", "", LocalDate.of(1970, 1, 1), BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP), "", "")

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper =
            ObjectMapper().apply {
                setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)
                findAndRegisterModules()
            }
    }
}
