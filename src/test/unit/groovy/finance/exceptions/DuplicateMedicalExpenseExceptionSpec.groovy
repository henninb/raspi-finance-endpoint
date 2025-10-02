package finance.exceptions

import spock.lang.Specification

class DuplicateMedicalExpenseExceptionSpec extends Specification {

    def "should create exception with message"() {
        given:
        def message = "Duplicate medical expense for transaction ID 123"

        when:
        def exception = new DuplicateMedicalExpenseException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
    }

    def "should extend RuntimeException"() {
        when:
        def exception = new DuplicateMedicalExpenseException("test message")

        then:
        exception instanceof RuntimeException
        exception instanceof Exception
        exception instanceof Throwable
    }

    def "should be in correct package"() {
        when:
        def packageName = DuplicateMedicalExpenseException.package.name

        then:
        packageName == "finance.exceptions"
    }

    def "should have proper class name"() {
        when:
        def className = DuplicateMedicalExpenseException.simpleName

        then:
        className == "DuplicateMedicalExpenseException"
    }

    def "should require message parameter"() {
        when:
        def exception = new DuplicateMedicalExpenseException("Test message")

        then:
        exception.message == "Test message"
        exception instanceof DuplicateMedicalExpenseException
    }

    def "should handle empty message"() {
        when:
        def exception = new DuplicateMedicalExpenseException("")

        then:
        exception.message == ""
        exception instanceof DuplicateMedicalExpenseException
    }

    def "should be throwable"() {
        given:
        def exception = new DuplicateMedicalExpenseException("Test exception")

        when:
        def thrownException = null
        try {
            throw exception
        } catch (DuplicateMedicalExpenseException e) {
            thrownException = e
        }

        then:
        thrownException != null
        thrownException.message == "Test exception"
        thrownException instanceof DuplicateMedicalExpenseException
    }

    def "should maintain stack trace"() {
        when:
        def exception = new DuplicateMedicalExpenseException("Test exception")

        then:
        exception.stackTrace != null
        exception.stackTrace.length > 0
    }

    def "should be serializable"() {
        given:
        def exception = new DuplicateMedicalExpenseException("Serialization test")

        when:
        def serializable = exception instanceof Serializable

        then:
        serializable == true
    }
}