package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginRequest(
    @field:NotBlank(message = "Username cannot be blank")
    @field:Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters")
    @param:JsonProperty("username")
    val username: String = "",
    @field:NotBlank(message = "Password cannot be blank")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @param:JsonProperty("password")
    val password: String = "",
)
