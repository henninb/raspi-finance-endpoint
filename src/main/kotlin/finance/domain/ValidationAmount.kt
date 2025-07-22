package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.TransactionStateConverter
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min

@Entity
@Table(name = "t_validation_amount")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ValidationAmount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_validation_amount_validation_id_seq")
    @param:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "validation_id", nullable = false)
    var validationId: Long,

    @param:JsonProperty
    @param:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    //@field:ValidTimestamp
    @Column(name = "validation_date", nullable = false)
    @param:JsonProperty
    var validationDate: Timestamp,

    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @param:JsonProperty
    @field:Convert(converter = TransactionStateConverter::class)
    @Column(name = "transaction_state", nullable = false)
    var transactionState: TransactionState,

    @param:JsonProperty
    @param:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var amount: BigDecimal
) {
    constructor() : this(0L, 0L, Timestamp(0L), true, TransactionState.Undefined, BigDecimal(0.0))

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
