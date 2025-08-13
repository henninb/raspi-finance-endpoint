package finance.domain

/**
 * Exception thrown when reoccurring type validation fails or invalid reoccurring operations are attempted
 */
class InvalidReoccurringTypeException(message: String) : RuntimeException(message)