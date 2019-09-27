package finance.utils

object Constants {

    const val ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
    const val ASCII_PATTERN = "^[\\u0000-\\u007F]*$"
    const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$"
    const val MUST_BE_ASCII_MESSAGE = "must be ascii character set"
    const val MUST_BE_UUID_MESSAGE = "must be uuid formatted"
    const val MUST_BE_ALPHA_UNDERSCORE_MESSAGE = "must be alpha separated by an underscore"
    const val MUST_BE_DOLLAR_MESSAGE = "must be dollar precision"
    const val METRIC_TRANSACTION_VALIDATOR_FAILED_COUNTER = "transaction.validator.failed.count"
    const val METRIC_TRANSACTION_DATABASE_INSERT_COUNTER = "transaction.record.inserted.count"
    const val METRIC_TRANSACTION_ALREADY_EXISTS_COUNTER = "transaction.record.already.exists.count"
    const val METRIC_ACCOUNT_ALREADY_EXISTS_COUNTER = "account.record.already.exists.count"
    const val METRIC_ACCOUNT_NOT_FOUND_COUNTER = "account.record.not.found.count"
    const val METRIC_INSERT_TRANSACTION_TIMER = "insert.transaction.timer"
    const val METRIC_DUPLICATE_ACCOUNT_INSERT_ATTEMPT_COUNTER = "duplicate.account.insert.attempt.counter"
    const val METRIC_DUPLICATE_CATEGORY_INSERT_ATTEMPT_COUNTER = "duplicate.category.insert.attempt.counter"
}