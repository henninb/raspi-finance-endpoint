package finance.utils

import spock.lang.Specification

class ConstantsSpec extends Specification {

    def "Constants - counter names are defined correctly"() {
        expect:
        Constants.EXCEPTION_THROWN_COUNTER == "exception.thrown.counter"
        Constants.EXCEPTION_CAUGHT_COUNTER == "exception.caught.counter"
        Constants.TRANSACTION_ALREADY_EXISTS_COUNTER == "transaction.already.exists.counter"
        Constants.TRANSACTION_SUCCESSFULLY_INSERTED_COUNTER == "transaction.successfully.inserted.counter"
    }

    def "Constants - tag names are defined correctly"() {
        expect:
        Constants.EXCEPTION_NAME_TAG == "exception.name.tag"
        Constants.SERVER_NAME_TAG == "server.name.tag"
        Constants.ACCOUNT_NAME_OWNER_TAG == "account.name.owner.tag"
    }

    def "Constants - transaction specific counters are defined correctly"() {
        expect:
        Constants.TRANSACTION_ACCOUNT_LIST_NONE_FOUND_COUNTER == "transaction.account.list.none.found.counter"
        Constants.TRANSACTION_TRANSACTION_STATE_UPDATED_CLEARED_COUNTER == "transaction.transaction.state.updated.cleared.counter"
        Constants.TRANSACTION_RECEIPT_IMAGE_INSERTED_COUNTER == "transaction.receipt.image.inserted.counter"
        Constants.TRANSACTION_REST_SELECT_NONE_FOUND_COUNTER == "transaction.rest.select.none.found.counter"
        Constants.TRANSACTION_REST_TRANSACTION_STATE_UPDATE_FAILURE_COUNTER == "transaction.rest.transaction.state.update.failure.counter"
        Constants.TRANSACTION_REST_REOCCURRING_TYPE_UPDATE_FAILURE_COUNTER == "transaction.rest.reoccurring.type.update.failure.counter"
    }

    def "Constants - regex patterns are defined correctly"() {
        expect:
        Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN == "^[a-z0-9_-]*\$"
        Constants.ALPHA_UNDERSCORE_PATTERN == "^[a-z-]*_[a-z]*\$"
        Constants.ASCII_PATTERN == "^[\\u0000-\\u007F]*\$"
        Constants.UUID_PATTERN == "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}\$"
    }

    def "Constants - validation messages are defined correctly"() {
        expect:
        Constants.FIELD_MUST_BE_ASCII_MESSAGE == "must be ascii character set"
        Constants.FIELD_MUST_BE_UUID_MESSAGE == "must be uuid formatted"
        Constants.FIELD_MUST_BE_ALPHA_SEPARATED_BY_UNDERSCORE_MESSAGE == "must be alpha separated by an underscore"
        Constants.FIELD_MUST_BE_A_CURRENCY_MESSAGE == "must be dollar precision"
        Constants.FIELD_MUST_BE_NUMERIC_NO_SPACE_MESSAGE == "must be alphanumeric no space"
        Constants.FIELD_MUST_BE_FOUR_DIGITS_MESSAGE == "Must be 4 digits."
    }

    def "Constants - size validation messages are defined correctly"() {
        expect:
        Constants.FILED_MUST_BE_BETWEEN_ONE_AND_FIFTY_MESSAGE == "size must be between 1 and 50"
        Constants.FILED_MUST_BE_BETWEEN_ONE_AND_SEVENTY_FIVE_MESSAGE == "size must be between 1 and 75"
        Constants.FILED_MUST_BE_BETWEEN_THREE_AND_FORTY_MESSAGE == "size must be between 3 and 40"
        Constants.FILED_MUST_BE_BETWEEN_ZERO_AND_FIFTY_MESSAGE == "size must be between 0 and 50"
    }

    def "Constants - other validation messages are defined correctly"() {
        expect:
        Constants.FILED_MUST_BE_GREATER_THAN_ZERO_MESSAGE == "must be greater than or equal to 0"
        Constants.FILED_MUST_BE_ALPHA_NUMERIC_NO_SPACE_MESSAGE == "must be alphanumeric no space"
        Constants.FILED_MUST_BE_DATE_GREATER_THAN_MESSAGE == "date must be greater than 1/1/2000."
    }

    def "Constants - regex patterns work correctly for valid inputs"() {
        expect:
        "test_account" ==~ Constants.ALPHA_UNDERSCORE_PATTERN
        "test123_account" ==~ Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN
        "Hello World!" ==~ Constants.ASCII_PATTERN
        "12345678-1234-1234-1234-123456789012" ==~ Constants.UUID_PATTERN
    }

    def "Constants - regex patterns reject invalid inputs"() {
        expect:
        !("TestAccount" ==~ Constants.ALPHA_UNDERSCORE_PATTERN) // missing underscore
        !("test account" ==~ Constants.ALPHA_NUMERIC_NO_SPACE_PATTERN) // contains space
        !("12345678-1234-1234-1234-12345678901Z" ==~ Constants.UUID_PATTERN) // invalid UUID format
    }
}