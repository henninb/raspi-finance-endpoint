package finance.domain

/**
 * Exception thrown when transaction validation fails or invalid transaction operations are attempted
 */
class TransactionValidationException(message: String) : RuntimeException(message)