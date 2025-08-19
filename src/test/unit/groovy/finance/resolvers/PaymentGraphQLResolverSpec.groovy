package finance.resolvers

import finance.domain.Payment
import finance.services.IPaymentService
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import spock.lang.Specification
import spock.lang.Subject

import java.math.BigDecimal
import java.sql.Date
import java.util.*

class PaymentGraphQLResolverSpec extends Specification {

    IPaymentService paymentService = Mock()
    MeterRegistry meterRegistry = Mock()
    Counter counter = Mock()

    @Subject
    PaymentGraphQLResolver paymentGraphQLResolver = new PaymentGraphQLResolver(paymentService, meterRegistry)

    def setup() {
        meterRegistry.counter(_ as String) >> counter
    }

    def "should fetch all payments"() {
        given: "a list of payments"
        def payment1 = createTestPayment(1L, "checking_brian", "discover_it", new BigDecimal("100.00"))
        def payment2 = createTestPayment(2L, "chase_freedom", "capital_one", new BigDecimal("250.00"))
        def payments = [payment1, payment2]

        and: "payment service returns payments"
        paymentService.findAllPayments() >> payments

        when: "payments data fetcher is called"
        DataFetcher<List<Payment>> dataFetcher = paymentGraphQLResolver.payments
        DataFetchingEnvironment environment = Mock()
        List<Payment> result = dataFetcher.get(environment)

        then: "should return all payments"
        result.size() == 2
        result[0].paymentId == 1L
        result[0].sourceAccount == "checking_brian"
        result[0].destinationAccount == "discover_it"
        result[0].amount == new BigDecimal("100.00")

        result[1].paymentId == 2L
        result[1].sourceAccount == "chase_freedom"
        result[1].destinationAccount == "capital_one"
        result[1].amount == new BigDecimal("250.00")
    }

    def "should fetch payment by ID"() {
        given: "a payment ID"
        def paymentId = 1L
        def payment = createTestPayment(paymentId, "checking_brian", "discover_it", new BigDecimal("100.00"))

        and: "payment service returns the payment"
        paymentService.findByPaymentId(paymentId) >> Optional.of(payment)

        and: "data fetching environment with payment ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("paymentId") >> paymentId

        when: "payment data fetcher is called"
        DataFetcher<Payment> dataFetcher = paymentGraphQLResolver.payment()
        Payment result = dataFetcher.get(environment)

        then: "should return the specific payment"
        result.paymentId == paymentId
        result.sourceAccount == "checking_brian"
        result.destinationAccount == "discover_it"
        result.amount == new BigDecimal("100.00")
    }

    def "should handle payment not found when fetching by ID"() {
        given: "a non-existent payment ID"
        def paymentId = 999L

        and: "payment service returns empty optional"
        paymentService.findByPaymentId(paymentId) >> Optional.empty()

        and: "data fetching environment with payment ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("paymentId") >> paymentId

        when: "payment data fetcher is called"
        DataFetcher<Payment> dataFetcher = paymentGraphQLResolver.payment()
        Payment result = dataFetcher.get(environment)

        then: "should return null"
        result == null
    }

    def "should create payment with validation"() {
        given: "payment input data"
        def paymentInput = [
            sourceAccount: "checking_brian",
            destinationAccount: "discover_it",
            transactionDate: "2023-12-01",
            amount: new BigDecimal("150.00")
        ]

        and: "expected created payment"
        def createdPayment = createTestPayment(1L, "checking_brian", "discover_it", new BigDecimal("150.00"))
        createdPayment.transactionDate = Date.valueOf("2023-12-01")

        and: "data fetching environment with payment input"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("payment") >> paymentInput

        when: "create payment mutation is called"
        DataFetcher<Payment> dataFetcher = paymentGraphQLResolver.createPayment()
        Payment result = dataFetcher.get(environment)

        then: "should call payment service with proper payment object and return created payment"
        1 * paymentService.insertPaymentNew({ Payment payment ->
            payment.sourceAccount == "checking_brian" &&
            payment.destinationAccount == "discover_it" &&
            payment.amount == new BigDecimal("150.00") &&
            payment.transactionDate == Date.valueOf("2023-12-01") &&
            payment.guidSource != null &&
            payment.guidDestination != null
        }) >> createdPayment

        and: "should return created payment"
        result.paymentId == 1L
        result.sourceAccount == "checking_brian"
        result.destinationAccount == "discover_it"
        result.amount == new BigDecimal("150.00")
        result.transactionDate == Date.valueOf("2023-12-01")
    }

    def "should handle validation errors during payment creation"() {
        given: "invalid payment input data"
        def paymentInput = [
            sourceAccount: "", // Invalid empty source account
            destinationAccount: "discover_it",
            transactionDate: "2023-12-01",
            amount: new BigDecimal("-100.00") // Invalid negative amount
        ]

        and: "payment service throws validation exception"
        paymentService.insertPaymentNew(_ as Payment) >> {
            throw new RuntimeException("Validation failed: Source account cannot be empty")
        }

        and: "data fetching environment with invalid payment input"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("payment") >> paymentInput

        when: "create payment mutation is called"
        DataFetcher<Payment> dataFetcher = paymentGraphQLResolver.createPayment()
        dataFetcher.get(environment)

        then: "should throw runtime exception"
        thrown(RuntimeException)
    }

    def "should delete payment by ID"() {
        given: "a payment ID"
        def paymentId = 1L

        and: "payment service successfully deletes payment"
        paymentService.deleteByPaymentId(paymentId) >> true

        and: "data fetching environment with payment ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("paymentId") >> paymentId

        when: "delete payment mutation is called"
        DataFetcher<Boolean> dataFetcher = paymentGraphQLResolver.deletePayment()
        Boolean result = dataFetcher.get(environment)

        then: "should return true for successful deletion"
        result == true
    }

    def "should handle payment not found during deletion"() {
        given: "a non-existent payment ID"
        def paymentId = 999L

        and: "payment service returns false"
        paymentService.deleteByPaymentId(paymentId) >> false

        and: "data fetching environment with payment ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("paymentId") >> paymentId

        when: "delete payment mutation is called"
        DataFetcher<Boolean> dataFetcher = paymentGraphQLResolver.deletePayment()
        Boolean result = dataFetcher.get(environment)

        then: "should return false"
        result == false
    }

    def "should increment metrics counters on successful operations"() {
        given: "a list of payments"
        def payments = [createTestPayment(1L, "checking_brian", "discover_it", new BigDecimal("100.00"))]
        paymentService.findAllPayments() >> payments

        when: "payments data fetcher is called"
        DataFetcher<List<Payment>> dataFetcher = paymentGraphQLResolver.payments
        DataFetchingEnvironment environment = Mock()
        dataFetcher.get(environment)

        then: "should increment success counter"
        1 * meterRegistry.counter("graphql.payments.fetch.success") >> counter
        1 * counter.increment()
    }

    def "should increment metrics counters on failed operations"() {
        given: "payment service throws exception"
        paymentService.findAllPayments() >> { throw new RuntimeException("Database error") }

        when: "payments data fetcher is called"
        DataFetcher<List<Payment>> dataFetcher = paymentGraphQLResolver.payments
        DataFetchingEnvironment environment = Mock()
        dataFetcher.get(environment)

        then: "should throw exception and increment error counter"
        thrown(RuntimeException)
        1 * meterRegistry.counter("graphql.payments.fetch.error") >> counter
        1 * counter.increment()
    }

    def "should handle authorization for payment creation"() {
        given: "valid payment input data"
        def paymentInput = [
            sourceAccount: "checking_brian",
            destinationAccount: "discover_it",
            transactionDate: "2023-12-01",
            amount: new BigDecimal("150.00")
        ]

        and: "expected created payment"
        def createdPayment = createTestPayment(1L, "checking_brian", "discover_it", new BigDecimal("150.00"))

        and: "data fetching environment with payment input"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("payment") >> paymentInput

        when: "create payment mutation is called with authorization"
        DataFetcher<Payment> dataFetcher = paymentGraphQLResolver.createPayment()
        Payment result = dataFetcher.get(environment)

        then: "should successfully create payment"
        1 * paymentService.insertPaymentNew(_ as Payment) >> createdPayment
        result.paymentId == 1L
    }

    def "should handle authorization for payment deletion"() {
        given: "a payment ID"
        def paymentId = 1L

        and: "payment service successfully deletes payment"
        paymentService.deleteByPaymentId(paymentId) >> true

        and: "data fetching environment with payment ID argument"
        DataFetchingEnvironment environment = Mock()
        environment.getArgument("paymentId") >> paymentId

        when: "delete payment mutation is called with authorization"
        DataFetcher<Boolean> dataFetcher = paymentGraphQLResolver.deletePayment()
        Boolean result = dataFetcher.get(environment)

        then: "should successfully delete payment"
        result == true
    }

    private Payment createTestPayment(Long id, String sourceAccount, String destAccount, BigDecimal amount) {
        def payment = new Payment()
        payment.paymentId = id
        payment.sourceAccount = sourceAccount
        payment.destinationAccount = destAccount
        payment.amount = amount
        payment.transactionDate = Date.valueOf("2023-12-01")
        payment.guidSource = UUID.randomUUID().toString()
        payment.guidDestination = UUID.randomUUID().toString()
        payment.activeStatus = true
        return payment
    }
}