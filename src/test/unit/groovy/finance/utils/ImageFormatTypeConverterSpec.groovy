package finance.utils

import finance.domain.ImageFormatType
import spock.lang.Specification

class ImageFormatTypeConverterSpec extends Specification {

    ImageFormatTypeConverter converter = new ImageFormatTypeConverter()

    def "convertToDatabaseColumn converts ImageFormatType enum to string"() {
        expect:
        converter.convertToDatabaseColumn(imageFormatType) == expectedString

        where:
        imageFormatType               | expectedString
        ImageFormatType.Jpeg          | "jpeg"
        ImageFormatType.Png           | "png"
        ImageFormatType.Undefined     | "undefined"
    }

    def "convertToEntityAttribute converts string to ImageFormatType enum"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedImageFormatType

        where:
        inputString    | expectedImageFormatType
        "jpeg"         | ImageFormatType.Jpeg
        "png"          | ImageFormatType.Png
        "undefined"    | ImageFormatType.Undefined
    }

    def "convertToEntityAttribute handles case insensitive input"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedImageFormatType

        where:
        inputString    | expectedImageFormatType
        "JPEG"         | ImageFormatType.Jpeg
        "Jpeg"         | ImageFormatType.Jpeg
        "PNG"          | ImageFormatType.Png
        "Png"          | ImageFormatType.Png
        "UNDEFINED"    | ImageFormatType.Undefined
        "Undefined"    | ImageFormatType.Undefined
    }

    def "convertToEntityAttribute handles whitespace"() {
        expect:
        converter.convertToEntityAttribute(inputString) == expectedImageFormatType

        where:
        inputString       | expectedImageFormatType
        " jpeg "          | ImageFormatType.Jpeg
        "  png  "         | ImageFormatType.Png
        "\tundefined\t"   | ImageFormatType.Undefined
        "\njpeg\n"        | ImageFormatType.Jpeg
    }

    def "convertToEntityAttribute throws exception for unknown attribute"() {
        when:
        converter.convertToEntityAttribute(invalidInput)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidInput}"

        where:
        invalidInput << [
            "invalid", "gif", "bmp", "tiff", "webp", "svg", 
            "", " ", "null", "jpg", "image"
        ]
    }

    def "convertToEntityAttribute handles mixed case and whitespace with unknown values"() {
        when:
        converter.convertToEntityAttribute("  GIF  ")

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute:   GIF  "
    }

    def "all ImageFormatType enum values are handled in convertToDatabaseColumn"() {
        expect: "all enum values can be converted to database column"
        ImageFormatType.values().every { imageFormatType ->
            String result = converter.convertToDatabaseColumn(imageFormatType)
            result != null && !result.isEmpty()
        }
    }

    def "round trip conversion works correctly"() {
        expect: "converting to database and back returns original value"
        ImageFormatType.values().every { original ->
            String dbValue = converter.convertToDatabaseColumn(original)
            ImageFormatType roundTrip = converter.convertToEntityAttribute(dbValue)
            roundTrip == original
        }
    }

    def "convertToEntityAttribute handles common image format variations that should fail"() {
        when:
        converter.convertToEntityAttribute(invalidVariation)

        then:
        RuntimeException ex = thrown()
        ex.message == "Unknown attribute: ${invalidVariation}"

        where:
        invalidVariation << ["jpg", "JPG"]
    }
}