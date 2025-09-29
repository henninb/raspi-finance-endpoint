package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE
import finance.utils.LowerCaseConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.sql.Timestamp
import java.util.Calendar

@Entity
// @Proxy(lazy = false)
@Table(name = "t_description")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Description(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_description_description_id_seq")
    @field:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "description_id", nullable = false)
    var descriptionId: Long,
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
    @field:Size(min = 1, max = 50, message = FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "description_name", unique = true, nullable = false)
    @param:JsonProperty
    var descriptionName: String,
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
    var descriptionCount: Long = 0

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
