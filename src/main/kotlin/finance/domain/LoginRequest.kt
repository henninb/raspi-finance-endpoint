package finance.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LoginRequest(
    @field:NotBlank(message = "Username cannot be blank")
    @param:JsonProperty
    val username: String = "",

    @field:NotBlank(message = "Password cannot be blank")
    @param:JsonProperty
    val password: String = ""
)
