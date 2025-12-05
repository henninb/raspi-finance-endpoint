package finance.utils

object Constants {
    const val EXCEPTION_THROWN_COUNTER = "exception.thrown.counter"
    const val EXCEPTION_CAUGHT_COUNTER = "exception.caught.counter"
    const val EXCEPTION_NAME_TAG = "exception.name.tag"
    const val SERVER_NAME_TAG = "server.name.tag"
    const val ACCOUNT_NAME_OWNER_TAG = "account.name.owner.tag"
    const val TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER = "transaction.account.list.none.found.counter"
    const val TRANSACTION_ALREADY_EXISTS_COUNTER = "transaction.already.exists.counter"
    const val TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER = "transaction.successfully.inserted.counter"
    const val TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER =
        "transaction.transaction.state.updated.cleared.counter"
    const val TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER = "transaction.receipt.image.inserted.counter"
    const val TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER = "transaction.rest.select.none.found.counter"
    const val TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER =
        "transaction.rest.transaction.state.update.failure.counter"
    const val TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER =
        "transaction.rest.reoccurring.type.update.failure.counter"

    const val ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$"
    const val ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
    const val ASCII_PATTERN = "^[\\u0000-\\u007F]*$"
    const val UUID_PATTERN = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"

    const val FIELD_MUST_BE_ASCII_MESSAGE = "must be ascii character set"
    const val FIELD_MUST_BE_UUID_MESSAGE = "must be uuid formatted"
    const val FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE = "must be alpha separated by an underscore"
    const val FIELD_MUST_BE_A_CURRENCY_MESSAGE = "must be dollar precision"
    const val FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE = "must be alphanumeric no space"
    const val FIELD_MUST_BE_FOUR_DIGITS_MESSAGE = "Must be 4 digits."
    const val FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE = "size must be between 1 and 50"
    const val FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE = "size must be between 1 and 75"
    const val FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE = "must be greater than or equal to 0"
    const val FILED_MUST_BE_ALPHA_NUMERIC_NO_SPACE_MESSAGE = "must be alphanumeric no space"
    const val FILED_MUST_BE_DATE_GREATER_THAN_MESSAGE = "date must be greater than 1/1/2000."
    const val FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE = "size must be between 3 and 40"
    const val FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE = "size must be between 0 and 50"
}
