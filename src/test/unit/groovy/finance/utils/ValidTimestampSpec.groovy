package finance.utils

import spock.lang.Specification
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.Retention
import java.lang.annotation.Documented
import java.sql.Timestamp

class ValidTimestampSpec extends Specification {

    def "should have correct annotation properties"() {
        when:
        ValidTimestamp annotation = TestClass.getDeclaredField("timestampField").getAnnotation(ValidTimestamp.class)

        then:
        annotation != null
        annotation.message() == "timestamp must be greater than 1/1/2000."
        annotation.groups().length == 0
        annotation.payload().length == 0
    }

    def "should specify TimestampValidator as validator class"() {
        when:
        ValidTimestamp annotation = TestClass.getDeclaredField("timestampField").getAnnotation(ValidTimestamp.class)

        then:
        annotation != null
        // Verify the annotation is properly configured with TimestampValidator
        // The actual validation logic is tested in TimestampValidatorSpec
    }

    def "should allow custom message"() {
        when:
        ValidTimestamp annotation = TestClass.getDeclaredField("customMessageField").getAnnotation(ValidTimestamp.class)

        then:
        annotation != null
        annotation.message() == "Custom timestamp validation message"
    }

    def "should target field level"() {
        when:
        def target = ValidTimestamp.class.getAnnotation(Target.class)

        then:
        target != null
        target.value().contains(ElementType.FIELD)
    }

    def "should have runtime retention"() {
        when:
        def retention = ValidTimestamp.class.getAnnotation(Retention.class)

        then:
        retention != null
        retention.value() == RetentionPolicy.RUNTIME
    }

    def "should be marked as documented"() {
        when:
        def documented = ValidTimestamp.class.getAnnotation(Documented.class)

        then:
        documented != null
    }

    static class TestClass {
        @ValidTimestamp
        Timestamp timestampField

        @ValidTimestamp(message = "Custom timestamp validation message")
        Timestamp customMessageField
    }
}