package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.AccountTypeConverter
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
import finance.utils.Constants.ALPHA_UNDERSCORE_PATTERN
import finance.utils.Constants.ASCII_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_ASCII_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE
import finance.utils.Constants.FIELD_MUST_BE_UUID_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE
import finance.utils.Constants.UUID_PATTERN
import finance.utils.LowerCaseConverter
import finance.utils.ReoccurringTypeConverter
import finance.utils.TransactionStateConverter
import finance.utils.TransactionTypeConverter
import finance.utils.ValidDate
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.apache.logging.log4j.LogManager
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Calendar

@Entity
@Table(name = "t_transaction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_transaction_transaction_id_seq")
    @field:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "transaction_id")
    var transactionId: Long = 0L,
    @param:JsonProperty
    @Column(name = "guid", unique = true, nullable = false)
    @field:Pattern(regexp = UUID_PATTERN, message = FIELD_MUST_BE_UUID_MESSAGE)
    var guid: String = "",
    @param:JsonProperty
    @field:Min(value = 0L)
    @Column(name = "account_id", nullable = false)
    var accountId: Long = 0L,
    @param:JsonProperty
    @Column(name = "account_type", columnDefinition = "TEXT", nullable = false)
    @field:Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType = AccountType.Undefined,
    @param:JsonProperty
    @Column(name = "transaction_type", columnDefinition = "TEXT", nullable = false)
    @field:Convert(converter = TransactionTypeConverter::class)
    var transactionType: TransactionType = TransactionType.Undefined,
    @param:JsonProperty
    @field:Size(min = 3, max = 40, message = FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE)
    @Column(name = "account_name_owner", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var accountNameOwner: String = "",
    @param:JsonProperty
    @field:ValidDate
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: LocalDate = LocalDate.of(1970, 1, 1),
    @param:JsonProperty
    @field:Size(min = 1, max = 75, message = FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @Column(name = "description", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var description: String = "",
    @param:JsonProperty
    @field:Size(max = 50, message = FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE)
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE_PATTERN, message = FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE)
    @Column(name = "category", nullable = false)
    @field:Convert(converter = LowerCaseConverter::class)
    var category: String = "",
    @param:JsonProperty
    @field:Digits(integer = 8, fraction = 2, message = FIELD_MUST_BE_A_CURRENCY_MESSAGE)
    @Column(name = "amount", nullable = false, precision = 8, scale = 2, columnDefinition = "NUMERIC(8,2) DEFAULT 0.00")
    var amount: BigDecimal = BigDecimal(0.00),
    @param:JsonProperty
    @field:Convert(converter = TransactionStateConverter::class)
    @Column(name = "transaction_state", nullable = false)
    var transactionState: TransactionState = TransactionState.Undefined,
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
    @param:JsonProperty
    @Column(name = "reoccurring_type", nullable = true, columnDefinition = "TEXT")
    @field:Convert(converter = ReoccurringTypeConverter::class)
    var reoccurringType: ReoccurringType = ReoccurringType.Undefined,
    @param:JsonProperty
    @field:Size(max = 100)
    @field:Pattern(regexp = ASCII_PATTERN, message = FIELD_MUST_BE_ASCII_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "notes", nullable = false)
    var notes: String = "",
) {
    constructor() : this(
        0L,
        "",
        0L,
        AccountType.Undefined,
        TransactionType.Undefined,
        "",
        LocalDate.of(1970, 1, 1),
        "",
        "",
        BigDecimal(0.00),
        TransactionState.Undefined,
        true,
        ReoccurringType.Undefined,
        "",
    )

    @Column(name = "due_date", columnDefinition = "DATE", nullable = true)
    @JsonProperty
    var dueDate: LocalDate? = null

    @JsonIgnore
    @Column(name = "receipt_image_id", nullable = true)
    var receiptImageId: Long? = null

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    // TODO: 11/19/2020 - cannot reference a transaction that does not exist
    // TODO: 11/19/2020 - Probably need to change to a OneToMany relationship
    // Foreign key constraint (one transaction can have many receiptImages)
    // @OneToOne(mappedBy = "receiptImageId", cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = true)
    @OneToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "receipt_image_id", nullable = true, insertable = false, updatable = false)
    @JsonProperty
    var receiptImage: ReceiptImage? = null

    // Foreign key constraint (many transactions can have one account)
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null

    // Foreign key constraint (many transactions can have many categories)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "t_transaction_categories",
        joinColumns = [JoinColumn(name = "transaction_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")],
    )
    @JsonIgnore
    var categories = mutableListOf<Category>()

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper =
            ObjectMapper().apply {
                setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                findAndRegisterModules()
            }

        @JsonIgnore
        private val logger = LogManager.getLogger()
    }
}
