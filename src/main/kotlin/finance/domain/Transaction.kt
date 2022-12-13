package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.*
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import org.apache.logging.log4j.LogManager
import org.hibernate.annotations.Proxy
import org.springframework.data.annotation.Immutable
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
//does not fix the issue
//@Immutable //The [account] property of the [finance.domain.Transaction] entity was modified, but it won't be updated because the property is immutable.
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_transaction_transaction_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "transaction_id")
    var transactionId: Long,

    @JsonProperty
    @Column(name = "guid", unique = true, nullable = false)
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    var guid: String,

    @JsonProperty
    @field:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long,

    @JsonProperty
    @Column(name = "account_type", columnDefinition = "TEXT", nullable = false)
    @field:Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType,

    @JsonProperty
    @Column(name = "transaction_type", columnDefinition = "TEXT", nullable = false)
    @field:Convert(converter = TransactionTypeConverter::class)
    var transactionType: TransactionType,

    @JsonProperty
    @field:Size(min = 3, max = 40)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var accountNameOwner: String,

    @JsonProperty
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: Date,

    @JsonProperty
    @field:Size(min = 1, max = 75)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var description: String,

    @JsonProperty
    @field:Size(max = 50)
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE_PATTERN, message = FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE)
    @Column(name = "category", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var category: String,

    @JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var amount: BigDecimal,

    @JsonProperty
    @field:Convert(converter = TransactionStateConverter::class)
    @Column(name = "transaction_state", nullable = false)
    var transactionState: TransactionState,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @JsonProperty
    @Column(name = "reoccurring_type", nullable = true, columnDefinition = "TEXT")
    @field:Convert(converter = ReoccurringTypeConverter::class)
    var reoccurringType: ReoccurringType = ReoccurringType.Undefined,

    @JsonProperty
    @field:Size(max = 100)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "notes", nullable = false)
    var notes: String = ""
) {

    constructor() : this(
        0L, "", 0, AccountType.Undefined, TransactionType.Undefined,"", Date(0),
        "", "", BigDecimal(0.00), TransactionState.Undefined, true, ReoccurringType.Undefined, ""
    )

    @JsonGetter("transactionDate")
    fun jsonGetterTransactionDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
//        simpleDateFormat.timeZone = TimeZone.getDefault()
//        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return simpleDateFormat.format(this.transactionDate)
    }

//    @JsonGetter("dueDate")
//    fun jsonGetterDueDate(): String {
//        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
//        simpleDateFormat.isLenient = false
//        return simpleDateFormat.format(this.dueDate)
//    }

    @JsonSetter("transactionDate")
    fun jsonSetterTransactionDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
//        simpleDateFormat.timeZone = TimeZone.getDefault()
//        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }

//    @JsonSetter("dueDate")
//    fun jsonSetterDueDate(stringDate: String) {
//        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
//        simpleDateFormat.isLenient = false
//        this.dueDate = Date(simpleDateFormat.parse(stringDate).time)
//    }

    @Column(name = "due_date", columnDefinition = "DATE", nullable = true)
    @JsonProperty
    var dueDate: Date? = null

    @JsonIgnore
    @Column(name = "receipt_image_id", nullable = true)
    var receiptImageId: Long? = null

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    //TODO: 11/19/2020 - cannot reference a transaction that does not exist
    //TODO: 11/19/2020 - Probably need to change to a OneToMany relationship
    //Foreign key constraint (one transaction can have many receiptImages)
    //@OneToOne(mappedBy = "receiptImageId", cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = true)
    @OneToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "receipt_image_id", nullable = true, insertable = false, updatable = false)
    @JsonProperty
    var receiptImage: ReceiptImage? = null

    //Foreign key constraint (many transactions can have one account)
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null

    //Foreign key constraint (many transactions can have many categories)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "t_transaction_categories",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    @JsonIgnore
    var categories = mutableListOf<Category>()

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()

        @JsonIgnore
        private val logger = LogManager.getLogger()
    }
}
