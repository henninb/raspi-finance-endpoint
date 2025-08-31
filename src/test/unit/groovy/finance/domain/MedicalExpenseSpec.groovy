package finance.domain

import spock.lang.Specification
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.json.JsonSlurper
import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

class MedicalExpenseSpec extends Specification {

    Validator validator
    ObjectMapper objectMapper

    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
        objectMapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

    def "should create valid medical expense"() {
        given: "a valid medical expense"
        MedicalExpense medicalExpense = new MedicalExpense()
        medicalExpense.transactionId = 1001L
        medicalExpense.providerId = 1L
        medicalExpense.familyMemberId = 1L
        medicalExpense.serviceDate = Date.valueOf("2024-01-15")
        medicalExpense.serviceDescription = "Annual Physical Exam"
        medicalExpense.procedureCode = "99396"
        medicalExpense.diagnosisCode = "Z00.00"
        medicalExpense.billedAmount = new BigDecimal("350.00")
        medicalExpense.insuranceDiscount = new BigDecimal("50.00")
        medicalExpense.insurancePaid = new BigDecimal("250.00")
        medicalExpense.patientResponsibility = new BigDecimal("50.00")
        medicalExpense.isOutOfNetwork = false
        medicalExpense.claimNumber = "CLM-2024-001"
        medicalExpense.claimStatus = ClaimStatus.Paid
        medicalExpense.paidDate = Date.valueOf("2024-02-01")

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "no validation violations should occur"
        violations.isEmpty()
        medicalExpense.transactionId == 1001L
        medicalExpense.billedAmount == new BigDecimal("350.00")
        medicalExpense.claimStatus == ClaimStatus.Paid
    }

    def "should fail validation when transaction ID is zero or negative"() {
        given: "a medical expense with zero transaction ID"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.transactionId = 0L

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Transaction ID must be positive") }
    }

    def "should fail validation when service date is invalid"() {
        given: "a medical expense with invalid service date"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.serviceDate = Date.valueOf("1999-12-31") // Before 2000-01-01

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
    }

    def "should fail validation when billed amount is negative"() {
        given: "a medical expense with negative billed amount"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.billedAmount = new BigDecimal("-100.00")

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Billed amount must be non-negative") }
    }

    def "should fail validation when patient responsibility is negative"() {
        given: "a medical expense with negative patient responsibility"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.patientResponsibility = new BigDecimal("-50.00")

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Patient responsibility must be non-negative") }
    }

    def "should fail validation when procedure code contains invalid characters"() {
        given: "a medical expense with invalid procedure code"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.procedureCode = "99396@invalid"

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Procedure code can only contain uppercase letters, numbers, and hyphens") }
    }

    def "should fail validation when diagnosis code contains invalid characters"() {
        given: "a medical expense with invalid diagnosis code"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.diagnosisCode = "Z00.00@invalid"

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Diagnosis code can only contain uppercase letters, numbers, periods, and hyphens") }
    }

    def "should calculate net amount correctly"() {
        given: "a medical expense with financial data"
        MedicalExpense medicalExpense = new MedicalExpense(
            billedAmount: new BigDecimal("1000.00"),
            insuranceDiscount: new BigDecimal("200.00"),
            insurancePaid: new BigDecimal("600.00")
        )

        when: "calculating net amount"
        BigDecimal netAmount = medicalExpense.getNetAmount()

        then: "net amount should be calculated correctly"
        netAmount == new BigDecimal("200.00") // 1000 - 200 - 600 = 200
    }

    def "should calculate total covered correctly"() {
        given: "a medical expense with insurance data"
        MedicalExpense medicalExpense = new MedicalExpense(
            insuranceDiscount: new BigDecimal("200.00"),
            insurancePaid: new BigDecimal("600.00")
        )

        when: "calculating total covered"
        BigDecimal totalCovered = medicalExpense.getTotalCovered()

        then: "total covered should be calculated correctly"
        totalCovered == new BigDecimal("800.00") // 200 + 600 = 800
    }

    def "should determine if fully paid when patient responsibility is zero"() {
        given: "a medical expense with zero patient responsibility"
        MedicalExpense medicalExpense = new MedicalExpense(
            patientResponsibility: BigDecimal.ZERO,
            paidDate: null
        )

        when: "checking if fully paid"
        boolean fullyPaid = medicalExpense.isFullyPaid()

        then: "should be fully paid"
        fullyPaid
    }

    def "should determine if fully paid when paid date is set"() {
        given: "a medical expense with paid date"
        MedicalExpense medicalExpense = new MedicalExpense(
            patientResponsibility: new BigDecimal("50.00"),
            paidDate: Date.valueOf("2024-02-01")
        )

        when: "checking if fully paid"
        boolean fullyPaid = medicalExpense.isFullyPaid()

        then: "should be fully paid"
        fullyPaid
    }

    def "should determine if not fully paid when responsibility exists and no paid date"() {
        given: "a medical expense with patient responsibility and no paid date"
        MedicalExpense medicalExpense = new MedicalExpense(
            patientResponsibility: new BigDecimal("50.00"),
            paidDate: null
        )

        when: "checking if fully paid"
        boolean fullyPaid = medicalExpense.isFullyPaid()

        then: "should not be fully paid"
        !fullyPaid
    }

    def "should calculate coverage percentage correctly"() {
        given: "a medical expense with financial data"
        MedicalExpense medicalExpense = new MedicalExpense(
            billedAmount: new BigDecimal("1000.00"),
            insuranceDiscount: new BigDecimal("200.00"),
            insurancePaid: new BigDecimal("600.00")
        )

        when: "calculating coverage percentage"
        BigDecimal coveragePercentage = medicalExpense.getCoveragePercentage()

        then: "coverage percentage should be calculated correctly"
        coveragePercentage == new BigDecimal("80.0000") // (200+600)/1000 * 100 = 80%
    }

    def "should handle zero billed amount in coverage percentage calculation"() {
        given: "a medical expense with zero billed amount"
        MedicalExpense medicalExpense = new MedicalExpense(
            billedAmount: BigDecimal.ZERO,
            insuranceDiscount: new BigDecimal("200.00"),
            insurancePaid: new BigDecimal("600.00")
        )

        when: "calculating coverage percentage"
        BigDecimal coveragePercentage = medicalExpense.getCoveragePercentage()

        then: "coverage percentage should be zero"
        coveragePercentage == BigDecimal.ZERO
    }

    def "should throw exception for inconsistent financial data"() {
        given: "a medical expense with inconsistent financial data"
        MedicalExpense medicalExpense = new MedicalExpense(
            transactionId: 1001L,
            serviceDate: Date.valueOf("2024-01-15"),
            billedAmount: new BigDecimal("100.00"),
            insuranceDiscount: new BigDecimal("200.00"),
            insurancePaid: new BigDecimal("600.00"),
            patientResponsibility: new BigDecimal("300.00") // Total exceeds billed amount
        )

        when: "pre-persisting the entity"
        medicalExpense.prePersist()

        then: "should throw IllegalStateException"
        thrown(IllegalStateException)
    }

    def "should update timestamp on pre-update"() {
        given: "a medical expense"
        MedicalExpense medicalExpense = new MedicalExpense(
            dateUpdated: new Timestamp(0L) // Set to epoch
        )

        when: "pre-updating the entity"
        medicalExpense.preUpdate()

        then: "date updated should be current timestamp"
        medicalExpense.dateUpdated.time > 0L
    }

    def "should have proper equals and hashCode implementation"() {
        given: "two medical expenses with same IDs"
        MedicalExpense expense1 = new MedicalExpense(
            medicalExpenseId: 123L,
            transactionId: 1001L
        )
        MedicalExpense expense2 = new MedicalExpense(
            medicalExpenseId: 123L,
            transactionId: 1001L
        )
        MedicalExpense expense3 = new MedicalExpense(
            medicalExpenseId: 456L,
            transactionId: 1002L
        )

        expect: "equals and hashCode to work correctly"
        expense1 == expense2
        expense1 != expense3
        expense1.hashCode() == expense2.hashCode()
        expense1.hashCode() != expense3.hashCode()
    }

    def "should have meaningful toString representation"() {
        given: "a medical expense"
        MedicalExpense medicalExpense = new MedicalExpense(
            medicalExpenseId: 123L,
            transactionId: 1001L,
            serviceDate: Date.valueOf("2024-01-15"),
            billedAmount: new BigDecimal("350.00"),
            patientResponsibility: new BigDecimal("50.00"),
            claimStatus: ClaimStatus.Paid
        )

        when: "converting to string"
        String result = medicalExpense.toString()

        then: "should contain key information in JSON form"
        def json = new groovy.json.JsonSlurper().parseText(result)
        (json.medicalExpenseId as Long) == 123L
        (json.transactionId as Long) == 1001L
        (json.billedAmount as BigDecimal) == new BigDecimal("350.00")
        json.claimStatus == 'paid'
    }

    def "should validate service description length"() {
        given: "a medical expense with overly long service description"
        String longDescription = "A" * 501 // 501 characters
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.serviceDescription = longDescription

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Service description cannot exceed 500 characters") }
    }

    def "should validate claim number format"() {
        given: "a medical expense with invalid claim number format"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.claimNumber = "clm-2024-001@invalid" // Contains invalid characters

        when: "validating the medical expense"
        Set<ConstraintViolation<MedicalExpense>> violations = validator.validate(medicalExpense)

        then: "validation should fail"
        !violations.isEmpty()
        violations.any { it.message.contains("Claim number can only contain uppercase letters, numbers, and hyphens") }
    }

    def "should serialize isOutOfNetwork property correctly to JSON when true"() {
        given: "a medical expense with isOutOfNetwork = true"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.isOutOfNetwork = true
        medicalExpense.claimNumber = "CLM-2024-001"

        when: "serializing to JSON"
        String jsonString = objectMapper.writeValueAsString(medicalExpense)

        then: "JSON should contain 'isOutOfNetwork': true (@JsonProperty annotation working correctly)"
        def jsonSlurper = new JsonSlurper()
        def jsonObject = jsonSlurper.parseText(jsonString)
        jsonObject.isOutOfNetwork == true
        jsonString.contains('"isOutOfNetwork":true')
        
        and: "JSON should NOT contain 'outOfNetwork' property name (annotation fix resolved this)"
        jsonObject.outOfNetwork == null
        !jsonString.contains('"outOfNetwork":')
    }

    def "should serialize isOutOfNetwork property correctly to JSON when false"() {
        given: "a medical expense with isOutOfNetwork = false"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.isOutOfNetwork = false
        medicalExpense.claimNumber = "CLM-2024-002"

        when: "serializing to JSON"
        String jsonString = objectMapper.writeValueAsString(medicalExpense)

        then: "JSON should contain 'isOutOfNetwork': false (@JsonProperty annotation working correctly)"
        def jsonSlurper = new JsonSlurper()
        def jsonObject = jsonSlurper.parseText(jsonString)
        jsonObject.isOutOfNetwork == false
        jsonString.contains('"isOutOfNetwork":false')
        
        and: "JSON should NOT contain 'outOfNetwork' property name (annotation fix resolved this)"
        jsonObject.outOfNetwork == null
        !jsonString.contains('"outOfNetwork":')
    }

    def "should demonstrate fixed JSON serialization behavior"() {
        given: "a medical expense with isOutOfNetwork = true"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.isOutOfNetwork = true
        medicalExpense.claimNumber = "CLM-2024-003"

        when: "serializing to JSON"
        String jsonString = objectMapper.writeValueAsString(medicalExpense)

        then: "JSON should contain 'isOutOfNetwork': true (fixed behavior)"
        def jsonSlurper = new JsonSlurper()
        def jsonObject = jsonSlurper.parseText(jsonString)
        jsonObject.isOutOfNetwork == true
        
        and: "object property is accessible as isOutOfNetwork in Kotlin"
        medicalExpense.isOutOfNetwork == true
        
        and: "JSON serialization now correctly uses 'isOutOfNetwork' (@JsonProperty annotation working)"
        jsonString.contains('"isOutOfNetwork":true')
        !jsonString.contains('"outOfNetwork":')
    }

    def "should document fixed JSON property name behavior (annotation working correctly)"() {
        given: "a medical expense with isOutOfNetwork = true"
        MedicalExpense medicalExpense = createValidMedicalExpense()
        medicalExpense.isOutOfNetwork = true
        medicalExpense.claimNumber = "CLM-2024-005"

        when: "serializing to JSON"
        String jsonString = objectMapper.writeValueAsString(medicalExpense)

        then: "JSON now correctly contains 'isOutOfNetwork' (as expected with working annotation)"
        jsonString.contains('"isOutOfNetwork":')
        
        and: "JSON does NOT contain the old 'outOfNetwork' property name"
        !jsonString.contains('"outOfNetwork":')
        !jsonString.contains('"out_of_network":')
        !jsonString.contains('"OutOfNetwork":')
        
        and: "property is accessible via JsonSlurper with correct 'isOutOfNetwork' key"
        def jsonSlurper = new JsonSlurper()
        def jsonObject = jsonSlurper.parseText(jsonString)
        jsonObject.containsKey('isOutOfNetwork')
        !jsonObject.containsKey('outOfNetwork')
        
        and: "this demonstrates the @JsonProperty annotation is now working properly"
        jsonObject.isOutOfNetwork == true
    }

    private MedicalExpense createValidMedicalExpense() {
        MedicalExpense medicalExpense = new MedicalExpense()
        medicalExpense.transactionId = 1001L
        medicalExpense.serviceDate = Date.valueOf("2024-01-15")
        medicalExpense.billedAmount = new BigDecimal("350.00")
        medicalExpense.insuranceDiscount = new BigDecimal("50.00")
        medicalExpense.insurancePaid = new BigDecimal("250.00")
        medicalExpense.patientResponsibility = new BigDecimal("50.00")
        medicalExpense.isOutOfNetwork = false
        medicalExpense.claimStatus = ClaimStatus.Submitted
        medicalExpense.activeStatus = true
        return medicalExpense
    }
}
