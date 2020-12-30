package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.ALPHA_NUMERIC_NO_SPACE
import finance.utils.Constants.MUST_BE_NUMERIC_NO_SPACE
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size


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
    @field:Pattern(regexp = ALPHA_NUMERIC_NO_SPACE, message = MUST_BE_NUMERIC_NO_SPACE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "category", unique = true, nullable = false)
    @JsonProperty
    var category: String
) {
    constructor() : this(0L, true, "")

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        mapper.setTimeZone(TimeZone.getDefault())
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
