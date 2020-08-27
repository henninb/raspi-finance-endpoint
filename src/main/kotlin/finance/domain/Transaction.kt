package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.services.ExcelFileService
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE
import finance.utils.Constants.MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.MUST_BE_DOLLAR_MESSAGE
import finance.utils.Constants.MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.LowerCaseConverter
import finance.utils.ValidDate
import org.hibernate.annotations.Proxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

        @Column(name="account_type", columnDefinition = "TEXT", nullable = false)
        @JsonProperty
        @field:Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        @Column(name="account_name_owner", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(name="transaction_date", columnDefinition = "DATE", nullable = false)
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Size(min = 1, max = 75)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name="description", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        var description: String,

        @JsonProperty
        @field:Size(max = 50)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name="category", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
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
        @Column(name = "reoccurring", columnDefinition = "BOOLEAN DEFAULT TRUE", nullable = false)
        var reoccurring: Boolean?,

        @JsonProperty
        @field:Size(max = 100)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "notes", nullable = false)
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

//    @JsonGetter("description")
//    fun jsonGetterDescription(): String {
//        logger.info("** jsonGetterDescription called **")
//        return description.toLowerCase()
//    }

    @JsonSetter("description")
    fun jsonSetterDescription( description: String) {
//        logger.info("** jsonSetterDescription called **")
        this.description = description.toLowerCase()
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
        val logger: Logger
            get() = LoggerFactory.getLogger(Transaction::class.java)
        //TODO: uncertain what this will do
//        operator fun invoke(description: String): Transaction {
//            return Transaction(description.toLowerCase())
//        }
    }
}
