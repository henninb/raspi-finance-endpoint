package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class ImageFormatTypeSpec extends Specification {

    @Unroll
    def "should have correct label for #imageFormatType"() {
        expect: "image format type has expected label"
        imageFormatType.label == expectedLabel

        where:
        imageFormatType              | expectedLabel
        ImageFormatType.Jpeg         | "jpeg"
        ImageFormatType.Png          | "png"
        ImageFormatType.Undefined    | "undefined"
    }

    @Unroll
    def "toString should return lowercase name for #imageFormatType"() {
        expect: "toString returns lowercase enum name"
        imageFormatType.toString() == expectedString

        where:
        imageFormatType              | expectedString
        ImageFormatType.Jpeg         | "jpeg"
        ImageFormatType.Png          | "png"
        ImageFormatType.Undefined    | "undefined"
    }

    def "all enum values should have non-empty labels"() {
        when: "getting all image format types"
        def allFormats = ImageFormatType.values() as List

        then: "every format has a non-empty label"
        allFormats.every { it.label != null && !it.label.isEmpty() }
    }

    def "all enum values should have lowercase labels"() {
        when: "getting all image format types"
        def allFormats = ImageFormatType.values() as List

        then: "every format has a lowercase label"
        allFormats.every { it.label == it.label.toLowerCase() }
    }

    def "enum should have exactly 3 values"() {
        when: "getting all image format types"
        def allFormats = ImageFormatType.values()

        then: "there are exactly 3 formats"
        allFormats.length == 3
    }

    def "should support enum comparison"() {
        expect:
        ImageFormatType.Jpeg == ImageFormatType.Jpeg
        ImageFormatType.Jpeg != ImageFormatType.Png
    }

    def "toString should match label for all formats"() {
        expect:
        ImageFormatType.Jpeg.toString() == ImageFormatType.Jpeg.label
        ImageFormatType.Png.toString() == ImageFormatType.Png.label
        ImageFormatType.Undefined.toString() == ImageFormatType.Undefined.label
    }

    def "should support switch statements"() {
        when: "using image format type in switch"
        def result
        switch(ImageFormatType.Jpeg) {
            case ImageFormatType.Jpeg:
                result = "jpeg image"
                break
            case ImageFormatType.Png:
                result = "png image"
                break
            default:
                result = "other image"
        }

        then: "switch works correctly"
        result == "jpeg image"
    }

    def "should have common image formats"() {
        expect: "JPEG and PNG are supported"
        ImageFormatType.Jpeg.label == "jpeg"
        ImageFormatType.Png.label == "png"
    }

    def "should distinguish between image formats"() {
        expect: "JPEG and PNG are different"
        ImageFormatType.Jpeg != ImageFormatType.Png
        ImageFormatType.Jpeg.label != ImageFormatType.Png.label
    }

    def "undefined should be available for unknown formats"() {
        expect: "undefined format exists"
        ImageFormatType.Undefined != null
        ImageFormatType.Undefined.label == "undefined"
    }

    def "enum values should be ordered consistently"() {
        when: "getting all values"
        def allFormats = ImageFormatType.values()

        then: "values are in declaration order"
        allFormats[0] == ImageFormatType.Jpeg
        allFormats[1] == ImageFormatType.Png
        allFormats[2] == ImageFormatType.Undefined
    }

    def "should support receipt image validation use cases"() {
        when: "checking if format is valid for upload"
        def validFormats = [ImageFormatType.Jpeg, ImageFormatType.Png]

        then: "jpeg and png are valid"
        validFormats.contains(ImageFormatType.Jpeg)
        validFormats.contains(ImageFormatType.Png)
        !validFormats.contains(ImageFormatType.Undefined)
    }
}
