package finance.controllers.dto

import finance.domain.BaseDomainSpec
import finance.domain.TransactionState
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation
import java.sql.Timestamp

class ValidationAmountInputDtoSpec extends BaseDomainSpec {

    def "ValidationAmountInputDto should be created with valid data"() {
        given:
        def accountId = 1L
        def validationDate = new Timestamp(System.currentTimeMillis())
        def transactionState = TransactionState.Cleared
        def amount = new BigDecimal("100.00")

        when:
        def dto = new ValidationAmountInputDto(
            null,              // validationId
            accountId,         // accountId
            validationDate,    // validationDate
            true,              // activeStatus
            transactionState,  // transactionState
            amount             // amount
        )

        then:
        dto.validationId == null
        dto.accountId == accountId
        dto.validationDate == validationDate
        dto.activeStatus == true
        dto.transactionState == transactionState
        dto.amount == amount
    }

    def "ValidationAmountInputDto constructor should fail for null accountId"() {
        when:
        def exception = null
        try {
            new ValidationAmountInputDto(
                null,
                null,  // accountId is null - constructor should fail
                new Timestamp(System.currentTimeMillis()),
                true,
                TransactionState.Cleared,
                new BigDecimal("100.00")
            )
        } catch (Exception e) {
            exception = e
        }

        then:
        exception != null
    }

    def "ValidationAmountInputDto constructor should fail for null validationDate"() {
        when:
        def exception = null
        try {
            new ValidationAmountInputDto(
                null,
                1L,
                null,  // validationDate is null - constructor should fail
                true,
                TransactionState.Cleared,
                new BigDecimal("100.00")
            )
        } catch (Exception e) {
            exception = e
        }

        then:
        exception != null
    }

    def "ValidationAmountInputDto constructor should fail for null transactionState"() {
        when:
        def exception = null
        try {
            new ValidationAmountInputDto(
                null,
                1L,
                new Timestamp(System.currentTimeMillis()),
                true,
                null,  // transactionState is null - constructor should fail
                new BigDecimal("100.00")
            )
        } catch (Exception e) {
            exception = e
        }

        then:
        exception != null
    }

    def "ValidationAmountInputDto constructor should fail for null amount"() {
        when:
        def exception = null
        try {
            new ValidationAmountInputDto(
                null,
                1L,
                new Timestamp(System.currentTimeMillis()),
                true,
                TransactionState.Cleared,
                null  // amount is null - constructor should fail
            )
        } catch (Exception e) {
            exception = e
        }

        then:
        exception != null
    }

    @Unroll
    def "ValidationAmountInputDto validation should fail for invalid amount: #amount"() {
        when:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            amount
        )
        def violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'amount' }

        where:
        amount << [
            new BigDecimal("0.00"),           // Zero (must be > 0)
            new BigDecimal("-100.00"),        // Negative
            new BigDecimal("123456789.00")    // Too many digits (max 8 integer digits)
        ]
    }

    def "ValidationAmountInputDto validation should pass for valid amount with two decimal places"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ValidationAmountInputDto validation should pass for minimum valid amount"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("0.01")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ValidationAmountInputDto validation should pass for large valid amount"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Cleared,
            new BigDecimal("99999999.99")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "ValidationAmountInputDto validation should pass for all TransactionState values: #state"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            state,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()

        where:
        state << [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future, TransactionState.Undefined]
    }

    def "ValidationAmountInputDto should have default values for optional fields"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            null,              // activeStatus is optional
            TransactionState.Cleared,
            new BigDecimal("100.00")
        )

        expect:
        dto.activeStatus == null  // Optional field, no default in DTO
        dto.validationId == null  // Optional field
    }

    def "ValidationAmountInputDto validation should pass when activeStatus is explicitly null"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            null,              // activeStatus explicitly null
            TransactionState.Cleared,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty() || violations.every { it.propertyPath.toString() != 'activeStatus' }
    }

    def "ValidationAmountInputDto validation should pass with all valid fields including validationId"() {
        given:
        def dto = new ValidationAmountInputDto(
            123L,              // validationId provided (for updates)
            1L,
            new Timestamp(System.currentTimeMillis()),
            false,
            TransactionState.Outstanding,
            new BigDecimal("250.50")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ValidationAmountInputDto validation should pass for Future transaction state"() {
        given:
        def dto = new ValidationAmountInputDto(
            null,
            1L,
            new Timestamp(System.currentTimeMillis()),
            true,
            TransactionState.Future,
            new BigDecimal("500.00")
        )

        when:
        Set<ConstraintViolation<ValidationAmountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ValidationAmountInputDto should properly handle Groovy-Kotlin constructor interop with all parameters"() {
        given: "All constructor parameters provided in order"
        def validationId = 999L
        def accountId = 5L
        def validationDate = new Timestamp(System.currentTimeMillis())
        def activeStatus = true
        def transactionState = TransactionState.Cleared
        def amount = new BigDecimal("1234.56")

        when: "DTO is constructed with all parameters"
        def dto = new ValidationAmountInputDto(
            validationId,
            accountId,
            validationDate,
            activeStatus,
            transactionState,
            amount
        )

        then: "All fields are properly set"
        dto.validationId == validationId
        dto.accountId == accountId
        dto.validationDate == validationDate
        dto.activeStatus == activeStatus
        dto.transactionState == transactionState
        dto.amount == amount
    }
}
