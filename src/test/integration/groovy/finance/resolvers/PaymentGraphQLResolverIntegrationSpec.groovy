package finance.resolvers

import finance.Application
import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.repositories.AccountRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import finance.services.IPaymentService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp
import java.util.UUID

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class PaymentGraphQLResolverIntegrationSpec extends Specification {

    @Autowired
    IPaymentService paymentService

    @Autowired
    MeterRegistry meterRegistry

    @Autowired
    AccountRepository accountRepository

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    TransactionRepository transactionRepository

    PaymentGraphQLResolver paymentGraphQLResolver

    void setup() {
        paymentGraphQLResolver = new PaymentGraphQLResolver(paymentService, meterRegistry)
        cleanup()
    }

    void cleanup() {
        paymentRepository.deleteAll()
        transactionRepository.deleteAll()
        accountRepository.deleteAll()
    }

    void setupTestAccounts() {
        // Create test source account (debit account - checking account)
        Account sourceAccount = new Account()
        sourceAccount.accountNameOwner = "checking_brian"
        sourceAccount.accountType = AccountType.Debit
        sourceAccount.activeStatus = true
        sourceAccount.moniker = "1001"
        sourceAccount.outstanding = new BigDecimal("0.00")
        sourceAccount.future = new BigDecimal("0.00")
        sourceAccount.cleared = new BigDecimal("2000.00")
        sourceAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        sourceAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(sourceAccount)

        // Create test destination account (credit account - credit card)
        Account destinationAccount = new Account()
        destinationAccount.accountNameOwner = "discover_it"
        destinationAccount.accountType = AccountType.Credit
        destinationAccount.activeStatus = true
        destinationAccount.moniker = "1002"
        destinationAccount.outstanding = new BigDecimal("0.00")
        destinationAccount.future = new BigDecimal("0.00")
        destinationAccount.cleared = new BigDecimal("-500.00")
        destinationAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        destinationAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(destinationAccount)
    }

    def "should fetch all payments via GraphQL resolver in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "existing payments in the database"
        createTestPayment("checking_brian", "discover_it", new BigDecimal("100.00"))
        createTestPayment("checking_brian", "discover_it", new BigDecimal("200.00"))

        when: "payments data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payments
        def payments = dataFetcher.get(null)

        then: "should return all payments from database in integration environment"
        payments.size() == 2
        payments.every { it instanceof Payment }
        payments.any { it.amount == new BigDecimal("100.00") }
        payments.any { it.amount == new BigDecimal("200.00") }
    }

    def "should fetch payment by ID via GraphQL resolver in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "an existing payment in the database"
        def savedPayment = createTestPayment("checking_brian", "discover_it", new BigDecimal("150.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedPayment.paymentId }] as graphql.schema.DataFetchingEnvironment

        when: "payment data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payment()
        def result = dataFetcher.get(environment)

        then: "should return the specific payment from database in integration environment"
        result != null
        result.paymentId == savedPayment.paymentId
        result.sourceAccount == "checking_brian"
        result.destinationAccount == "discover_it"
        result.amount == new BigDecimal("150.00")
    }

    def "should create payment via GraphQL resolver in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "payment input data"
        def paymentInput = [
            sourceAccount: "checking_brian",
            destinationAccount: "discover_it",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("250.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> paymentInput }] as graphql.schema.DataFetchingEnvironment

        when: "create payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.createPayment()
        def result = dataFetcher.get(environment)

        then: "should create and return payment from database in integration environment"
        result != null
        result.paymentId > 0
        result.sourceAccount == "checking_brian"
        result.destinationAccount == "discover_it"
        result.amount == new BigDecimal("250.00")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus == true

        and: "payment should be persisted in database"
        def savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        savedPayment.isPresent()
        savedPayment.get().sourceAccount == "checking_brian"

        and: "corresponding debit and credit transactions should be created"
        def transactions = transactionRepository.findAll()
        transactions.size() == 2
        transactions.any { it.guid == result.guidSource }
        transactions.any { it.guid == result.guidDestination }
    }

    def "should delete payment via GraphQL resolver in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "an existing payment in the database"
        def savedPayment = createTestPayment("checking_brian", "discover_it", new BigDecimal("75.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedPayment.paymentId }] as graphql.schema.DataFetchingEnvironment

        when: "delete payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.deletePayment()
        def result = dataFetcher.get(environment)

        then: "should successfully delete payment and return true in integration environment"
        result == true

        and: "payment should be removed from database"
        def deletedPayment = paymentRepository.findByPaymentId(savedPayment.paymentId)
        !deletedPayment.isPresent()
    }

    def "should handle validation errors for invalid payment creation in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "invalid payment input data (invalid sourceAccount too short)"
        def paymentInput = [
            sourceAccount: "ab", // Invalid - too short (less than 3 characters)
            destinationAccount: "discover_it",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> paymentInput }] as graphql.schema.DataFetchingEnvironment

        when: "create payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.createPayment()
        dataFetcher.get(environment)

        then: "should throw runtime exception for validation failure"
        thrown(RuntimeException)
    }

    def "should handle validation for payment to debit account in integration environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "another debit account for invalid payment destination"
        Account anotherDebitAccount = new Account()
        anotherDebitAccount.accountNameOwner = "savings_brian"
        anotherDebitAccount.accountType = AccountType.Debit
        anotherDebitAccount.activeStatus = true
        anotherDebitAccount.moniker = "1003"
        anotherDebitAccount.outstanding = new BigDecimal("0.00")
        anotherDebitAccount.future = new BigDecimal("0.00")
        anotherDebitAccount.cleared = new BigDecimal("1000.00")
        anotherDebitAccount.dateClosed = new Timestamp(System.currentTimeMillis())
        anotherDebitAccount.validationDate = new Timestamp(System.currentTimeMillis())
        accountRepository.save(anotherDebitAccount)

        and: "payment input data attempting to pay a debit account"
        def paymentInput = [
            sourceAccount: "checking_brian",
            destinationAccount: "savings_brian", // This is a debit account
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> paymentInput }] as graphql.schema.DataFetchingEnvironment

        when: "create payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.createPayment()
        dataFetcher.get(environment)

        then: "should throw validation exception for payment to debit account"
        thrown(RuntimeException)
    }

    def "should successfully execute GraphQL operations in integration environment without authentication issues"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "an existing payment in the database"
        createTestPayment("checking_brian", "discover_it", new BigDecimal("50.00"))

        when: "payments data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payments
        def payments = dataFetcher.get(null)

        then: "should execute successfully in integration environment"
        payments != null
        payments.size() >= 1
        payments.any { it.amount == new BigDecimal("50.00") }
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

        return paymentService.insertPaymentNew(payment)
    }
}