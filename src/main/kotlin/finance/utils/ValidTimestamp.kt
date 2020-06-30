package finance.utils

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TimestampValidator::class])
annotation class ValidTimestamp(val message: String = "timestamp must be greater than 1/1/2000.", val groups: Array<KClass<*>> = [], val payload: Array<KClass<out Payload>> = [])