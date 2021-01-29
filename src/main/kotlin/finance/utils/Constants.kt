package finance.utils

object Constants {
    //TODO: fix the values
    const val EXCEPTION_THROWN_COUNTER = "exception.thrown.counter"
    const val EXCEPTION_CAUGHT_COUNTER = "exception.caught.counter"
    const val EXCEPTION_NAME_TAG = "exception.name.tag"
    const val SERVER_NAME_TAG = "server.name.tag"
    const val ACCOUNT_NAME_OWNER_TAG = "account.name.owner.tag"
    const val TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER = "transaction.account.list.none.found.counter"
    const val TRANSACTION_ALREADY_EXISTS_COUNTER = "transaction.already.exists.counter"
    const val TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER = "transaction.successfully.inserted.counter"
    const val TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER = "transaction.transaction.state.updated.cleared.counter"
    const val TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER = "transaction.receipt.image.inserted.counter"
    const val TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER = "transaction.rest.select.none.found.counter"
    const val TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER = "transaction.rest.transaction.state.update.failure.counter"
    const val TRANSACTION_REST_REOCCURRING_STATE_UPDATE_FAILURE_COUNTER = "transaction.rest.reoccurring.state.update.failure.counter"
    const val CAMEL_STRING_PROCESSOR_COUNTER = "camel.string.processor.counter"

    const val ALPHA_NUMERIC_NO_SPACE = "^[a-z0-9_-]*$"
    const val ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
    const val ASCII_PATTERN = "^[\\u0000-\\u007F]*$"

    //GUID length = 36
    const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$"
    const val MUST_BE_ASCII_MESSAGE = "must be ascii character set"
    const val MUST_BE_UUID_MESSAGE = "must be uuid formatted"
    const val MUST_BE_ALPHA_UNDERSCORE_MESSAGE = "must be alpha separated by an underscore"
    const val MUST_BE_DOLLAR_MESSAGE = "must be dollar precision"
    const val MUST_BE_NUMERIC_NO_SPACE = "must be alphanumeric no space"

}