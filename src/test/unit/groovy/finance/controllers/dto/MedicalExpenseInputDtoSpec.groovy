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

    def "MedicalExpenseInputDto fails validation when insuranceDiscount is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            null,
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
        violations.any { it.propertyPath.toString() == 'insuranceDiscount' }
    }

    def "MedicalExpenseInputDto fails validation when insurancePaid is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            null,
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
        violations.any { it.propertyPath.toString() == 'insurancePaid' }
    }

    def "MedicalExpenseInputDto fails validation when patientResponsibility is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            null,
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
        violations.any { it.propertyPath.toString() == 'patientResponsibility' }
    }

    def "MedicalExpenseInputDto fails validation when isOutOfNetwork is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            null, null,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            new BigDecimal("50.00")
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'isOutOfNetwork' }
    }

    def "MedicalExpenseInputDto fails validation when paidAmount is null"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00"),
            null, false,
            "CLM-001",
            ClaimStatus.Submitted,
            true,
            null
        )

        when:
        Set<ConstraintViolation<MedicalExpenseInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'paidAmount' }
    }

    def "MedicalExpenseInputDto fails validation when transactionId is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, -1L, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
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
        violations.any { it.propertyPath.toString() == 'transactionId' }
    }

    def "MedicalExpenseInputDto fails validation when providerId is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, -1L, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
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
        violations.any { it.propertyPath.toString() == 'providerId' }
    }

    def "MedicalExpenseInputDto fails validation when familyMemberId is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, -1L,
            LocalDate.of(2024, 1, 15),
            null, null, null,
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
        violations.any { it.propertyPath.toString() == 'familyMemberId' }
    }

    @Unroll
    def "MedicalExpenseInputDto fails validation for invalid diagnosisCode: '#code'"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null,
            code,
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
        violations.any { it.propertyPath.toString() == 'diagnosisCode' }

        where:
        code << ["lowercase", "has space", "special!"]
    }

    def "MedicalExpenseInputDto fails validation when insuranceDiscount is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("-1.00"),
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
        violations.any { it.propertyPath.toString() == 'insuranceDiscount' }
    }

    def "MedicalExpenseInputDto fails validation when patientResponsibility is negative"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15),
            null, null, null,
            new BigDecimal("200.00"),
            new BigDecimal("50.00"),
            new BigDecimal("100.00"),
            new BigDecimal("-1.00"),
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
        violations.any { it.propertyPath.toString() == 'patientResponsibility' }
    }

    def "MedicalExpenseInputDto equals and hashCode for identical values"() {
        given:
        def dto1 = validDto()
        def dto2 = validDto()

        expect:
        dto1 == dto2
        dto1.hashCode() == dto2.hashCode()
    }

    def "MedicalExpenseInputDto inequality when claimNumber differs"() {
        given:
        def dto1 = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15), "checkup", null, null,
            new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("50.00"),
            null, false, "CLM-001", ClaimStatus.Submitted, true, new BigDecimal("50.00")
        )
        def dto2 = new MedicalExpenseInputDto(
            null, null, null, null,
            LocalDate.of(2024, 1, 15), "checkup", null, null,
            new BigDecimal("200.00"), new BigDecimal("50.00"), new BigDecimal("100.00"), new BigDecimal("50.00"),
            null, false, "CLM-002", ClaimStatus.Submitted, true, new BigDecimal("50.00")
        )

        expect:
        dto1 != dto2
    }

    def "MedicalExpenseInputDto toString contains claimNumber"() {
        given:
        def dto = validDto()

        expect:
        dto.toString() != null
        dto.toString().contains("CLM-001")
    }
}
