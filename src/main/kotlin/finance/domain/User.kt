package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import org.hibernate.annotations.Proxy
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Entity
@Proxy(lazy = false)
@Table(name = "t_user")
@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SequenceGenerator(name = "t_user_user_id_seq")
        @field:Min(value = 0L)
        @JsonProperty
        @Column(name = "user_id", nullable = false)
        var userId: Long,

        @JsonProperty
        @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var activeStatus: Boolean = true,

        @field:Size(min = 1, max = 50)
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "username", unique = true, nullable = false)
        @JsonProperty
        var username: String,

        @field:Size(min = 1, max = 50)
        @Column(name = "password", unique = true, nullable = false)
        @JsonProperty
        var password: String
        //BCryptPasswordEncoder
) {
    constructor() : this(0L, true, "", "")

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