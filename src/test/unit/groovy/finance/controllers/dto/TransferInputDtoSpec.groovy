package finance.controllers.dto

import finance.domain.BaseDomainSpec
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import java.sql.Date

class TransferInputDtoSpec extends BaseDomainSpec {

    def "TransferInputDto should be created with valid data"() {
        given:
        def sourceAccount = "checking_primary"
        def destinationAccount = "savings_primary"
        def transactionDate = Date.valueOf("2024-01-15")
        def amount = new BigDecimal("250.00")
        def guidSource = "11111111-2222-3333-4444-555555555555"
        def guidDestination = "66666666-7777-8888-9999-000000000000"
        def activeStatus = false

        when:
        def dto = new TransferInputDto(
            null,                    // transferId
            sourceAccount,           // sourceAccount
            destinationAccount,      // destinationAccount
            transactionDate,         // transactionDate
            amount,                  // amount
            guidSource,              // guidSource
            guidDestination,         // guidDestination
            activeStatus             // activeStatus
        )

        then:
        dto.sourceAccount == sourceAccount
        dto.destinationAccount == destinationAccount
        dto.transactionDate == transactionDate
        dto.amount == amount
        dto.guidSource == guidSource
        dto.guidDestination == guidDestination
        dto.activeStatus == activeStatus
        dto.transferId == null
    }

    def "TransferInputDto validation should pass for valid minimal data"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "TransferInputDto validation should fail for invalid sourceAccount: '#sourceAccount'"() {
        given:
        def dto = new TransferInputDto(
            null,
            sourceAccount,
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'sourceAccount' }

        where:
        sourceAccount << ["", "   "]
    }

    @Unroll
    def "TransferInputDto validation should fail for invalid destinationAccount: '#destinationAccount'"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            destinationAccount,
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'destinationAccount' }

        where:
        destinationAccount << ["", "   "]
    }

    def "TransferInputDto constructor should fail for null transactionDate"() {
        when:
        def exception = null
        try {
            new TransferInputDto(
                null,
                "checking_primary",
                "savings_primary",
                null,
                new BigDecimal("200.00"),
                null,
                null,
                null
            )
        } catch (Exception e) {
            exception = e
        }

        then:
        exception != null
    }

    @Unroll
    def "TransferInputDto validation should fail for invalid amount: '#amount'"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            amount,
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'amount' }

        where:
        amount << [new BigDecimal("0.00"), new BigDecimal("-1.00"), new BigDecimal("-500.00")]
    }

    def "TransferInputDto validation should pass for minimum valid amount"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("0.01"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "TransferInputDto validation should fail for invalid guidSource: '#guidSource'"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            guidSource,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guidSource' }

        where:
        guidSource << [
            "",
            "not-a-guid",
            "11111111-2222-3333-4444-55555555555", // too short
            "11111111-2222-3333-4444-5555555555555", // too long
            "11111111-2222-3333-4444-55555555555X", // invalid character
            "FFFFFFFF-EEEE-DDDD-CCCC-BBBBBBBBBBBB"  // uppercase
        ]
    }

    @Unroll
    def "TransferInputDto validation should fail for invalid guidDestination: '#guidDestination'"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            guidDestination,
            null
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guidDestination' }

        where:
        guidDestination << [
            "",
            "bad-guid-format",
            "66666666-7777-8888-9999-00000000000", // too short
            "66666666-7777-8888-9999-0000000000000", // too long
            "66666666-7777-8888-9999-00000000000Z", // invalid character
            "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"  // uppercase
        ]
    }

    def "TransferInputDto validation should pass for valid GUIDs"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            "11111111-2222-3333-4444-555555555555",
            "66666666-7777-8888-9999-000000000000",
            true
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "TransferInputDto validation should pass when GUIDs are null"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            null,
            false
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "TransferInputDto should have default values for optional fields"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("200.00"),
            null,
            null,
            null
        )

        expect:
        dto.transferId == null
        dto.guidSource == null
        dto.guidDestination == null
        dto.activeStatus == null
    }

    def "TransferInputDto validation should pass for large amounts"() {
        given:
        def dto = new TransferInputDto(
            null,
            "checking_primary",
            "savings_primary",
            Date.valueOf("2024-01-15"),
            new BigDecimal("99999999.99"),
            null,
            null,
            true
        )

        when:
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }
}