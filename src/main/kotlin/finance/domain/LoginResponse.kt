package finance.domain

data class LoginResponse(
    val token: String? = null,
    val error: String? = null,
)
