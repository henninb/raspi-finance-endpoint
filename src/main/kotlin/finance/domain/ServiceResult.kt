package finance.domain

/**
 * Standardized service response wrapper for consistent error handling
 * Provides a type-safe way to handle service operation results with comprehensive error information
 */
sealed class ServiceResult<T> {
    /**
     * Represents a successful operation with data
     */
    data class Success<T>(
        val data: T,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(data: T): Success<T> = Success(data)
        }
    }

    /**
     * Represents a not found error
     */
    data class NotFound<T>(
        val message: String,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(message: String): NotFound<T> = NotFound(message)
        }
    }

    /**
     * Represents validation errors with field-specific error messages
     */
    data class ValidationError<T>(
        val errors: Map<String, String>,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(errors: Map<String, String>): ValidationError<T> = ValidationError(errors)
        }
    }

    /**
     * Represents business logic errors with error codes
     */
    data class BusinessError<T>(
        val message: String,
        val errorCode: String,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(
                message: String,
                errorCode: String,
            ): BusinessError<T> = BusinessError(message, errorCode)
        }
    }

    /**
     * Represents system/technical errors with exception details
     */
    data class SystemError<T>(
        val exception: Exception,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(exception: Exception): SystemError<T> = SystemError(exception)
        }
    }

    /**
     * Returns true if this result represents a successful operation
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Returns true if this result represents an error
     */
    fun isError(): Boolean = !isSuccess()

    /**
     * Executes the given action if this result is successful
     * @param action Function to execute with the successful data
     * @return This ServiceResult for method chaining
     */
    inline fun onSuccess(action: (T) -> Unit): ServiceResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Executes the given action if this result is an error
     * @param action Function to execute with the error message
     * @return This ServiceResult for method chaining
     */
    inline fun onError(action: (String) -> Unit): ServiceResult<T> {
        when (this) {
            is NotFound -> action(message)
            is ValidationError -> action(errors.toString())
            is BusinessError -> action(message)
            is SystemError -> action(exception.message ?: "System error occurred")
            else -> {
                // Success case - do nothing
            }
        }
        return this
    }

    /**
     * Returns the data if successful, null otherwise
     */
    fun getDataOrNull(): T? =
        when (this) {
            is Success -> data
            else -> null
        }

    /**
     * Returns the data if successful, default value otherwise
     */
    fun getDataOrDefault(defaultValue: T): T =
        when (this) {
            is Success -> data
            else -> defaultValue
        }
}
