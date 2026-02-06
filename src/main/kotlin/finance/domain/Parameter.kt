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
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.sql.Timestamp
import java.util.Calendar

@Entity
@Table(
    name = "t_parameter",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["owner", "parameter_name"],
            name = "uk_parameter_owner_name",
        ),
    ],
)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Parameter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "t_parameter_parameter_id_seq")
    @field:Min(value = 0L)
    @param:JsonProperty
    @Column(name = "parameter_id", nullable = false)
    var parameterId: Long = 0L,
    @param:JsonProperty
    @Column(name = "owner", nullable = false)
    @field:Size(max = 100, message = "Owner must be 100 characters or less")
    @field:Convert(converter = LowerCaseConverter::class)
    var owner: String = "",
    @field:Size(min = 1, max = 50, message = FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_name", nullable = false)
    @param:JsonProperty
    var parameterName: String = "",
    @field:Size(min = 1, max = 50, message = FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE)
    @field:Convert(converter = LowerCaseConverter::class)
    @Column(name = "parameter_value", unique = true, nullable = false)
    @param:JsonProperty
    var parameterValue: String = "",
    @param:JsonProperty
    @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    var activeStatus: Boolean = true,
) {
    constructor() : this(0L, "", "", "", true)

    @JsonIgnore
    @Column(name = "date_added", nullable = false)
    var dateAdded: Timestamp = Timestamp(Calendar.getInstance().time.time)

    @JsonIgnore
    @Column(name = "date_updated", nullable = false)
    var dateUpdated: Timestamp = Timestamp(Calendar.getInstance().time.time)

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
