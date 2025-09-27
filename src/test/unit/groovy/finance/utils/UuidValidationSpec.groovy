package finance.utils

import finance.controllers.dto.TransferInputDto
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import spock.lang.Specification

import java.sql.Date

class UuidValidationSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    private static TransferInputDto makeTransferInputDto(Map params = [:]) {
        def defaults = [
                sourceAccount: 'checking_primary',
                destinationAccount: 'savings_secondary',
                amount: BigDecimal.valueOf(100.00),
                guidSource: null,
                guidDestination: null,
                activeStatus: true
        ]
        def merged = defaults + params
        new TransferInputDto(
                null,
                merged.sourceAccount,
                merged.destinationAccount,
                new Date(System.currentTimeMillis()),
                merged.amount,
                merged.guidSource,
                merged.guidDestination,
                merged.activeStatus
        )
    }

    /*
    def "should accept empty string for guidSource and guidDestination"() {
        given: "a TransferInputDto with empty string guid fields"
        TransferInputDto transferInput = makeTransferInputDto(
                guidSource: "",
                guidDestination: ""
        )

        when: "validating the DTO"
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(transferInput)

        then: "no UUID validation errors should occur"
        violations.findAll { it.propertyPath.toString() in ['guidSource', 'guidDestination'] }.isEmpty()
    }
    */

    def "should accept valid UUID for guidSource and guidDestination"() {
        given: "a TransferInputDto with valid UUID guid fields"
        TransferInputDto transferInput = makeTransferInputDto(
                guidSource: "550e8400-e29b-41d4-a716-446655440001",
                guidDestination: "550e8400-e29b-41d4-a716-446655440002"
        )

        when: "validating the DTO"
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(transferInput)

        then: "no UUID validation errors should occur"
        violations.findAll { it.propertyPath.toString() in ['guidSource', 'guidDestination'] }.isEmpty()
    }

    def "should reject invalid UUID format for guidSource and guidDestination"() {
        given: "a TransferInputDto with invalid UUID guid fields"
        TransferInputDto transferInput = makeTransferInputDto(
                guidSource: "invalid-uuid",
                guidDestination: "also-invalid"
        )

        when: "validating the DTO"
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(transferInput)

        then: "UUID validation errors should occur"
        def uuidViolations = violations.findAll { it.propertyPath.toString() in ['guidSource', 'guidDestination'] }
        uuidViolations.size() == 2
        uuidViolations.every { it.message == "must be uuid formatted" }
    }

    def "should accept null for guidSource and guidDestination"() {
        given: "a TransferInputDto with null guid fields"
        TransferInputDto transferInput = makeTransferInputDto(
                guidSource: null,
                guidDestination: null
        )

        when: "validating the DTO"
        Set<ConstraintViolation<TransferInputDto>> violations = validator.validate(transferInput)

        then: "no UUID validation errors should occur"
        violations.findAll { it.propertyPath.toString() in ['guidSource', 'guidDestination'] }.isEmpty()
    }
}
