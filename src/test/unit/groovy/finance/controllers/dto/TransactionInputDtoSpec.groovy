package finance.controllers.dto

import finance.domain.AccountType
import finance.domain.BaseDomainSpec
import finance.domain.ReoccurringType
import finance.domain.TransactionState
import finance.domain.TransactionType
import spock.lang.Unroll
import jakarta.validation.ConstraintViolation

import java.time.LocalDate

class TransactionInputDtoSpec extends BaseDomainSpec {

    def "TransactionInputDto should be created with valid required data"() {
        given:
        def accountNameOwner = "checking_primary"
        def accountType = AccountType.Debit
        def transactionType = TransactionType.Expense
        def transactionDate = LocalDate.of(2024, 1, 15)
        def description = "grocery store"
        def category = "groceries"
        def amount = new BigDecimal("100.50")
        def transactionState = TransactionState.Outstanding

        when:
        def dto = new TransactionInputDto(
            null, // transactionId
            null, // guid
            null, // accountId
            accountType,
            transactionType,
            accountNameOwner,
            transactionDate,
            description,
            category,
            amount,
            transactionState,
            null, // activeStatus
            null, // reoccurringType
            null, // notes
            null, // dueDate
            null  // receiptImageId
        )

        then:
        dto.accountNameOwner == accountNameOwner
        dto.accountType == accountType
        dto.transactionType == transactionType
        dto.transactionDate == transactionDate
        dto.description == description
        dto.category == category
        dto.amount == amount
        dto.transactionState == transactionState
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid accountNameOwner: '#accountNameOwner'"() {
        when:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            accountNameOwner,
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'accountNameOwner' }

        where:
        accountNameOwner << ["", "ab", "a".repeat(41), "invalid account", "UPPERCASE_NAME", "double__underscore"]
    }

    def "TransactionInputDto validation should pass for valid accountNameOwner"() {
        given:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid description: '#description'"() {
        when:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            description,
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'description' }

        where:
        description << ["", "a".repeat(76)]
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid category: '#category'"() {
        when:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            category,
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'category' }

        where:
        category << ["", "a".repeat(51), "has spaces", "UPPERCASE"]
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid amount"() {
        when:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            amount,
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()

        where:
        amount << [new BigDecimal("123456789.12"), new BigDecimal("100.123")]
    }

    def "TransactionInputDto validation should fail for null required fields"() {
        when:
        new TransactionInputDto(
            null,
            null,
            null,
            null, // accountType - required
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )

        then:
        thrown(Exception)
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid GUID format: '#guid'"() {
        when:
        def dto = new TransactionInputDto(
            null,
            guid,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'guid' }

        where:
        guid << ["invalid-guid", "UPPERCASE-GUID", "123", "abc-def-ghi"]
    }

    def "TransactionInputDto validation should pass for valid UUID format"() {
        given:
        def dto = new TransactionInputDto(
            null,
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )

        when:
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        violations.isEmpty()
    }

    def "TransactionInputDto should have default values for optional fields"() {
        given:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            null,
            null,
            null
        )

        expect:
        dto.transactionId == null
        dto.guid == null
        dto.accountId == null
        dto.activeStatus == null
        dto.reoccurringType == null
        dto.notes == null
        dto.dueDate == null
        dto.receiptImageId == null
    }

    def "TransactionInputDto should accept all optional fields when provided"() {
        given:
        def dto = new TransactionInputDto(
            123L,
            "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            456L,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            true,
            ReoccurringType.Monthly,
            "test notes",
            LocalDate.of(2024, 2, 15),
            789L
        )

        expect:
        dto.transactionId == 123L
        dto.guid == "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        dto.accountId == 456L
        dto.activeStatus == true
        dto.reoccurringType == ReoccurringType.Monthly
        dto.notes == "test notes"
        dto.dueDate == LocalDate.of(2024, 2, 15)
        dto.receiptImageId == 789L
    }

    @Unroll
    def "TransactionInputDto validation should fail for invalid notes length"() {
        when:
        def dto = new TransactionInputDto(
            null,
            null,
            null,
            AccountType.Debit,
            TransactionType.Expense,
            "checking_primary",
            LocalDate.of(2024, 1, 15),
            "test description",
            "groceries",
            new BigDecimal("100.00"),
            TransactionState.Outstanding,
            null,
            null,
            notes,
            null,
            null
        )
        Set<ConstraintViolation<TransactionInputDto>> violations = validator.validate(dto)

        then:
        !violations.isEmpty()
        violations.any { it.propertyPath.toString() == 'notes' }

        where:
        notes << ["a".repeat(101)]
    }
}
