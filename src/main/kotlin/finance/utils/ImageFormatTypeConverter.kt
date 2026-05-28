package finance.utils

import finance.domain.ImageFormatType
import jakarta.persistence.Converter

@Converter
class ImageFormatTypeConverter : LabeledEnumConverter<ImageFormatType>(ImageFormatType::class.java)
