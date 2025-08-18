package finance.domain

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.ObjectMapper
import finance.utils.LowerCaseConverter
import java.sql.Timestamp
import java.util.*
import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

@Entity
@Table(name = "t_user")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @SequenceGenerator(name = "t_user_user_id_seq")
        @param:Min(value = 0L)
        @param:JsonProperty
        @Column(name = "user_id", nullable = false)
        var userId: Long,

        @param:JsonProperty
        @Column(name = "active_status", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
        var activeStatus: Boolean = true,

        @field:Size(min = 1, max = 40, message = "First name must be between 1 and 40 characters")
        @field:NotBlank(message = "First name cannot be blank")
        @field:Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name can only contain letters, spaces, apostrophes, and hyphens")
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "first_name",  nullable = false)
        @get:JsonProperty
        var firstName: String,

        @field:Size(min = 1, max = 40, message = "Last name must be between 1 and 40 characters")
        @field:NotBlank(message = "Last name cannot be blank")
        @field:Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name can only contain letters, spaces, apostrophes, and hyphens")
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "last_name", nullable = false)
        @get:JsonProperty
        var lastName: String,

        @field:Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters")
        @field:NotBlank(message = "Username cannot be blank")
        @field:Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
        @field:Convert(converter = LowerCaseConverter::class)
        @Column(name = "username", unique = true, nullable = false)
        @param:JsonProperty
        var username: String,

        @field:Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
        @field:NotBlank(message = "Password cannot be blank")
        @Column(name = "password", unique = true, nullable = false)
        @param:JsonProperty
        var password: String
) {
    constructor() : this(0L, true, "","", "","")

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