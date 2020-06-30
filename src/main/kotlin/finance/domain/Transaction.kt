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
import finance.utils.ValidTimestamp
import org.hibernate.annotations.Proxy
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
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
        var transactionId: Long,

        @Column(unique = true)
        @JsonProperty
        @field:Pattern(regexp = UUID_PATTERN, message = MUST_BE_UUID_MESSAGE)
        var guid: String,

        @JsonProperty
        @field:Min(value = 0L)
        var accountId: Long,

        @Column(columnDefinition = "VARCHAR")
        @JsonProperty
        @field:Convert(converter = AccountTypeConverter::class)
        var accountType: AccountType,

        @JsonProperty
        @field:Size(min = 3, max = 40)
        @field:Pattern(regexp = ALPHA_UNDERSCORE_PATTERN, message = MUST_BE_ALPHA_UNDERSCORE_MESSAGE)
        var accountNameOwner: String,

        @field:ValidDate
        @Column(columnDefinition = "DATE")
        @JsonProperty
        var transactionDate: Date,

        @JsonProperty
        @field:Size(min = 1, max = 75)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        var description: String,

        @JsonProperty
        @field:Size(max = 50)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        var category: String,

        @JsonProperty
        @field:Digits(integer = 6, fraction = 2, message = MUST_BE_DOLLAR_MESSAGE)
        var amount: BigDecimal,

        @JsonProperty
        @field:Min(value = -3)
        @field:Max(value = 1)
        @Column(name = "cleared")
        var cleared: Int,

        @JsonProperty
        var reoccurring: Boolean,

        @JsonProperty
        @field:Size(max = 100)
        @field:Pattern(regexp = ASCII_PATTERN, message = MUST_BE_ASCII_MESSAGE)
        var notes: String,

        @JsonProperty
        @field:ValidTimestamp
        var dateUpdated: Timestamp,

        @JsonProperty
        @field:ValidTimestamp
        var dateAdded: Timestamp,

        //TODO: remove this field as it is not required.
        @JsonProperty
        @field:Size(max = 70)
        var sha256: String) {

    constructor() : this(0L, "", 0, AccountType.Credit, "", Date(0),
            "", "", BigDecimal(0.00), 0, false, "",
            Timestamp(0), Timestamp(0), "") {
    }

    @JsonGetter("transactionDate")
    fun jsonGetterTransactionDate(): Long {
        return (this.transactionDate.time)
    }

    @JsonGetter("dateUpdated")
    fun jsonGetterDateUpdated(): Long {
        return (this.dateUpdated.time / 1000)
    }

    @JsonGetter("dateAdded")
    fun jsonGetterDateAdded(): Long {
        return (this.dateAdded.time / 1000)
    }

    //TODO: camelCase or snake_case?
    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "accountId", nullable = true, insertable = false, updatable = false)
    @JsonIgnore
    var account: Account? = null

    @ManyToMany
    @JoinTable(name = "t_transaction_categories",
            joinColumns = [JoinColumn(name = "transactionId")],
            inverseJoinColumns = [JoinColumn(name = "categoryId")])
    @JsonIgnore
    var categories = mutableListOf<Category>()

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
