package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import finance.utils.TransactionStateConverter
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import java.sql.Date
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_validation_amount_date")
@JsonIgnoreProperties(ignoreUnknown = true)
data class ValidationAmount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_validation_amount_date_validation_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "validation_id", nullable = false)
    var validationId: Long,

    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @field:ValidDate
    @Column(name = "validation_date", columnDefinition = "DATE", nullable = false)
    @JsonProperty
    var validationDate: Timestamp,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @JsonProperty
    @field:Convert(converter = TransactionStateConverter::class)
    @Column(name = "transaction_state", nullable = false)
    var transactionState: TransactionState
) {
    constructor() : this(0L, 0L, Timestamp(0L),true, TransactionState.Undefined )

//    @JsonIgnore
//    @Column(name = "date_added", nullable = false)
//    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)
//
//    @JsonIgnore
//    @Column(name = "date_updated", nullable = false)
//    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
