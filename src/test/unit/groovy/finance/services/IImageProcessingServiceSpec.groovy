package finance.services

import finance.domain.ImageFormatType
import spock.lang.Specification

import java.lang.reflect.Method

class IImageProcessingServiceSpec extends Specification {

    def "should define createThumbnail method"() {
        when:
        def methods = IImageProcessingService.getDeclaredMethods().findAll { it.name == "createThumbnail" }

        then:
        methods.size() == 1
        def method = methods[0]
        method.returnType == byte[].class
        method.parameterTypes.length == 2
        method.parameterTypes[0] == byte[].class
        method.parameterTypes[1] == ImageFormatType.class
    }

    def "should define getImageFormatType method"() {
        when:
        Method method = IImageProcessingService.getMethod("getImageFormatType", byte[].class)

        then:
        method != null
        method.returnType == ImageFormatType.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == byte[].class
    }

    def "should define validateImageSize method"() {
        when:
        Method method = IImageProcessingService.getMethod("validateImageSize", byte[].class)

        then:
        method != null
        method.returnType == boolean.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == byte[].class
    }

    def "should define processImage method"() {
        when:
        Method method = IImageProcessingService.getMethod("processImage", byte[].class)

        then:
        method != null
        method.returnType == ImageProcessingResult.class
        method.parameterTypes.length == 1
        method.parameterTypes[0] == byte[].class
    }

    def "should be a proper interface"() {
        when:
        def serviceInterface = IImageProcessingService

        then:
        serviceInterface.isInterface()
        serviceInterface.name == "finance.services.IImageProcessingService"
        serviceInterface.package.name == "finance.services"
    }

    def "should have all required methods defined"() {
        when:
        def methods = IImageProcessingService.getDeclaredMethods()
        def methodNames = methods.collect { it.name }

        then:
        methodNames.containsAll([
            "createThumbnail",
            "getImageFormatType",
            "validateImageSize",
            "processImage"
        ])
    }

    def "should have proper method contract"() {
        when:
        def methods = IImageProcessingService.getDeclaredMethods()

        then:
        methods.size() == 4  // Expected number of interface methods
        methods.every { method ->
            // All methods should be abstract (interface methods)
            method.isDefault() == false
        }
    }
}

class ImageProcessingResultSpec extends Specification {

    def "should create ImageProcessingResult with all properties"() {
        given:
        def format = ImageFormatType.Jpeg
        def thumbnail = new byte[100]
        def isValid = true
        def originalSize = 1024
        def thumbnailSize = 100

        when:
        def result = new ImageProcessingResult(format, thumbnail, isValid, originalSize, thumbnailSize)

        then:
        result.format == format
        result.thumbnail == thumbnail
        result.isValid == isValid
        result.originalSize == originalSize
        result.thumbnailSize == thumbnailSize
    }

    def "should implement equals correctly for identical objects"() {
        given:
        def thumbnail = new byte[100]
        def result1 = new ImageProcessingResult(ImageFormatType.Png, thumbnail, true, 1024, 100)
        def result2 = new ImageProcessingResult(ImageFormatType.Png, thumbnail, true, 1024, 100)

        when:
        def areEqual = result1.equals(result2)

        then:
        areEqual == true
    }

    def "should implement equals correctly for different objects"() {
        given:
        def thumbnail1 = new byte[100]
        def thumbnail2 = new byte[200]
        def result1 = new ImageProcessingResult(ImageFormatType.Png, thumbnail1, true, 1024, 100)
        def result2 = new ImageProcessingResult(ImageFormatType.Jpeg, thumbnail2, false, 2048, 200)

        when:
        def areEqual = result1.equals(result2)

        then:
        areEqual == false
    }

    def "should handle null in equals"() {
        given:
        def result = new ImageProcessingResult(ImageFormatType.Png, new byte[100], true, 1024, 100)

        when:
        def areEqual = result.equals(null)

        then:
        areEqual == false
    }

    def "should handle same reference in equals"() {
        given:
        def result = new ImageProcessingResult(ImageFormatType.Png, new byte[100], true, 1024, 100)

        when:
        def areEqual = result.equals(result)

        then:
        areEqual == true
    }

    def "should implement consistent hashCode"() {
        given:
        def thumbnail = new byte[100]
        def result1 = new ImageProcessingResult(ImageFormatType.Png, thumbnail, true, 1024, 100)
        def result2 = new ImageProcessingResult(ImageFormatType.Png, thumbnail, true, 1024, 100)

        when:
        def hash1 = result1.hashCode()
        def hash2 = result2.hashCode()

        then:
        hash1 == hash2
    }

    def "should generate different hashCodes for different objects"() {
        given:
        def result1 = new ImageProcessingResult(ImageFormatType.Png, new byte[100], true, 1024, 100)
        def result2 = new ImageProcessingResult(ImageFormatType.Jpeg, new byte[200], false, 2048, 200)

        when:
        def hash1 = result1.hashCode()
        def hash2 = result2.hashCode()

        then:
        hash1 != hash2
    }
}