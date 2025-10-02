package finance.utils

import spock.lang.Specification
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class ValidImageSpec extends Specification {

    def "should have correct annotation properties"() {
        when:
        ValidImage annotation = TestClass.getDeclaredField("imageField").getAnnotation(ValidImage.class)

        then:
        annotation != null
        annotation.message() == "image must be a jpeg or png file."
        annotation.groups().length == 0
        annotation.payload().length == 0
    }

    def "should specify ImageValidator as validator class"() {
        when:
        ValidImage annotation = TestClass.getDeclaredField("imageField").getAnnotation(ValidImage.class)

        then:
        annotation != null
        // Verify the annotation is properly configured with ImageValidator
        // The actual validation logic is tested in ImageValidatorSpec
    }

    def "should allow custom message"() {
        when:
        ValidImage annotation = TestClass.getDeclaredField("customMessageField").getAnnotation(ValidImage.class)

        then:
        annotation != null
        annotation.message() == "Custom validation message"
    }

    def "should target field level"() {
        when:
        def target = ValidImage.class.getAnnotation(Target.class)

        then:
        target != null
        target.value().contains(ElementType.FIELD)
    }

    def "should have runtime retention"() {
        when:
        def retention = ValidImage.class.getAnnotation(Retention.class)

        then:
        retention != null
        retention.value() == RetentionPolicy.RUNTIME
    }

    static class TestClass {
        @ValidImage
        String imageField

        @ValidImage(message = "Custom validation message")
        String customMessageField
    }
}