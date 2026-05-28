package finance.domain

sealed class ServiceResult<T> {
    data class Success<T>(
        val data: T,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(data: T): Success<T> = Success(data)
        }
    }

    data class NotFound<T>(
        val message: String,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(message: String): NotFound<T> = NotFound(message)
        }
    }

    data class ValidationError<T>(
        val errors: Map<String, String>,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(errors: Map<String, String>): ValidationError<T> = ValidationError(errors)
        }
    }

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

    data class SystemError<T>(
        val exception: Exception,
    ) : ServiceResult<T>() {
        companion object {
            @JvmStatic
            fun <T> of(exception: Exception): SystemError<T> = SystemError(exception)
        }
    }

    fun isSuccess(): Boolean = this is Success

    fun isError(): Boolean = !isSuccess()

    inline fun onSuccess(action: (T) -> Unit): ServiceResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    inline fun onError(action: (String) -> Unit): ServiceResult<T> {
        when (this) {
            is NotFound -> action(message)
            is ValidationError -> action(errors.toString())
            is BusinessError -> action(message)
            is SystemError -> action(exception.message ?: "System error occurred")
            is Success -> Unit
        }
        return this
    }

    fun getDataOrNull(): T? =
        when (this) {
            is Success -> data
            else -> null
        }

    fun getDataOrDefault(defaultValue: T): T =
        when (this) {
            is Success -> data
            else -> defaultValue
        }
}

fun <T> ServiceResult<T>.getOrThrow(): T =
    when (this) {
        is ServiceResult.Success -> data
        is ServiceResult.NotFound -> throw IllegalArgumentException(message)
        is ServiceResult.ValidationError -> throw IllegalArgumentException("Validation failed: $errors")
        is ServiceResult.BusinessError -> throw IllegalStateException(message)
        is ServiceResult.SystemError -> throw exception
    }
