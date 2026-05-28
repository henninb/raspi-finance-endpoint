package finance.utils

import finance.domain.LabeledEnum
import jakarta.persistence.AttributeConverter

abstract class LabeledEnumConverter<E>(
    private val enumClass: Class<E>,
) : AttributeConverter<E, String> where E : Enum<E>, E : LabeledEnum {
    override fun convertToDatabaseColumn(attribute: E): String = attribute.label

    override fun convertToEntityAttribute(dbData: String): E =
        enumClass.enumConstants
            .firstOrNull { it.label == dbData.trim().lowercase() }
            ?: throw RuntimeException("Unknown ${enumClass.simpleName} attribute: $dbData")
}
