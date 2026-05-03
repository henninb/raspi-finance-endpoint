package finance.controllers.graphql

import finance.controllers.dto.*
import finance.domain.*
import finance.services.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification

import java.time.LocalDate
import java.math.BigDecimal
import java.sql.Timestamp

class GraphQLMutationControllerSpec extends Specification {

    GraphQLMutationController controller
    AccountService mockAccountService
    CategoryService mockCategoryService
    DescriptionService mockDescriptionService
    MedicalExpenseService mockMedicalExpenseService
    ParameterService mockParameterService
    PaymentService mockPaymentService
    TransactionService mockTransactionService
    TransferService mockTransferService
    ValidationAmountService mockValidationAmountService
    MeterRegistry mockMeterRegistry
    Counter mockCounter

    def setup() {
        mockAccountService = Mock(AccountService)
        mockCategoryService = Mock(CategoryService)
        mockDescriptionService = Mock(DescriptionService)
        mockMedicalExpenseService = Mock(MedicalExpenseService)
        mockParameterService = Mock(ParameterService)
        mockPaymentService = Mock(PaymentService)
        mockTransactionService = Mock(TransactionService)
        mockTransferService = Mock(TransferService)
        mockValidationAmountService = Mock(ValidationAmountService)
        mockMeterRegistry = Mock(MeterRegistry)
        mockCounter = Mock(Counter)

        mockMeterRegistry.counter(_) >> mockCounter

        controller = new GraphQLMutationController(
            mockAccountService,
            mockCategoryService,
            mockDescriptionService,
            mockMedicalExpenseService,
            mockParameterService,
            mockPaymentService,
            mockTransactionService,
            mockTransferService,
            mockValidationAmountService,
            mockMeterRegistry
        )
    }

    def "createPayment should create payment successfully"() {
        given: "a valid payment input"
        def inputDto = new PaymentInputDto(
            null, "checking_primary", "bills_payable", LocalDate.of(2024, 1, 15),
            new BigDecimal("100.00"), null, null, true
        )
        def savedPayment = new Payment(paymentId: 123L)

        when: "createPayment is called"
        def result = controller.createPayment(inputDto)

        then:
        1 * mockPaymentService.save(_) >> ServiceResult.Success.of(savedPayment)
        1 * mockCounter.increment()
        result == savedPayment
    }

    def "updatePayment should update payment successfully"() {
        given:
        def dto = new PaymentInputDto(1L, "acc", "dest", LocalDate.now(), new BigDecimal("150.00"), null, null, true)
        def existing = new Payment(paymentId: 1L, guidSource: "src", guidDestination: "dest")
        def updated = new Payment(paymentId: 1L, amount: new BigDecimal("150.00"))

        when:
        def result = controller.updatePayment(1L, dto)

        then:
        1 * mockPaymentService.findByPaymentId(1L) >> Optional.of(existing)
        1 * mockPaymentService.updatePayment(1L, _) >> updated
        1 * mockCounter.increment()
        result == updated
    }

    def "deletePayment should delete payment by ID"() {
        given: "a payment ID"
        def paymentId = 123L

        when: "deletePayment is called"
        def result = controller.deletePayment(paymentId)

        then: "service deletes the payment"
        1 * mockPaymentService.deleteById(123L) >> ServiceResult.Success.of(new Payment(paymentId: 123L))

        and: "deletion success is returned"
        result == true
    }

    def "deletePayment should return false when payment not found"() {
        given: "a non-existent payment ID"
        def paymentId = 999L

        when: "deletePayment is called"
        def result = controller.deletePayment(paymentId)

        then: "service returns not found"
        1 * mockPaymentService.deleteById(999L) >> ServiceResult.NotFound.of("Payment not found: 999")

        and: "deletion failure is returned"
        result == false
    }

    def "createTransfer should create transfer successfully"() {
        given: "a valid transfer input"
        def inputDto = new TransferInputDto(
            null, "checking_primary", "savings_primary", LocalDate.of(2024, 1, 15),
            new BigDecimal("500.00"), null, null, true
        )
        def savedTransfer = new Transfer(transferId: 321L)

        when: "createTransfer is called"
        def result = controller.createTransfer(inputDto)

        then:
        1 * mockTransferService.insertTransfer(_) >> savedTransfer
        1 * mockCounter.increment()
        result == savedTransfer
    }

    def "updateTransfer should update transfer successfully"() {
        given:
        def dto = new TransferInputDto(1L, "src", "dest", LocalDate.now(), new BigDecimal("600.00"), null, null, true)
        def existing = new Transfer(transferId: 1L, guidSource: "src", guidDestination: "dest")
        def updated = new Transfer(transferId: 1L, amount: new BigDecimal("600.00"))

        when:
        def result = controller.updateTransfer(1L, dto)

        then:
        1 * mockTransferService.findByTransferId(1L) >> Optional.of(existing)
        1 * mockTransferService.updateTransfer(_) >> updated
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteTransfer should delete transfer by ID"() {
        given: "a transfer ID"
        def transferId = 321L

        when: "deleteTransfer is called"
        def result = controller.deleteTransfer(transferId)

        then: "service deletes the transfer"
        1 * mockTransferService.deleteByTransferId(321L) >> true

        and: "deletion success is returned"
        result == true
    }

    def "deleteTransfer should return false when transfer not found"() {
        given: "a non-existent transfer ID"
        def transferId = 999L

        when: "deleteTransfer is called"
        def result = controller.deleteTransfer(transferId)

        then:
        1 * mockTransferService.deleteByTransferId(999L) >> false

        and: "deletion failure is returned"
        result == false
    }

    def "createParameter should create parameter successfully"() {
        given: "a valid parameter DTO"
        def parameterDto = new ParameterInputDto(null, "test_param", "test_value", true)
        def savedParameter = new Parameter(parameterId: 123L)

        when: "createParameter is called"
        def result = controller.createParameter(parameterDto)

        then:
        1 * mockParameterService.save(_) >> ServiceResult.Success.of(savedParameter)
        1 * mockCounter.increment()
        result == savedParameter
    }

    def "updateParameter should update parameter successfully"() {
        given: "an existing parameter DTO"
        def parameterDto = new ParameterInputDto(123L, "test_param", "updated_value", true)

        when: "updateParameter is called"
        def result = controller.updateParameter(parameterDto)

        then:
        1 * mockParameterService.update(_) >> ServiceResult.Success.of(new Parameter(parameterId: 123L))
        1 * mockCounter.increment()
        result.parameterId == 123L
    }

    def "deleteParameter should delete parameter successfully"() {
        given: "an existing parameter ID"
        def parameterId = 123L

        when: "deleteParameter is called"
        def result = controller.deleteParameter(parameterId)

        then: "service returns success result"
        1 * mockParameterService.deleteById(parameterId) >> ServiceResult.Success.of(new Parameter())
        1 * mockCounter.increment()
        result == true
    }

    // Category mutations
    def "createCategory should create category successfully"() {
        given:
        def dto = new CategoryInputDto(null, "groceries", true)
        def saved = new Category(categoryName: "groceries")

        when:
        def result = controller.createCategory(dto)

        then:
        1 * mockCategoryService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateCategory should update category successfully"() {
        given:
        def dto = new CategoryInputDto(null, "groceries", false)
        def updated = new Category(categoryName: "groceries")

        when:
        def result = controller.updateCategory(dto, "old_groceries")

        then:
        1 * mockCategoryService.findByCategoryNameStandardized("old_groceries") >> ServiceResult.Success.of(new Category(categoryId: 1L))
        1 * mockCategoryService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteCategory should delete category by name"() {
        when:
        def result = controller.deleteCategory("groceries")

        then:
        1 * mockCategoryService.deleteByCategoryNameStandardized("groceries") >> ServiceResult.Success.of(new Category())
        1 * mockCounter.increment()
        result == true
    }

    // Description mutations
    def "createDescription should create description successfully"() {
        given:
        def dto = new DescriptionInputDto(null, "amazon", true)
        def saved = new Description(descriptionName: "amazon")

        when:
        def result = controller.createDescription(dto)

        then:
        1 * mockDescriptionService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateDescription should update description successfully"() {
        given:
        def dto = new DescriptionInputDto(null, "amazon", false)
        def updated = new Description(descriptionName: "amazon")

        when:
        def result = controller.updateDescription(dto, "old_amazon")

        then:
        1 * mockDescriptionService.findByDescriptionNameStandardized("old_amazon") >> ServiceResult.Success.of(new Description(descriptionId: 1L))
        1 * mockDescriptionService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteDescription should delete description by name"() {
        when:
        def result = controller.deleteDescription("amazon")

        then:
        1 * mockDescriptionService.deleteByDescriptionNameStandardized("amazon") >> ServiceResult.Success.of(new Description())
        1 * mockCounter.increment()
        result == true
    }

    // Account mutations
    def "createAccount should create account successfully"() {
        given:
        def dto = new AccountInputDto(null, "chase_brian", AccountType.Credit, true, "0001", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)
        def saved = new Account(accountNameOwner: "chase_brian")

        when:
        def result = controller.createAccount(dto)

        then:
        1 * mockAccountService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateAccount should update account successfully"() {
        given:
        def dto = new AccountInputDto(null, "chase_brian", AccountType.Credit, true, "0002", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null)
        def updated = new Account(accountNameOwner: "chase_brian")

        when:
        def result = controller.updateAccount(dto, "old_chase_brian")

        then:
        1 * mockAccountService.findById("old_chase_brian") >> ServiceResult.Success.of(new Account(accountId: 1L))
        1 * mockAccountService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteAccount should delete account by name"() {
        when:
        def result = controller.deleteAccount("chase_brian")

        then:
        1 * mockAccountService.deleteById("chase_brian") >> ServiceResult.Success.of(new Account())
        1 * mockCounter.increment()
        result == true
    }

    // Transaction mutations
    def "createTransaction should create transaction successfully"() {
        given:
        def guid = UUID.randomUUID().toString()
        def dto = new TransactionInputDto(
            null, guid, 100L, AccountType.Credit, TransactionType.Expense,
            "chase_brian", LocalDate.now(), "amazon", "online", new BigDecimal("50.00"),
            TransactionState.Cleared, true, ReoccurringType.Undefined, "notes", null, 0L
        )
        def saved = new Transaction(guid: guid)

        when:
        def result = controller.createTransaction(dto)

        then:
        1 * mockTransactionService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateTransaction should update transaction successfully"() {
        given:
        def guid = UUID.randomUUID().toString()
        def dto = new TransactionInputDto(1L, guid, 100L, AccountType.Credit, TransactionType.Expense, "chase_brian", LocalDate.now(), "updated", "online", new BigDecimal("50.00"), TransactionState.Cleared, true, ReoccurringType.Undefined, "notes", null, 0L)
        def updated = new Transaction(guid: guid, description: "updated")

        when:
        def result = controller.updateTransaction(dto)

        then:
        1 * mockTransactionService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteTransaction should delete transaction by guid"() {
        given:
        def guid = UUID.randomUUID().toString()

        when:
        def result = controller.deleteTransaction(guid)

        then:
        1 * mockTransactionService.deleteById(guid) >> ServiceResult.Success.of(new Transaction(guid: guid))
        1 * mockCounter.increment()
        result == true
    }

    // MedicalExpense mutations
    def "createMedicalExpense should create medical expense successfully"() {
        given:
        def dto = new MedicalExpenseInputDto(
            null, null, null, null, LocalDate.now(), "cleaning", "proc", "diag", 100.0G,
            20.0G, 50.0G, 30.0G, null, false, "claim123", ClaimStatus.Submitted, true, 0.0G
        )
        def saved = new MedicalExpense(medicalExpenseId: 1L)

        when:
        def result = controller.createMedicalExpense(dto)

        then:
        1 * mockMedicalExpenseService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateMedicalExpense should update medical expense successfully"() {
        given:
        def dto = new MedicalExpenseInputDto(
            1L, null, null, null, LocalDate.now(), "cleaning", "proc", "diag", 100.0G,
            20.0G, 50.0G, 30.0G, null, false, "claim123", ClaimStatus.Paid, true, 30.0G
        )
        def updated = new MedicalExpense(medicalExpenseId: 1L, claimStatus: ClaimStatus.Paid)

        when:
        def result = controller.updateMedicalExpense(dto)

        then:
        1 * mockMedicalExpenseService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteMedicalExpense should delete medical expense by ID"() {
        when:
        def result = controller.deleteMedicalExpense(1L)

        then:
        1 * mockMedicalExpenseService.deleteById(1L) >> ServiceResult.Success.of(new MedicalExpense(medicalExpenseId: 1L))
        1 * mockMeterRegistry.counter("graphql.medicalExpense.delete.success") >> mockCounter
        1 * mockCounter.increment()
        result == true
    }

    // ValidationAmount mutations
    def "createValidationAmount should create validation amount successfully"() {
        given:
        def dto = new ValidationAmountInputDto(null, 1L, new Timestamp(System.currentTimeMillis()), true, TransactionState.Cleared, 1000.0G)
        def saved = new ValidationAmount(validationId: 1L)

        when:
        def result = controller.createValidationAmount(dto)

        then:
        1 * mockValidationAmountService.save(_) >> ServiceResult.Success.of(saved)
        1 * mockCounter.increment()
        result == saved
    }

    def "updateValidationAmount should update validation amount successfully"() {
        given:
        def dto = new ValidationAmountInputDto(1L, 1L, new Timestamp(System.currentTimeMillis()), true, TransactionState.Cleared, 1100.0G)
        def updated = new ValidationAmount(validationId: 1L)

        when:
        def result = controller.updateValidationAmount(dto)

        then:
        1 * mockValidationAmountService.update(_) >> ServiceResult.Success.of(updated)
        1 * mockCounter.increment()
        result == updated
    }

    def "deleteValidationAmount should delete validation amount by ID"() {
        when:
        def result = controller.deleteValidationAmount(1L)

        then:
        1 * mockValidationAmountService.deleteById(1L) >> ServiceResult.Success.of(new ValidationAmount(validationId: 1L))
        1 * mockMeterRegistry.counter("graphql.validationAmount.delete.success") >> mockCounter
        1 * mockCounter.increment()
        result == true
    }
}
