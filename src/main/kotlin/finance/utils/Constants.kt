package finance.utils

object Constants {
    //TODO: fix the values
    const val EXCEPTION_COUNTER = "exception.counter"
    const val EXCEPTION_NAME_TYPE_TAG = "exception.name"
    const val ACCOUNT_NAME_TAG = "account.name"
    const val TRANSACTION_LIST_IS_EMPTY = "TRANSACTION_LIST_IS_EMPTY"
    const val TRANSACTION_RECEIVED_EVENT_COUNTER = "TRANSACTION_RECEIVED_EVENT_COUNTER"
    const val TRANSACTION_ALREADY_EXISTS_COUNTER = "TRANSACTION_ALREADY_EXISTS_COUNTER"
    const val TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER = "TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER"
    const val TRANSACTION_UPDATE_CLEARED_COUNTER = "TRANSACTION_UPDATE_CLEARED_COUNTER"
    const val TRANSACTION_RECEIPT_IMAGE = "TRANSACTION_RECEIPT_IMAGE"

    const val ALPHA_NUMERIC_NO_SPACE = "^[a-z0-9_-]*$"
    const val ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
    const val ASCII_PATTERN = "^[\\u0000-\\u007F]*$"

    //GUID - max length 36
    const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$"
    const val MUST_BE_ASCII_MESSAGE = "must be ascii character set"
    const val MUST_BE_UUID_MESSAGE = "must be uuid formatted"
    const val MUST_BE_ALPHA_UNDERSCORE_MESSAGE = "must be alpha separated by an underscore"
    const val MUST_BE_DOLLAR_MESSAGE = "must be dollar precision"
    const val MUST_BE_NUMERIC_NO_SPACE = "must be alphanumeric no space"

}