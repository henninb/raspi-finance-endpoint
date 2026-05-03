package finance.controllers.dto

import finance.domain.BaseDomainSpec
import jakarta.validation.ConstraintViolation
import spock.lang.Unroll

class TransactionAccountChangeInputDtoSpec extends BaseDomainSpec {

    def "TransactionAccountChangeInputDto created with valid data"() {
        when:
        def dto = new TransactionAccountChangeInputDto(
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "checking_primary"
        )

        then:
        dto.guid == "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        dto.accountNameOwner == "checking_primary"
    }

    def "TransactionAccountChangeInputDto validation passes for valid data"() {
        given:
        def dto = new TransactionAccountChangeInputDto(
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "checking_primary"
        )

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "TransactionAccountChangeInputDto fails validation for invalid guid: '#guid'"() {
        given:
        def dto = new TransactionAccountChangeInputDto(guid, "checking_primary")

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guid' }

        where:
        guid << ["", "not-a-uuid", "UPPERCASE-GUID", "123", "abc-def-ghi-jkl"]
    }

    @Unroll
    def "TransactionAccountChangeInputDto fails validation for invalid accountNameOwner: '#name'"() {
        given:
        def dto = new TransactionAccountChangeInputDto(
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            name
        )

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'accountNameOwner' }

        where:
        name << ["", "ab", "a".repeat(41), "UPPERCASE_NAME", "double__underscore", "has spaces"]
    }

    def "TransactionAccountChangeInputDto fails validation when guid is blank"() {
        given:
        def dto = new TransactionAccountChangeInputDto("   ", "checking_primary")

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guid' }
    }

    def "TransactionAccountChangeInputDto fails validation when accountNameOwner is blank"() {
        given:
        def dto = new TransactionAccountChangeInputDto(
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            "   "
        )

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'accountNameOwner' }
    }

    def "TransactionAccountChangeInputDto validation passes for various valid account names"() {
        given:
        def dto = new TransactionAccountChangeInputDto(
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            accountName
        )

        when:
        Set<ConstraintViolation<TransactionAccountChangeInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()

        where:
        accountName << ["checking_primary", "savings_joint", "visa_reward", "amex_platinum"]
    }
}
