package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginRequest(
    @field:NotBlank(message = "Username cannot be blank")
    @param:JsonProperty("username")
    val username: String = "",
    @field:NotBlank(message = "Password cannot be blank")
    @param:JsonProperty("password")
    val password: String = "",
)
