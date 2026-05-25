package finance.domain

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

@JsonFormat
enum class ImageFormatType(val label: String) {
    @JsonProperty("jpeg")
    Jpeg("jpeg"),

    @JsonProperty("png")
    Png("png"),

    @JsonProperty("undefined")
    Undefined("undefined");

    override fun toString(): String = name.lowercase()
}