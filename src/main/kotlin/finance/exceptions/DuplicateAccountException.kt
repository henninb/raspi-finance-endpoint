package finance.exceptions

import finance.domain.DomainException

/**
 * Exception thrown when an account operation would collide with an existing accountNameOwner.
 * The message is user-facing and results in a 409 Conflict HTTP response.
 */
class DuplicateAccountException(
    message: String,
) : DomainException(message)
