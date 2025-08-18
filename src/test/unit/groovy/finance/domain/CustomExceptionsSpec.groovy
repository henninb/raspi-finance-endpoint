package finance.domain

import spock.lang.Specification
import spock.lang.Unroll

class CustomExceptionsSpec extends Specification {

    @Unroll
    def "test #exceptionClass creation with message"() {
        given:
        String message = "Test error message"

        when:
        RuntimeException exception = exceptionClass.newInstance(message)

        then:
        exception.message == message
        exception instanceof RuntimeException

        where:
        exceptionClass << [
            TransactionNotFoundException,
            ReceiptImageException,
            InvalidTransactionStateException,
            TransactionValidationException,
            AccountValidationException,
            InvalidReoccurringTypeException
        ]
    }

    def "test TransactionNotFoundException specific functionality"() {
        given:
        String transactionId = "tx-123-not-found"
        String message = "Transaction not found: $transactionId"

        when:
        TransactionNotFoundException exception = new TransactionNotFoundException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.TransactionNotFoundException"
    }

    def "test ReceiptImageException specific functionality"() {
        given:
        String message = "Failed to process receipt image: invalid format"

        when:
        ReceiptImageException exception = new ReceiptImageException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.ReceiptImageException"
    }

    def "test InvalidTransactionStateException specific functionality"() {
        given:
        String message = "Invalid state transition from CLEARED to FUTURE"

        when:
        InvalidTransactionStateException exception = new InvalidTransactionStateException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.InvalidTransactionStateException"
    }

    def "test TransactionValidationException specific functionality"() {
        given:
        String message = "Transaction validation failed: amount cannot be negative"

        when:
        TransactionValidationException exception = new TransactionValidationException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.TransactionValidationException"
    }

    def "test AccountValidationException specific functionality"() {
        given:
        String message = "Account validation failed: invalid account name format"

        when:
        AccountValidationException exception = new AccountValidationException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.AccountValidationException"
    }

    def "test InvalidReoccurringTypeException specific functionality"() {
        given:
        String message = "Invalid reoccurring type: UNKNOWN_TYPE"

        when:
        InvalidReoccurringTypeException exception = new InvalidReoccurringTypeException(message)

        then:
        exception.message == message
        exception instanceof RuntimeException
        exception.getClass().name == "finance.domain.InvalidReoccurringTypeException"
    }

    @Unroll
    def "test #exceptionClass with empty message"() {
        when:
        RuntimeException exception = exceptionClass.newInstance("")

        then:
        exception.message == ""
        exception instanceof RuntimeException

        where:
        exceptionClass << [
            TransactionNotFoundException,
            ReceiptImageException,
            InvalidTransactionStateException
        ]
    }

    @Unroll
    def "test #exceptionClass can be thrown and caught"() {
        when:
        throw exceptionClass.newInstance("Test exception")

        then:
        RuntimeException ex = thrown()
        ex.getClass() == exceptionClass
        ex.message == "Test exception"

        where:
        exceptionClass << [
            TransactionNotFoundException,
            ReceiptImageException,
            InvalidTransactionStateException,
            TransactionValidationException,
            AccountValidationException,
            InvalidReoccurringTypeException
        ]
    }

    def "test exception hierarchy"() {
        when:
        TransactionNotFoundException exception = new TransactionNotFoundException("test")

        then:
        exception instanceof RuntimeException
        exception instanceof Exception
        exception instanceof Throwable
        !(exception instanceof Error)
    }

    @Unroll
    def "test #exceptionClass toString contains message"() {
        given:
        String message = "Detailed error message for testing"
        RuntimeException exception = exceptionClass.newInstance(message)

        when:
        String result = exception.toString()

        then:
        result.contains(message)
        result.contains(exceptionClass.simpleName)

        where:
        exceptionClass << [
            TransactionNotFoundException,
            ReceiptImageException,
            InvalidTransactionStateException
        ]
    }
}