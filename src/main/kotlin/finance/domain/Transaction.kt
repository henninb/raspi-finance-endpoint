package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.*
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.MUST_BE_ALPHA_UNDERSCORE_MESSAGE
import finance.utils.Constants.MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.MUST_BE_DOLLAR_MESSAGE
import finance.utils.Constants.MUST_BE_NUMERIC_NO_SPACE
import finance.utils.Constants.MUST_BE_UUID_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import org.hibernate.annotations.Proxy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.Date
import java.text.SimpleDateFormat
import javax.persistence.*
import javax.validation.constraints.Digits
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity(name = "TransactionEntity")
@Proxy(lazy = false)
@Table(name = "t_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SequenceGenerator(name = "t_transaction_transaction_id_seq")
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "transaction_id")
        var transactionId: Long,

        @Column(name = "guid", unique = true, nullable = false)
        @JsonProperty
        @field:Pattern(regexp = UUID_PATTERN, message = MUST_BE_UUID_MESSAGE)
        var guid: String,

        @JsonProperty
        @field:Min(value = 0L)
        @Column(name = "account_id", nullable = false)
        var accountId: Long,

        @Column(name = "account_type", columnDefinition = "TEXT", nullable = false)
        @JsonProperty
        @field:Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        @Column(name = "account_name_owner", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Size(min = 1, max = 75)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @Column(name = "description", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        var description: String,

        @JsonProperty
        @field:Size(max = 50)
        @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE, message = MUST_BE_NUMERIC_NO_SPACE)
        @Column(name = "category", nullable = false)
        @field:Convert(converter = LowerCaseConverter::class)
        var category: String,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        @Column(name = "amount", nullable = false)
        var amount: BigDecimal,

        @JsonProperty
        @field:Convert(converter = TransactionStateConverter::class)
        @Column(name = "transaction_state", nullable = false)
        var transactionState: TransactionState,

        @JsonProperty
        @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var activeStatus: Boolean?,

        @JsonProperty
        @Column(name = "reoccurring", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var reoccurring: Boolean?,

        @Column(name = "reoccurring_type", nullable = true, columnDefinition = "TEXT")
        @JsonProperty
        @field:Convert(converter = ReoccurringTypeConverter::class)
        var reoccurringType: ReoccurringType?,

        @JsonProperty
        @field:Size(max = 100)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "notes", nullable = false)
        var notes: String
) {

    constructor() : this(0L, "", 0, AccountType.Undefined, "", Date(0),
            "", "", BigDecimal(0.00), TransactionState.Undefined, true, false, ReoccurringType.Undefined, "")

    @JsonGetter("transactionDate")
    fun jsonGetterTransactionDate(): String {
        return SimpleDateFormat("yyyy-MM-dd").format(this.transactionDate)
    }

//    //TODO: look to remove this field as it may not be required
//    @JsonIgnore
//    @Column(name = "receipt_image_id", nullable = true)
//    var receiptImageId: Long? = null

    //TODO: 11/19/2020 - cannot reference a transaction that does not exist
    //Foreign key constraint
    //TODO: Probably need to change to a OneToMany relationship (one transaction can have many receiptImages)
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
    }
}
