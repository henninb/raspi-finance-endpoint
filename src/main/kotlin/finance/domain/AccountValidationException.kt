package finance.domain

/**
 * Exception thrown when account validation fails or account-related operations are invalid
 */
class AccountValidationException(
    message: String,
) : RuntimeException(message)
