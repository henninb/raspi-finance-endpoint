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

    def "CategoryInputDto equals and hashCode for identical values"() {
        given:
        def dto1 = new CategoryInputDto(1L, "groceries", true)
        def dto2 = new CategoryInputDto(1L, "groceries", true)

        expect:
        dto1 == dto2
        dto1.hashCode() == dto2.hashCode()
    }

    def "CategoryInputDto inequality when categoryName differs"() {
        given:
        def dto1 = new CategoryInputDto(null, "groceries", true)
        def dto2 = new CategoryInputDto(null, "utilities", true)

        expect:
        dto1 != dto2
    }

    def "CategoryInputDto toString contains categoryName"() {
        given:
        def dto = new CategoryInputDto(5L, "groceries", true)

        expect:
        dto.toString() != null
        dto.toString().contains("groceries")
    }

    def "CategoryInputDto copy produces modified instance"() {
        given:
        def original = new CategoryInputDto(1L, "groceries", true)

        when:
        def copy = original.copy(1L, "utilities", true)

        then:
        copy.categoryName == "utilities"
        original.categoryName == "groceries"
    }
}
