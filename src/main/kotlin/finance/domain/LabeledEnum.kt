package finance.domain

import com.fasterxml.jackson.annotation.JsonValue

interface LabeledEnum {
    @get:JsonValue
    val label: String
}

inline fun <reified E> fromLabel(label: String): E? where E : Enum<E>, E : LabeledEnum = enumValues<E>().firstOrNull { it.label.equals(label, ignoreCase = true) }

inline fun <reified E> fromLabelOrThrow(value: String?): E where E : Enum<E>, E : LabeledEnum = fromLabel<E>(value ?: "") ?: throw IllegalArgumentException("Unknown ${E::class.simpleName}: $value")
