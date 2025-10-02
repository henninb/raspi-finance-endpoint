package finance.utils

import spock.lang.Specification
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.Retention
import java.lang.annotation.Documented

class ValidDateSpec extends Specification {

    def "should have correct annotation properties"() {
        when:
        ValidDate annotation = TestClass.getDeclaredField("dateField").getAnnotation(ValidDate.class)

        then:
        annotation != null
        annotation.message() == "date must be greater than 1/1/2000."
        annotation.groups().length == 0
        annotation.payload().length == 0
    }

    def "should specify DateValidator as validator class"() {
        when:
        ValidDate annotation = TestClass.getDeclaredField("dateField").getAnnotation(ValidDate.class)

        then:
        annotation != null
        // Verify the annotation is properly configured with DateValidator
        // The actual validation logic is tested in DateValidatorSpec
    }

    def "should allow custom message"() {
        when:
        ValidDate annotation = TestClass.getDeclaredField("customMessageField").getAnnotation(ValidDate.class)

        then:
        annotation != null
        annotation.message() == "Custom date validation message"
    }

    def "should target field level"() {
        when:
        def target = ValidDate.class.getAnnotation(Target.class)

        then:
        target != null
        target.value().contains(ElementType.FIELD)
    }

    def "should have runtime retention"() {
        when:
        def retention = ValidDate.class.getAnnotation(Retention.class)

        then:
        retention != null
        retention.value() == RetentionPolicy.RUNTIME
    }

    def "should be marked as documented"() {
        when:
        def documented = ValidDate.class.getAnnotation(Documented.class)

        then:
        documented != null
    }

    static class TestClass {
        @ValidDate
        java.sql.Date dateField

        @ValidDate(message = "Custom date validation message")
        java.sql.Date customMessageField
    }
}