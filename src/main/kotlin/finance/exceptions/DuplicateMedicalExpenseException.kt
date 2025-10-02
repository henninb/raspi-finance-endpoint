package finance.exceptions

/**
 * Exception thrown when attempting to create a medical expense for a transaction that already has one.
 * This should result in a 409 Conflict HTTP response.
 */
class DuplicateMedicalExpenseException(
    message: String,
) : RuntimeException(message)
