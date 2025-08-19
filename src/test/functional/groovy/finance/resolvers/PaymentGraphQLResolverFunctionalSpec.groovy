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

@ActiveProfiles("func")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
class PaymentGraphQLResolverFunctionalSpec extends Specification {

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
        sourceAccount.accountNameOwner = "paymentsource_brian"
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
        destinationAccount.accountNameOwner = "paymentdest_brian"
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

    def "should fetch all payments via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "existing payments in the database"
        createTestPayment("paymentsource_brian", "paymentdest_brian", new BigDecimal("100.00"))
        createTestPayment("paymentsource_brian", "paymentdest_brian", new BigDecimal("200.00"))

        when: "payments data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payments
        def payments = dataFetcher.get(null)

        then: "should return all payments from database in functional environment"
        payments.size() == 2
        payments.every { it instanceof Payment }
        payments.any { it.amount == new BigDecimal("100.00") }
        payments.any { it.amount == new BigDecimal("200.00") }
    }

    def "should create payment via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "payment input data"
        def paymentInput = [
            sourceAccount: "paymentsource_brian",
            destinationAccount: "paymentdest_brian",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("250.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> paymentInput }] as graphql.schema.DataFetchingEnvironment

        when: "create payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.createPayment()
        def result = dataFetcher.get(environment)

        then: "should create and return payment from database in functional environment"
        result != null
        result.paymentId > 0
        result.sourceAccount == "paymentsource_brian"
        result.destinationAccount == "paymentdest_brian"
        result.amount == new BigDecimal("250.00")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.guidSource != null
        result.guidDestination != null
        result.activeStatus == true

        and: "payment should be persisted in database"
        def savedPayment = paymentRepository.findByPaymentId(result.paymentId)
        savedPayment.isPresent()
        savedPayment.get().sourceAccount == "paymentsource_brian"

        and: "corresponding debit and credit transactions should be created"
        def transactions = transactionRepository.findAll()
        transactions.size() == 2
        transactions.any { it.guid == result.guidSource }
        transactions.any { it.guid == result.guidDestination }
    }

    def "should fetch payment by ID via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "an existing payment in the database"
        def savedPayment = createTestPayment("paymentsource_brian", "paymentdest_brian", new BigDecimal("150.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedPayment.paymentId }] as graphql.schema.DataFetchingEnvironment

        when: "payment data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payment()
        def result = dataFetcher.get(environment)

        then: "should return the specific payment from database in functional environment"
        result != null
        result.paymentId == savedPayment.paymentId
        result.sourceAccount == "paymentsource_brian"
        result.destinationAccount == "paymentdest_brian"
        result.amount == new BigDecimal("150.00")
    }

    def "should delete payment via GraphQL resolver in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "an existing payment in the database"
        def savedPayment = createTestPayment("paymentsource_brian", "paymentdest_brian", new BigDecimal("75.00"))

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> savedPayment.paymentId }] as graphql.schema.DataFetchingEnvironment

        when: "delete payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.deletePayment()
        def result = dataFetcher.get(environment)

        then: "should successfully delete payment and return true in functional environment"
        result == true

        and: "payment should be removed from database"
        def deletedPayment = paymentRepository.findByPaymentId(savedPayment.paymentId)
        !deletedPayment.isPresent()
    }

    def "should handle validation errors for invalid payment creation in functional environment"() {
        given: "test accounts are created"
        setupTestAccounts()
        
        and: "invalid payment input data (non-existent destination account)"
        def paymentInput = [
            sourceAccount: "paymentsource_brian",
            destinationAccount: "nonexistent_account",
            transactionDate: "2024-01-15",
            amount: new BigDecimal("100.00")
        ]

        and: "mocked data fetching environment"
        def environment = [getArgument: { String arg -> paymentInput }] as graphql.schema.DataFetchingEnvironment

        when: "create payment mutation is called"
        def dataFetcher = paymentGraphQLResolver.createPayment()
        dataFetcher.get(environment)

        then: "should throw runtime exception in functional environment"
        thrown(RuntimeException)
    }

    def "should successfully execute GraphQL operations in functional environment without authentication issues"() {
        given: "test accounts are created"
        setupTestAccounts()

        and: "an existing payment in the database"
        createTestPayment("paymentsource_brian", "paymentdest_brian", new BigDecimal("50.00"))

        when: "payments data fetcher is called"
        def dataFetcher = paymentGraphQLResolver.payments
        def payments = dataFetcher.get(null)

        then: "should execute successfully in functional environment"
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