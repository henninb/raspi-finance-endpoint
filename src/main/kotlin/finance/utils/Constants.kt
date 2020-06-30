package finance.utils

object Constants {

    const val ALPHA_UNDERSCORE_PATTERN = "^[a-z-]*_[a-z]*$"
    const val ASCII_PATTERN = "^[\\u0000-\\u007F]*$"

    //GUID - max length 36
    const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$"

    //const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$|^[a-z0-9]{6}-[a-z0-9]{3}-[a-z0-9]{4}-[a-z0-9]{3}-[a-z0-9]{12}$"
    //const val UUID_PATTERN = "^[a-z0-9]{6,8}-[a-z0-9]{3,4}-[a-z0-9]{3,4}-[a-z0-9]{3,4}-[a-z0-9]{10,12}$"
    //const val UUID_PATTERN = "^[a-z0-9]+-[a-z0-9]+-[a-z0-9]+-[a-z0-9]+-[a-z0-9]+$"
    const val MUST_BE_ASCII_MESSAGE = "must be ascii character set"
    const val MUST_BE_UUID_MESSAGE = "must be uuid formatted"
    const val MUST_BE_ALPHA_UNDERSCORE_MESSAGE = "must be alpha separated by an underscore"
    const val MUST_BE_DOLLAR_MESSAGE = "must be dollar precision"
}