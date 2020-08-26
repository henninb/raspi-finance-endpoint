package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE
import finance.utils.Constants.MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.MUST_BE_DOLLAR_MESSAGE
import finance.utils.Constants.MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import java.text.SimpleDateFormat
import javax.persistence.*
import javax.validation.constraints.*

@Entity(name = "TransactionEntity")
@Proxy(lazy = false)
@Table(name = "t_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
//TODO: the field activeStatus

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name="transaction_id")
        var transactionId: Long,

        @Column(name="guid", unique = true, nullable = false)
        @JsonProperty
        @field:Pattern(regexp = UUID_PATTERN, message = MUST_BE_UUID_MESSAGE)
        var guid: String,

        @JsonProperty
        @field:Min(value = 0L)
        @Column(name="account_id", nullable = false)
        var accountId: Long,

        @Column(name="account_type", columnDefinition = "VARCHAR", nullable = false)
        @JsonProperty
        @field:Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        @Column(name="account_name_owner", nullable = false)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(name="transaction_date", columnDefinition = "DATE", nullable = false)
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Size(min = 1, max = 75)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name="description", nullable = false)
        var description: String,

        @JsonProperty
        @field:Size(max = 50)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name="category", nullable = false)
        var category: String,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        @Column(name="amount", nullable = false)
        var amount: BigDecimal,

        @JsonProperty
        @field:Min(value = -1)
        @field:Max(value = 1)
        @Column(name = "cleared", nullable = false)
        var cleared: Int,

        @JsonProperty
        @Column(name = "reoccurring")
        var reoccurring: Boolean?,

        @JsonProperty
        @field:Size(max = 100)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name = "notes")
        var notes: String
        ) {

    constructor() : this(0L, "", 0, AccountType.Credit, "", Date(0),
            "", "", BigDecimal(0.00), 0, false, "")

//    @JsonGetter("transactionDate")
//    fun jsonGetterTransactionDate(): Long {
//        return (this.transactionDate.time)
//    }

    @JsonGetter("transactionDate")
    fun jsonGetterTransactionDate(): String {
        return SimpleDateFormat("yyyy-MM-dd").format(this.transactionDate)
    }

    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = true, insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "t_transaction_categories",
            joinColumns = [JoinColumn(name = "transactionId")],
            inverseJoinColumns = [JoinColumn(name = "category_id")])
    @JsonIgnore
    var categories = mutableListOf<Category>()

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
