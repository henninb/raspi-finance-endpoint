package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import java.sql.Timestamp
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_description")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Description(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_description_description_id_seq")
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "description_id", nullable = false)
    var descriptionId: Long,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,

    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "description", unique = true, nullable = false)
    @JsonProperty
    var description: String
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
