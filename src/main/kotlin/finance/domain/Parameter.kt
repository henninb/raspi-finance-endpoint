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
@Table(name = "t_parameter")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Parameter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_parameter_parameter_id_seq", initialValue = 1001, allocationSize = 100)
    @field:Min(value = 0L)
    @JsonProperty
    @Column(name = "parameter_id", nullable = false)
    var parameterId: Long,

    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_name", unique = true, nullable = false)
    @JsonProperty
    var parameterName: String,


    @field:Size(min = 1, max = 50)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_value", unique = true, nullable = false)
    @JsonProperty
    var parameterValue: String,

    @JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true
) {
    constructor() : this(0L, "", "", true)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String {
        return mapper.writeValueAsString(this)
    }

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
