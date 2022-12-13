package finance.utils

import jakarta.validation.Constraint
import jakarta.validation.Payload
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