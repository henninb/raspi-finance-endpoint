package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
import finance.utils.Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_category")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_category_category_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "category_id", nullable = false)
    var categoryId: Long,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @field:Size(min = 1, max = 50)
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE_PATTERN, message = FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "category_name", unique = true, nullable = false)
    @JsonProperty
    var categoryName: String
) {
    constructor() : this(0L, true, "")

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @Transient
    @JsonProperty
    var categoryCount: Long = 0

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
