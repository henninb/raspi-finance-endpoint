package finance.domain

/**
 * Exception thrown when an invalid transaction state operation is attempted
 */
class InvalidTransactionStateException(
    message: String,
) : RuntimeException(message)
