package finance.utils

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ImageValidator::class])
annotation class ValidImage(
    val message: String = "image must be a jpeg or png file.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)