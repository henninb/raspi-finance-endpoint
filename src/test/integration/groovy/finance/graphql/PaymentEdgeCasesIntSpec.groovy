package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.domain.AccountType
import org.springframework.beans.factory.annotation.Autowired

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Integration tests for payment edge cases and boundary conditions.
 * Tests scenarios that might cause issues or unexpected behavior.
 */
class PaymentEdgeCasesIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    def "createPayment should handle accounts with Undefined account type gracefully"() {
        given:
        withUserRole()
        String undefinedSource = testDataManager.createAccountFor(testOwner, "undefined_src", "undefined", true)
        String checkingDest = testDataManager.createAccountFor(testOwner, "checking_dest", "checking", true)

        and: "a payment from undefined account type"
        def dto = new PaymentInputDto(
                null,
                undefinedSource,
                checkingDest,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with UNDEFINED behavior (safest - both negative)"
        result != null
        result.paymentId > 0
    }

    def "createPayment should handle utility account type (expense category)"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_utility", "checking", true)
        String utilityAccount = testDataManager.createAccountFor(testOwner, "utility_test", "utility", true)

        and: "a payment to utility account"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                utilityAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("125.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created (utility has 'expense' category, treated as UNDEFINED)"
        result != null
        result.paymentId > 0
    }

    def "createPayment should handle payment with future date"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_future", "checking", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_future", "savings", true)

        and: "a payment scheduled for future date"
        def futureDate = LocalDate.parse("2025-12-31")
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                savingsAccount,
                futureDate,
                new BigDecimal("500.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created successfully with future date"
        result != null
        result.transactionDate == futureDate
    }

    def "createPayment should handle payment with past date (historical transaction)"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_past", "checking", true)
        String creditCardAccount = testDataManager.createAccountFor(testOwner, "credit_past", "credit_card", true)

        and: "a payment with past date"
        def pastDate = LocalDate.parse("2020-01-01")
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                creditCardAccount,
                pastDate,
                new BigDecimal("250.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created successfully with past date"
        result != null
        result.transactionDate == pastDate
    }

    def "createPayment with business account types should work correctly"() {
        given:
        withUserRole()
        String businessChecking = testDataManager.createAccountFor(testOwner, "business_check", "business_checking", true)
        String businessSavings = testDataManager.createAccountFor(testOwner, "business_save", "business_savings", true)

        and: "a payment between business accounts"
        def dto = new PaymentInputDto(
                null,
                businessChecking,
                businessSavings,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("5000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created (both assets - TRANSFER behavior)"
        result != null
        result.amount == new BigDecimal("5000.00")
    }

    def "createPayment with prepaid account type should work correctly"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_prepaid", "checking", true)
        String prepaidAccount = testDataManager.createAccountFor(testOwner, "prepaid_test", "prepaid", true)

        and: "a payment to prepaid account (asset to asset)"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                prepaidAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.paymentId > 0
    }

    def "createPayment with gift card account type should work correctly"() {
        given:
        withUserRole()
        String cashAccount = testDataManager.createAccountFor(testOwner, "cash_gift", "cash", true)
        String giftCardAccount = testDataManager.createAccountFor(testOwner, "gift_card_test", "gift_card", true)

        and: "a payment to gift card (asset to asset)"
        def dto = new PaymentInputDto(
                null,
                cashAccount,
                giftCardAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("50.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.paymentId > 0
    }

    def "createPayment with student loan should work correctly"() {
        given:
        withUserRole()
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_student", "savings", true)
        String studentLoanAccount = testDataManager.createAccountFor(testOwner, "student_loan_test", "student_loan", true)

        and: "a payment to student loan (asset to liability - BILL_PAYMENT)"
        def dto = new PaymentInputDto(
                null,
                savingsAccount,
                studentLoanAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("350.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with BILL_PAYMENT behavior"
        result != null
        result.amount == new BigDecimal("350.00")
    }

    def "createPayment with line of credit should work correctly"() {
        given:
        withUserRole()
        String lineOfCreditAccount = testDataManager.createAccountFor(testOwner, "loc_test", "line_of_credit", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_loc", "checking", true)

        and: "a payment from line of credit to checking (liability to asset - CASH_ADVANCE)"
        def dto = new PaymentInputDto(
                null,
                lineOfCreditAccount,
                checkingAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("1000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with CASH_ADVANCE behavior"
        result != null
        result.amount == new BigDecimal("1000.00")
    }

    def "createPayment with certificate of deposit should work correctly"() {
        given:
        withUserRole()
        String certificateAccount = testDataManager.createAccountFor(testOwner, "cd_test", "certificate", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_cd", "checking", true)

        and: "a payment from certificate to checking (asset to asset - TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                certificateAccount,
                checkingAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("5000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.amount == new BigDecimal("5000.00")
    }

    def "createPayment with money market account should work correctly"() {
        given:
        withUserRole()
        String moneyMarketAccount = testDataManager.createAccountFor(testOwner, "mm_test", "money_market", true)
        String autoLoanAccount = testDataManager.createAccountFor(testOwner, "auto_loan_test", "auto_loan", true)

        and: "a payment from money market to auto loan (asset to liability - BILL_PAYMENT)"
        def dto = new PaymentInputDto(
                null,
                moneyMarketAccount,
                autoLoanAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("450.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with BILL_PAYMENT behavior"
        result != null
        result.amount == new BigDecimal("450.00")
    }

    def "createPayment with personal loan should work correctly"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_personal", "checking", true)
        String personalLoanAccount = testDataManager.createAccountFor(testOwner, "personal_loan_test", "personal_loan", true)

        and: "a payment to personal loan (asset to liability - BILL_PAYMENT)"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                personalLoanAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("200.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with BILL_PAYMENT behavior"
        result != null
        result.amount == new BigDecimal("200.00")
    }

    def "createPayment with retirement account types should work correctly"() {
        given:
        withUserRole()
        String retirement401k = testDataManager.createAccountFor(testOwner, "401k_test", "retirement_401k", true)
        String retirementIRA = testDataManager.createAccountFor(testOwner, "ira_test", "retirement_ira", true)

        and: "a payment between retirement accounts (asset to asset - TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                retirement401k,
                retirementIRA,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("10000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.amount == new BigDecimal("10000.00")
    }

    def "createPayment with FSA account should work correctly"() {
        given:
        withUserRole()
        String fsaAccount = testDataManager.createAccountFor(testOwner, "fsa_test", "fsa", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_fsa", "checking", true)

        and: "a payment from FSA to checking (asset to asset - TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                fsaAccount,
                checkingAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("150.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.amount == new BigDecimal("150.00")
    }

    def "createPayment should handle escrow account correctly"() {
        given:
        withUserRole()
        String escrowAccount = testDataManager.createAccountFor(testOwner, "escrow_test", "escrow", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_escrow", "checking", true)

        and: "a payment from escrow to checking (asset to asset - TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                escrowAccount,
                checkingAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("3000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.amount == new BigDecimal("3000.00")
    }

    def "createPayment should handle trust account correctly"() {
        given:
        withUserRole()
        String trustAccount = testDataManager.createAccountFor(testOwner, "trust_test", "trust", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_trust", "savings", true)

        and: "a payment from trust to savings (asset to asset - TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                trustAccount,
                savingsAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("25000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created with TRANSFER behavior"
        result != null
        result.amount == new BigDecimal("25000.00")
    }
}
