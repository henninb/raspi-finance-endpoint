package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.domain.AccountType
import finance.repositories.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Integration tests verifying that payment creation generates transactions
 * with correct amounts, account types, and metadata.
 *
 * These tests address gaps in transaction verification.
 */
class PaymentTransactionVerificationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    TransactionRepository transactionRepository

    def "createPayment should create transactions with correct amount signs for BILL_PAYMENT"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_verify", "checking", true)
        String creditCardAccount = testDataManager.createAccountFor(testOwner, "credit_verify", "credit_card", true)

        and: "a payment from checking to credit card (BILL_PAYMENT)"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,                // source: asset
                creditCardAccount,              // destination: liability
                LocalDate.parse("2024-02-01"),
                new BigDecimal("150.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null
        result.guidSource != null
        result.guidDestination != null

        and: "source transaction has negative amount (money leaving)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-150.00")

        and: "destination transaction has negative amount (debt decreasing)"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("-150.00")
    }

    def "createPayment should create transactions with correct amount signs for TRANSFER"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_xfer", "checking", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_xfer", "savings", true)

        and: "a payment from checking to savings (TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,                // source: asset
                savingsAccount,                 // destination: asset
                LocalDate.parse("2024-02-01"),
                new BigDecimal("500.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "source transaction has negative amount (money leaving)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-500.00")

        and: "destination transaction has positive amount (money arriving)"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("500.00")
    }

    def "createPayment should create transactions with correct amount signs for CASH_ADVANCE"() {
        given:
        withUserRole()
        String creditCardAccount = testDataManager.createAccountFor(testOwner, "credit_advance", "credit_card", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_advance", "checking", true)

        and: "a payment from credit card to checking (CASH_ADVANCE)"
        def dto = new PaymentInputDto(
                null,
                creditCardAccount,              // source: liability
                checkingAccount,                // destination: asset
                LocalDate.parse("2024-02-01"),
                new BigDecimal("300.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "source transaction has positive amount (debt increasing)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("300.00")

        and: "destination transaction has positive amount (money arriving)"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("300.00")
    }

    def "createPayment should create transactions with correct amount signs for BALANCE_TRANSFER"() {
        given:
        withUserRole()
        String creditCard1 = testDataManager.createAccountFor(testOwner, "credit_bal1", "credit_card", true)
        String creditCard2 = testDataManager.createAccountFor(testOwner, "credit_bal2", "credit_card", true)

        and: "a payment from credit card to credit card (BALANCE_TRANSFER)"
        def dto = new PaymentInputDto(
                null,
                creditCard1,                    // source: liability (charging this card)
                creditCard2,                    // destination: liability (paying this card)
                LocalDate.parse("2024-02-01"),
                new BigDecimal("1000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "source transaction has positive amount (debt increasing - charging to pay another card)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("1000.00")

        and: "destination transaction has negative amount (debt decreasing - being paid off)"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("-1000.00")
    }

    def "createPayment should set accountType field to actual account type, not legacy Debit/Credit"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_type", "checking", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_type", "savings", true)

        and: "a payment between modern account types"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                savingsAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("75.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "source transaction uses actual Checking account type"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.accountType == AccountType.Checking
        sourceTransaction.accountType != AccountType.Debit

        and: "destination transaction uses actual Savings account type"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.accountType == AccountType.Savings
        destTransaction.accountType != AccountType.Credit
    }

    def "createPayment should populate transaction notes with account references"() {
        given:
        withUserRole()
        String sourceAcct = testDataManager.createAccountFor(testOwner, "source_notes", "checking", true)
        String destAcct = testDataManager.createAccountFor(testOwner, "dest_notes", "savings", true)

        and: "a payment"
        def dto = new PaymentInputDto(
                null,
                sourceAcct,
                destAcct,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "source transaction notes reference destination"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.notes.contains(destAcct)

        and: "destination transaction notes reference source"
        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.notes.contains(sourceAcct)
    }

    def "createPayment should set transaction description and category correctly"() {
        given:
        withUserRole()
        String sourceAcct = testDataManager.createAccountFor(testOwner, "source_meta", "checking", true)
        String destAcct = testDataManager.createAccountFor(testOwner, "dest_meta", "credit_card", true)

        and: "a payment"
        def dto = new PaymentInputDto(
                null,
                sourceAcct,
                destAcct,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("200.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "both transactions have 'payment' description"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.description == "payment"

        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.description == "payment"

        and: "both transactions have 'bill_pay' category"
        sourceTransaction.category == "bill_pay"
        destTransaction.category == "bill_pay"
    }

    def "createPayment should handle very small amounts correctly"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_small", "checking", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_small", "savings", true)

        and: "a payment with minimum allowed amount"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                savingsAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("0.01"),         // minimum amount
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created successfully"
        result != null
        result.amount == new BigDecimal("0.01")

        and: "transactions have correct small amounts"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-0.01")

        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("0.01")
    }

    def "createPayment should handle large amounts correctly"() {
        given:
        withUserRole()
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_large", "checking", true)
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings_large", "savings", true)

        and: "a payment with large amount"
        def dto = new PaymentInputDto(
                null,
                checkingAccount,
                savingsAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("999999.99"),    // large amount
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created successfully"
        result != null
        result.amount == new BigDecimal("999999.99")

        and: "transactions have correct large amounts with proper signs"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-999999.99")

        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("999999.99")
    }

    def "createPayment with medical account types should use correct behavior"() {
        given:
        withUserRole()
        String hsaAccount = testDataManager.createAccountFor(testOwner, "hsa_medical", "hsa", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking_medical", "checking", true)

        and: "a payment from HSA to checking (TRANSFER - both assets)"
        def dto = new PaymentInputDto(
                null,
                hsaAccount,
                checkingAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("250.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "transactions have TRANSFER behavior amounts (negative source, positive dest)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-250.00")
        sourceTransaction.accountType == AccountType.HSA

        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("250.00")
        destTransaction.accountType == AccountType.Checking
    }

    def "createPayment with investment account types should use correct behavior"() {
        given:
        withUserRole()
        String brokerageAccount = testDataManager.createAccountFor(testOwner, "brokerage_inv", "brokerage", true)
        String mortgageAccount = testDataManager.createAccountFor(testOwner, "mortgage_inv", "mortgage", true)

        and: "a payment from brokerage to mortgage (BILL_PAYMENT - asset to liability)"
        def dto = new PaymentInputDto(
                null,
                brokerageAccount,
                mortgageAccount,
                LocalDate.parse("2024-02-01"),
                new BigDecimal("2000.00"),
                null,
                null,
                null
        )

        when: "creating the payment"
        def result = mutationController.createPayment(dto)

        then: "payment is created"
        result != null

        and: "transactions have BILL_PAYMENT behavior amounts (both negative)"
        def sourceTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidSource).get()
        sourceTransaction.amount == new BigDecimal("-2000.00")
        sourceTransaction.accountType == AccountType.Brokerage

        def destTransaction = transactionRepository.findByOwnerAndGuid(testOwner,result.guidDestination).get()
        destTransaction.amount == new BigDecimal("-2000.00")
        destTransaction.accountType == AccountType.Mortgage
    }
}
