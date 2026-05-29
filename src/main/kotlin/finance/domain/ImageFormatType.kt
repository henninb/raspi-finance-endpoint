package finance.domain

import com.fasterxml.jackson.annotation.JsonCreator

enum class ImageFormatType(
    override val label: String,
) : LabeledEnum {
    Jpeg("jpeg"),
    Png("png"),
    Undefined("undefined"),
    ;

    override fun toString(): String = label

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromString(value: String?): ImageFormatType = fromLabelOrThrow(value)
    }
}
