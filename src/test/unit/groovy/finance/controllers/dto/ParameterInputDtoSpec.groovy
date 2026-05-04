package finance.controllers.dto

import finance.domain.BaseDomainSpec
import jakarta.validation.ConstraintViolation
import spock.lang.Unroll

class ParameterInputDtoSpec extends BaseDomainSpec {

    def "ParameterInputDto created with valid data"() {
        when:
        def dto = new ParameterInputDto(null, "setting_one", "value1", true)

        then:
        dto.parameterName == "setting_one"
        dto.parameterValue == "value1"
        dto.activeStatus == true
        dto.parameterId == null
    }

    def "ParameterInputDto validation passes for valid data"() {
        given:
        def dto = new ParameterInputDto(null, "param_name", "paramvalue", null)

        when:
        Set<ConstraintViolation<ParameterInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "ParameterInputDto validation fails for invalid parameterName: '#name'"() {
        given:
        def dto = new ParameterInputDto(null, name, "validvalue", null)

        when:
        Set<ConstraintViolation<ParameterInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'parameterName' }

        where:
        name << ["", "a".repeat(51), "has spaces", "UPPERCASE"]
    }

    @Unroll
    def "ParameterInputDto validation fails for invalid parameterValue: '#value'"() {
        given:
        def dto = new ParameterInputDto(null, "valid_name", value, null)

        when:
        Set<ConstraintViolation<ParameterInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'parameterValue' }

        where:
        value << ["", "a".repeat(51)]
    }

    def "ParameterInputDto validation passes for minimum length fields"() {
        given:
        def dto = new ParameterInputDto(null, "a", "v", null)

        when:
        Set<ConstraintViolation<ParameterInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ParameterInputDto validation passes for max length name"() {
        given:
        def dto = new ParameterInputDto(null, "a" * 50, "value", null)

        when:
        Set<ConstraintViolation<ParameterInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "ParameterInputDto optional fields default to null"() {
        when:
        def dto = new ParameterInputDto(null, "my_param", "myvalue", null)

        then:
        dto.parameterId == null
        dto.activeStatus == null
    }

    def "ParameterInputDto with id and activeStatus"() {
        when:
        def dto = new ParameterInputDto(5L, "feature_flag", "enabled", false)

        then:
        dto.parameterId == 5L
        dto.parameterName == "feature_flag"
        dto.parameterValue == "enabled"
        dto.activeStatus == false
    }

    def "ParameterInputDto equals and hashCode for identical values"() {
        given:
        def dto1 = new ParameterInputDto(1L, "my_param", "myvalue", true)
        def dto2 = new ParameterInputDto(1L, "my_param", "myvalue", true)

        expect:
        dto1 == dto2
        dto1.hashCode() == dto2.hashCode()
    }

    def "ParameterInputDto inequality when parameterValue differs"() {
        given:
        def dto1 = new ParameterInputDto(null, "my_param", "value1", null)
        def dto2 = new ParameterInputDto(null, "my_param", "value2", null)

        expect:
        dto1 != dto2
    }

    def "ParameterInputDto toString contains parameterName"() {
        given:
        def dto = new ParameterInputDto(5L, "feature_flag", "enabled", true)

        expect:
        dto.toString() != null
        dto.toString().contains("feature_flag")
    }

    def "ParameterInputDto copy produces modified instance"() {
        given:
        def original = new ParameterInputDto(1L, "feature_flag", "enabled", true)

        when:
        def copy = original.copy(1L, "feature_flag", "disabled", true)

        then:
        copy.parameterValue == "disabled"
        original.parameterValue == "enabled"
    }
}
