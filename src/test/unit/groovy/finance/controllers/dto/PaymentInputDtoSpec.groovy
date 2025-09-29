package finance.controllers.dto

import finance.domain.BaseDomainSpec
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import java.sql.Date

class PaymentInputDtoSpec extends BaseDomainSpec {

    def "PaymentInputDto should be created with valid data"() {
        given:
        def sourceAccount = "checking_primary"
        def destinationAccount = "bills_payable"
        def transactionDate = Date.valueOf("2024-01-15")
        def amount = new BigDecimal("100.00")
        def guidSource = "12345678-1234-1234-1234-123456789012"
        def guidDestination = "87654321-4321-4321-4321-210987654321"
        def activeStatus = true

        when:
        def dto = new PaymentInputDto(
            null,                    // paymentId
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
        dto.paymentId == null
    }

    def "PaymentInputDto validation should pass for valid minimal data"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "PaymentInputDto validation should fail for invalid sourceAccount: '#sourceAccount'"() {
        given:
        def dto = new PaymentInputDto(
            null,
            sourceAccount,
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'sourceAccount' }

        where:
        sourceAccount << ["", "   "]
    }

    @Unroll
    def "PaymentInputDto validation should fail for invalid destinationAccount: '#destinationAccount'"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            destinationAccount,
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'destinationAccount' }

        where:
        destinationAccount << ["", "   "]
    }

    def "PaymentInputDto constructor should fail for null transactionDate"() {
        when:
        def exception = null
        try {
            new PaymentInputDto(
                null,
                "checking_primary",
                "bills_payable",
                null,
                new BigDecimal("100.00"),
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
    def "PaymentInputDto validation should fail for invalid amount: '#amount'"() {
        when:
        def violations
        if (amount == null) {
            // Cannot construct with null amount - constructor will fail
            def exception = null
            try {
                new PaymentInputDto(null, "checking_primary", "bills_payable", Date.valueOf("2024-01-15"), null, null, null, null)
            } catch (Exception e) {
                exception = e
            }
            assert exception != null
            return
        } else {
            def dto = new PaymentInputDto(null, "checking_primary", "bills_payable", Date.valueOf("2024-01-15"), amount, null, null, null)
            violations = validator.validate(dto)
        }

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'amount' }

        where:
        amount << [null, new BigDecimal("0.00"), new BigDecimal("-1.00"), new BigDecimal("-100.00")]
    }

    def "PaymentInputDto validation should pass for minimum valid amount"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("0.01"),
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "PaymentInputDto validation should fail for invalid guidSource: '#guidSource'"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            guidSource,
            null,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guidSource' }

        where:
        guidSource << [
            "",
            "invalid-guid",
            "12345678-1234-1234-1234-12345678901", // too short
            "12345678-1234-1234-1234-1234567890123", // too long
            "12345678-1234-1234-1234-12345678901G", // invalid character
            "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"  // uppercase
        ]
    }

    @Unroll
    def "PaymentInputDto validation should fail for invalid guidDestination: '#guidDestination'"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            guidDestination,
            null
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guidDestination' }

        where:
        guidDestination << [
            "",
            "invalid-guid",
            "87654321-4321-4321-4321-21098765432", // too short
            "87654321-4321-4321-4321-2109876543213", // too long
            "87654321-4321-4321-4321-21098765432G", // invalid character
            "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"  // uppercase
        ]
    }

    def "PaymentInputDto validation should pass for valid GUIDs"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            "12345678-1234-1234-1234-123456789012",
            "87654321-4321-4321-4321-210987654321",
            true
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "PaymentInputDto validation should pass when GUIDs are null"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            true
        )

        when:
        Set<ConstraintViolation<PaymentInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "PaymentInputDto should have default values for optional fields"() {
        given:
        def dto = new PaymentInputDto(
            null,
            "checking_primary",
            "bills_payable",
            Date.valueOf("2024-01-15"),
            new BigDecimal("100.00"),
            null,
            null,
            null
        )

        expect:
        dto.paymentId == null
        dto.guidSource == null
        dto.guidDestination == null
        dto.activeStatus == null
    }
}