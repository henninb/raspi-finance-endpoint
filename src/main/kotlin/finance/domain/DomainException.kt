package finance.domain

/** Base class for all domain-level business rule violations. */
open class DomainException(
    message: String,
) : RuntimeException(message)
