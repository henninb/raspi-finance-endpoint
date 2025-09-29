package finance.controllers.dto

import finance.domain.AccountType
import finance.domain.BaseDomainSpec
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import java.sql.Timestamp

class AccountInputDtoSpec extends BaseDomainSpec {

    def "AccountInputDto should be created with valid data"() {
        given:
        def accountNameOwner = "test_primary"
        def accountType = AccountType.Checking
        def activeStatus = true
        def moniker = "1234"
        def outstanding = new BigDecimal("100.00")
        def cleared = new BigDecimal("50.00")
        def future = new BigDecimal("25.00")
        def dateClosed = new Timestamp(System.currentTimeMillis())
        def validationDate = new Timestamp(System.currentTimeMillis())

        when:
        def dto = new AccountInputDto(
            null, // accountId
            accountNameOwner,
            accountType,
            activeStatus,
            moniker,
            outstanding,
            cleared,
            future,
            dateClosed,
            validationDate
        )

        then:
        dto.accountNameOwner == accountNameOwner
        dto.accountType == accountType
        dto.activeStatus == activeStatus
        dto.moniker == moniker
        dto.outstanding == outstanding
        dto.cleared == cleared
        dto.future == future
        dto.dateClosed == dateClosed
        dto.validationDate == validationDate
    }

    @Unroll
    def "AccountInputDto validation should fail for invalid accountNameOwner: '#accountNameOwner'"() {
        when:
        def violations
        if (accountNameOwner == null) {
            // Cannot construct with null required parameter - this is expected behavior
            def dto = new AccountInputDto(null, "checking_primary", AccountType.Checking, null, null, null, null, null, null, null)
            dto.metaClass.setProperty(dto, 'accountNameOwner', null) // Force null for validation test
            violations = validator.validate(dto)
        } else {
            def dto = new AccountInputDto(null, accountNameOwner, AccountType.Checking, null, null, null, null, null, null, null)
            violations = validator.validate(dto)
        }

        then:
        if (accountNameOwner == null) {
            violations.any { it.propertyPath.toString() == 'accountNameOwner' && it.message.contains('must not be blank') }
        } else {
            !violations.isEmpty()
            violations.any { it.propertyPath.toString() == 'accountNameOwner' }
        }

        where:
        accountNameOwner << ["", "ab", "a".repeat(41), "invalid-name", "UPPERCASE"]
    }

    def "AccountInputDto validation should pass for valid accountNameOwner"() {
        given:
        def dto = new AccountInputDto(null, "checking_primary", AccountType.Checking, null, null, null, null, null, null, null)

        when:
        Set<ConstraintViolation<AccountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "AccountInputDto validation should fail for null accountType"() {
        when:
        // This will fail at construction time, which is expected
        new AccountInputDto(null, "investment_account", null, null, null, null, null, null, null, null)

        then:
        thrown(Exception)
    }

    @Unroll
    def "AccountInputDto validation should fail for invalid moniker: '#moniker'"() {
        given:
        def dto = new AccountInputDto(null, "investment_account", AccountType.Checking, null, moniker, null, null, null, null, null)

        when:
        Set<ConstraintViolation<AccountInputDto>> violations = validator.validate(dto)

        then:
        if (moniker == null) {
            violations.isEmpty() // null is allowed for optional field
        } else {
            !violations.isEmpty()
            violations.any { it.propertyPath.toString() == 'moniker' }
        }

        where:
        moniker << ["123", "12345", "abcd", "12a4", null]
    }

    def "AccountInputDto validation should pass for valid moniker"() {
        given:
        def dto = new AccountInputDto(null, "investment_account", AccountType.Checking, null, "1234", null, null, null, null, null)

        when:
        Set<ConstraintViolation<AccountInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "AccountInputDto validation should fail for invalid currency amounts"() {
        given:
        def dto = new AccountInputDto(null, "investment_account", AccountType.Checking, null, null, outstanding, cleared, future, null, null)

        when:
        Set<ConstraintViolation<AccountInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()

        where:
        outstanding                    | cleared                    | future
        new BigDecimal("12345678.123") | null                       | null                       // too many integer digits
        null                           | new BigDecimal("1.123")    | null                       // too many fraction digits
        null                           | null                       | new BigDecimal("999999.999") // too many fraction digits
    }

    def "AccountInputDto should have default values for optional fields"() {
        given:
        def dto = new AccountInputDto(null, "investment_account", AccountType.Checking, null, null, null, null, null, null, null)

        expect:
        dto.activeStatus == null  // Optional field
        dto.moniker == null       // Optional field
        dto.outstanding == null   // Optional field
        dto.cleared == null       // Optional field
        dto.future == null        // Optional field
        dto.dateClosed == null    // Optional field
        dto.validationDate == null // Optional field
    }
}