package finance.controllers.dto

import finance.domain.BaseDomainSpec
import jakarta.validation.ConstraintViolation
import spock.lang.Unroll

class CategoryInputDtoSpec extends BaseDomainSpec {

    def "CategoryInputDto created with valid data"() {
        when:
        def dto = new CategoryInputDto(null, "groceries", true)

        then:
        dto.categoryName == "groceries"
        dto.activeStatus == true
        dto.categoryId == null
    }

    def "CategoryInputDto validation passes for valid category name"() {
        given:
        def dto = new CategoryInputDto(null, "groceries", null)

        when:
        Set<ConstraintViolation<CategoryInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "CategoryInputDto validation fails for invalid categoryName: '#name'"() {
        given:
        def dto = new CategoryInputDto(null, name, null)

        when:
        Set<ConstraintViolation<CategoryInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'categoryName' }

        where:
        name << ["", "a".repeat(51), "has spaces", "UPPERCASE"]
    }

    def "CategoryInputDto validation passes for minimum length name"() {
        given:
        def dto = new CategoryInputDto(null, "a", null)

        when:
        Set<ConstraintViolation<CategoryInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "CategoryInputDto validation passes for max length name"() {
        given:
        def dto = new CategoryInputDto(null, "a" * 50, null)

        when:
        Set<ConstraintViolation<CategoryInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "CategoryInputDto has correct optional fields"() {
        when:
        def dto = new CategoryInputDto(null, "food", null)

        then:
        dto.categoryId == null
        dto.activeStatus == null
        dto.categoryName == "food"
    }

    def "CategoryInputDto with id and activeStatus"() {
        when:
        def dto = new CategoryInputDto(10L, "utilities", false)

        then:
        dto.categoryId == 10L
        dto.categoryName == "utilities"
        dto.activeStatus == false
    }
}
