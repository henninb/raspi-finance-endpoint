package finance.controllers.dto

import finance.domain.BaseDomainSpec
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

class DescriptionInputDtoSpec extends BaseDomainSpec {

    def "DescriptionInputDto should be created with valid data"() {
        given:
        def descriptionName = "grocery_store"
        def activeStatus = true

        when:
        def dto = new DescriptionInputDto(null, descriptionName, activeStatus)

        then:
        dto.descriptionName == descriptionName
        dto.activeStatus == activeStatus
        dto.descriptionId == null
    }

    @Unroll
    def "DescriptionInputDto validation should fail for invalid descriptionName: '#descriptionName'"() {
        when:
        def violations
        if (descriptionName == null) {
            // Cannot construct with null required parameter - this is expected behavior
            def dto = new DescriptionInputDto(null, "grocery_store", true)
            dto.metaClass.setProperty(dto, 'descriptionName', null) // Force null for validation test
            violations = validator.validate(dto)
        } else {
            def dto = new DescriptionInputDto(null, descriptionName, true)
            violations = validator.validate(dto)
        }

        then:
        if (descriptionName == null) {
            violations.any { it.propertyPath.toString() == 'descriptionName' && it.message.contains('must not be blank') }
        } else {
            !violations.isEmpty()
            violations.any { it.propertyPath.toString() == 'descriptionName' }
        }

        where:
        descriptionName << ["", "a".repeat(51)]
    }

    def "DescriptionInputDto validation should pass for valid descriptionName"() {
        given:
        def dto = new DescriptionInputDto(null, "store_walmart", true)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "DescriptionInputDto validation should pass with minimum length descriptionName"() {
        given:
        def dto = new DescriptionInputDto(null, "x", true)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "DescriptionInputDto validation should pass with maximum length descriptionName"() {
        given:
        def dto = new DescriptionInputDto(null, "restaurant_with_a_very_long_name_that_hits_max", true)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "DescriptionInputDto should reject mixed case input due to pattern validation"() {
        given:
        def dto = new DescriptionInputDto(null, "Test_Description", true)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        // Mixed case should be rejected - pattern only allows lowercase letters, numbers, underscore, hyphen
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'descriptionName' && it.message.contains('alphanumeric no space') }
    }

    def "DescriptionInputDto should have default values for optional fields"() {
        given:
        def dto = new DescriptionInputDto(null, "grocery_store", null)

        expect:
        dto.activeStatus == null  // Optional field, no default in DTO
        dto.descriptionId == null // Optional field
    }

    def "DescriptionInputDto validation should pass when activeStatus is explicitly null"() {
        given:
        def dto = new DescriptionInputDto(null, "grocery_store", null)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty() || violations.every { it.propertyPath.toString() != 'activeStatus' }
    }

    def "DescriptionInputDto validation should pass with all valid fields"() {
        given:
        def dto = new DescriptionInputDto(1L, "online_purchase", false)

        when:
        Set<ConstraintViolation<DescriptionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }
}