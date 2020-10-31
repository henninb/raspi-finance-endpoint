package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import javax.persistence.*
import javax.validation.constraints.Min
import javax.validation.constraints.Size

@Entity(name = "DescriptionEntity")
@Proxy(lazy = false)
@Table(name = "t_description")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Description(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "description_id", nullable = false)
        var description_id: Long,

        @field:Size(min = 1, max = 50)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "description", unique = true, nullable = false)
        @JsonProperty
        var description: String
) {
    constructor() : this(0L, "")

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}