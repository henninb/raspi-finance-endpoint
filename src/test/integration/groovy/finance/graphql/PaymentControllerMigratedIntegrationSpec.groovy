package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.GraphQLQueryController
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.repositories.AccountRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import finance.services.StandardizedPaymentService
import finance.helpers.SmartAccountBuilder
import finance.helpers.SmartPaymentBuilder
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared

import java.math.BigDecimal
import java.sql.Date
import java.util.UUID

/**
 * MIGRATED INTEGRATION TEST - PaymentGraphQL Resolver with robust, isolated architecture
 *
 * This is the migrated version of PaymentGraphQLResolverIntegrationSpec showing:
 * ✅ No hardcoded account names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Proper FK relationship management with Account setup
 * ✅ GraphQL resolver testing with proper mocking
 * ✅ Financial validation and consistency
 * ✅ Eliminated shared global state and cleanup issues
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerMigratedIntegrationSpec extends BaseIntegrationSpec {

    @Shared
    @Autowired
    StandardizedPaymentService paymentService

    @Shared
    @Autowired
    MeterRegistry meterRegistry

    @Shared
    @Autowired
    AccountRepository accountRepository

    @Shared
    @Autowired
    PaymentRepository paymentRepository

    @Shared
    @Autowired
    TransactionRepository transactionRepository

    @Shared
    @Autowired
    GraphQLMutationController mutationController

    @Shared
    @Autowired
    GraphQLQueryController queryController

    @Shared
    def repositoryContext

    @Shared
    Long sourceAccountId
    @Shared
    Long destinationAccountId
    @Shared
    String sourceAccountName
    @Shared
    String destinationAccountName

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)

        // Create test accounts using SmartBuilder
        sourceAccountName = "checking_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"
        destinationAccountName = "credit_${testOwner.replaceAll(/[^a-z]/, '').toLowerCase()}"

        // Source account (debit - checking)
        Account sourceAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("checking")
                .asDebit()
                .withCleared(new BigDecimal("2000.00"))
                .buildAndValidate()
        Account savedSourceAccount = accountRepository.save(sourceAccount)
        sourceAccountId = savedSourceAccount.accountId
        sourceAccountName = savedSourceAccount.accountNameOwner

        // Destination account (credit - credit card)
        Account destinationAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("credit")
                .asCredit()
                .withCleared(new BigDecimal("-500.00"))
                .buildAndValidate()
        Account savedDestAccount = accountRepository.save(destinationAccount)
        destinationAccountId = savedDestAccount.accountId
        destinationAccountName = savedDestAccount.accountNameOwner
    }

    def "should fetch all payments via controller with isolated test data"() {
        given: "existing payments in the database"
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("100.00"))
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("200.00"))

        when:
        def payments = queryController.payments()

        then: "should return payments from database with testOwner isolation"
        payments.size() >= 2
        payments.every { it instanceof Payment }
        payments.any { it.amount == new BigDecimal("100.00") && it.sourceAccount == sourceAccountName }
        payments.any { it.amount == new BigDecimal("200.00") && it.sourceAccount == sourceAccountName }
    }

    def "should fetch payment by ID via controller with isolated test data"() {
        given: "an existing payment in the database"
        def savedPayment = createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("150.00"))

        when:
        def result = queryController.payment(savedPayment.paymentId)

        then: "should return the specific payment with testOwner-based account names"
        result != null
        result.paymentId == savedPayment.paymentId
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("150.00")
    }

    def "should create payment via controller with SmartBuilder validation"() {
        given: "payment input data with testOwner-based account names"
        def paymentInput = [
            sourceAccount: sourceAccountName,
            destinationAccount: destinationAccountName,
            transactionDate: "2024-01-15",
            amount: new BigDecimal("250.00")
        ]

        and: "authenticated user"
        withUserRole("test", ["USER"])
        when:
        def result = mutationController.createPayment(new finance.controllers.dto.PaymentInputDto(null, sourceAccountName, destinationAccountName, Date.valueOf("2024-01-15"), new BigDecimal("250.00"), null))

        then: "should create and return payment with testOwner isolation"
        result != null
        result.paymentId > 0
        result.sourceAccount == sourceAccountName
        result.destinationAccount == destinationAccountName
        result.amount == new BigDecimal("250.00")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus == true

        and: "payment should be persisted with testOwner account names"
        def savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        savedPayment.isPresent()
        savedPayment.get().sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))

        and: "corresponding debit and credit transactions should be created with proper FK relationships"
        def transactions = transactionRepository.findAll()
        transactions.size() >= 2
        transactions.any { it.guid == result.guidSource && it.accountNameOwner == sourceAccountName }
        transactions.any { it.guid == result.guidDestination && it.accountNameOwner == destinationAccountName }
    }

    def "should delete payment via controller with isolated test data"() {
        given: "an existing payment in the database"
        def savedPayment = createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("75.00"))

        and: "authenticated user"
        withUserRole("test", ["USER"])
        when:
        def result = mutationController.deletePayment(savedPayment.paymentId)

        then: "should successfully delete payment and return true"
        result == true

        and: "payment should be removed from database"
        def deletedPayment = paymentRepository.findByPaymentId(savedPayment.paymentId)
        !deletedPayment.isPresent()
    }

    def "should handle validation errors for invalid payment creation with SmartBuilder constraints"() {
        given: "invalid payment input data (invalid sourceAccount too short)"
        def paymentInput = [
            sourceAccount: "ab", // Invalid - too short (less than 3 characters)
            destinationAccount: destinationAccountName,
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "authenticated user"
        withUserRole("test", ["USER"])
        when:
        mutationController.createPayment(new finance.controllers.dto.PaymentInputDto(null, "ab", destinationAccountName, Date.valueOf("2024-01-15"), new BigDecimal("100.00"), null))

        then: "should throw runtime exception for validation failure"
        thrown(RuntimeException)
    }

    def "should handle validation for payment to debit account with SmartBuilder architecture"() {
        given: "another debit account for invalid payment destination"
        Account anotherDebitAccount = SmartAccountBuilder.builderForOwner(testOwner)
                .withUniqueAccountName("savings")
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .buildAndValidate()
        Account savedDebitAccount = accountRepository.save(anotherDebitAccount)

        and: "payment input data attempting to pay a debit account"
        def paymentInput = [
            sourceAccount: sourceAccountName,
            destinationAccount: savedDebitAccount.accountNameOwner, // This is a debit account
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "authenticated user"
        withUserRole("test", ["USER"])
        when:
        mutationController.createPayment(new finance.controllers.dto.PaymentInputDto(null, sourceAccountName, savedDebitAccount.accountNameOwner, Date.valueOf("2024-01-15"), new BigDecimal("100.00"), null))

        then: "should throw validation exception for payment to debit account"
        thrown(RuntimeException)
    }

    def "should successfully execute GraphQL operations with testOwner isolation"() {
        given: "an existing payment in the database"
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("50.00"))

        when:
        def payments = queryController.payments()

        then: "should execute successfully with testOwner-based accounts"
        payments != null
        payments.size() >= 1
        payments.any {
            it.amount == new BigDecimal("50.00") &&
            it.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            it.destinationAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }
    }

    def "should handle complex GraphQL payment scenarios with FK relationships"() {
        given: "multiple payments with different amounts and dates"
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("25.50"))
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("150.75"))
        createTestPayment(sourceAccountName, destinationAccountName, new BigDecimal("300.25"))

        when: "querying all payments for this testOwner"
        def payments = queryController.payments()
        def testOwnerPayments = payments.findAll {
            it.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }

        then: "should return all payments for this testOwner with proper FK relationships"
        testOwnerPayments.size() >= 3
        testOwnerPayments.every { payment ->
            payment.sourceAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            payment.destinationAccount.contains(testOwner.replaceAll(/[^a-z]/, '')) &&
            payment.guidSource != null &&
            payment.guidDestination != null &&
            payment.activeStatus == true
        }

        and: "corresponding transactions should exist with proper account relationships"
        def allTransactions = transactionRepository.findAll()
        def testOwnerTransactions = allTransactions.findAll {
            it.accountNameOwner.contains(testOwner.replaceAll(/[^a-z]/, ''))
        }
        testOwnerTransactions.size() >= 6  // 2 transactions per payment (source + destination)
    }

    private Payment createTestPayment(String sourceAccount, String destinationAccount, BigDecimal amount) {
        Payment payment = new Payment()
        payment.sourceAccount = sourceAccount
        payment.destinationAccount = destinationAccount
        payment.transactionDate = Date.valueOf("2024-01-01")
        payment.amount = amount
        payment.guidSource = UUID.randomUUID().toString()
        payment.guidDestination = UUID.randomUUID().toString()
        payment.activeStatus = true

        return paymentService.insertPayment(payment)
    }
}
