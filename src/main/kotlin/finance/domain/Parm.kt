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

@Entity(name = "ParmEntity")
@Proxy(lazy = false)
@Table(name = "t_parm")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Parm(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SequenceGenerator(name = "t_parm_parm_id_seq")
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "parm_id", nullable = false)
        var parm_id: Long,

        @field:Size(min = 1, max = 50)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "parm_name", unique = true, nullable = false)
        @JsonProperty
        var parmName: String,

        @field:Size(min = 1, max = 50)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "parm_value", unique = true, nullable = false)
        @JsonProperty
        var parmValue: String
) {
    constructor() : this(0L, "", "")

    override fun toString(): String = mapper.writeValueAsString(this)

    companion object {
        @JsonIgnore
        private val mapper = ObjectMapper()
    }
}
