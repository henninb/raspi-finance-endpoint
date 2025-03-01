package finance.domain

import com.fasterxml.jackson.annotation.*
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.ValidDate
import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.*

@Entity
@Proxy(lazy = false)
@Table(name = "t_pending_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class PendingTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_pending_transaction_pending_transaction_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "pending_transaction_id")
    var pendingTransactionId: Long = 0L,

    @JsonProperty
    @field:Size(min = 3, max = 40)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    var accountNameOwner: String,

    @JsonProperty
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: Date,

    @JsonProperty
    @field:Size(min = 1, max = 75)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    var description: String,

    @JsonProperty
    @field:Digits(integer = 12, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMERIC(12,2) DEFAULT 0.00")
    var amount: BigDecimal,

    @JsonProperty
    @Column(name = "review_status", nullable = false, columnDefinition = "TEXT DEFAULT 'pending'")
    var reviewStatus: String = "pending",

    @JsonProperty
    @Column(name = "owner", nullable = true)
    var owner: String? = null,

    @JsonIgnore
    @Column(name = "date_added", nullable = false, columnDefinition = "TIMESTAMP DEFAULT now()")
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time),

    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_name_owner", referencedColumnName = "account_name_owner", insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null
)
