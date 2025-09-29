package finance.domain

/**
 * Exception thrown when a transaction cannot be found by the given identifier
 */
class TransactionNotFoundException(message: String) : RuntimeException(message)
