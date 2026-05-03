package finance.controllers.dto

import finance.domain.BaseDomainSpec
import finance.domain.ClaimStatus
import jakarta.validation.ConstraintViolation
import spock.lang.Unroll

import java.time.LocalDate

class MedicalExpenseInputDtoSpec extends BaseDomainSpec {

    private MedicalExpenseInputDto validDto() {
        new MedicalExpenseInputDto(
            null,
            null,
            null,
            null,
            LocalDate.of(2024, 1, 15),
            "Annual checkup",
            null,
            null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            null,
            false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("50.00")
        )
    }

    def "MedicalExpenseInputDto created with valid data passes validation"() {
        given:
        def dto = validDto()

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "MedicalExpenseInputDto fails validation when serviceDate is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            null,
            "Annual checkup",
            null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("50.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'serviceDate' }
    }

    def "MedicalExpenseInputDto fails validation when billedAmount is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            "checkup",
            null, null,
            null,
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("50.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'billedAmount' }
    }

    def "MedicalExpenseInputDto fails validation when billedAmount is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            "checkup",
            null, null,
            new BigDecimal("-1.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("0.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'billedAmount' }
    }

    @Unroll
    def "MedicalExpenseInputDto fails validation for invalid procedureCode: '#code'"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null,
            code,
            null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'procedureCode' }

        where:
        code << ["lowercase", "has space", "special!char"]
    }

    @Unroll
    def "MedicalExpenseInputDto fails validation for invalid claimNumber: '#num'"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            num,
            ClaimStatus.Submitted,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'claimNumber' }

        where:
        num << ["lowercase", "has space"]
    }

    def "MedicalExpenseInputDto fails validation when claimNumber is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            null,
            ClaimStatus.Submitted,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'claimNumber' }
    }

    def "MedicalExpenseInputDto fails validation when claimStatus is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            "CLM-001",
            null,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'claimStatus' }
    }

    def "MedicalExpenseInputDto passes validation with all ClaimStatus values"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            "CLM-001",
            status,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()

        where:
        status << ClaimStatus.values()
    }

    def "MedicalExpenseInputDto serviceDescription exceeds 500 chars fails validation"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            "a" * 501,
            null, null,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("100.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'serviceDescription' }
    }

    def "MedicalExpenseInputDto optional fields are null by default"() {
        when:
        def dto = validDto()

        then:
        dto.medicalExpenseId == null
        dto.transactionId == null
        dto.providerId == null
        dto.familyMemberId == null
        dto.paidDate == null
        dto.procedureCode == null
        dto.diagnosisCode == null
        dto.serviceDescription == "Annual checkup"
    }
}
