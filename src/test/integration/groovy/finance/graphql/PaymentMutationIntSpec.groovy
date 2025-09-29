package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.helpers.GraphQLIntegrationContext
import finance.helpers.PaymentTestScenario
import org.springframework.beans.factory.annotation.Autowired

import java.math.BigDecimal
import java.sql.Date
import jakarta.validation.ConstraintViolationException

class PaymentMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController


    @spock.lang.Shared
    GraphQLIntegrationContext gqlCtx
    @spock.lang.Shared
    PaymentTestScenario payScenario

    def setupSpec() {
        gqlCtx = testFixtures.createGraphQLIntegrationContext(testOwner)
        payScenario = gqlCtx.createPaymentScenario()
    }

    private String getSrcName() { payScenario.sourceAccountName }
    private String getDestName() { payScenario.destinationAccountName }

    def setup() {
        // Ensure accounts exist inside the test transaction
        testDataManager.createAccountFor(testOwner, "source", "debit", true)
        testDataManager.createAccountFor(testOwner, "dest", "credit", true)
    }

    def "createPayment mutation succeeds with valid input"() {
        given:
        withUserRole()
        def dto = new PaymentInputDto(
                null,                           // paymentId
                srcName,                        // sourceAccount
                destName,                       // destinationAccount
                Date.valueOf("2024-01-15"),     // transactionDate
                new BigDecimal("123.45"),       // amount
                null,                           // guidSource
                null,                           // guidDestination
                null                            // activeStatus
        )

        when:
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.paymentId > 0
        result.sourceAccount == srcName
        result.destinationAccount == destName
        result.amount == new BigDecimal("123.45")
        result.transactionDate == Date.valueOf("2024-01-15")
        result.activeStatus == true
    }

    def "createPayment mutation fails validation for negative amount"() {
        given:
        withUserRole()
        def dto = new PaymentInputDto(
                null,                           // paymentId
                srcName,                        // sourceAccount
                destName,                       // destinationAccount
                Date.valueOf("2024-01-15"),     // transactionDate
                new BigDecimal("-5.00"),        // amount
                null,                           // guidSource
                null,                           // guidDestination
                null                            // activeStatus
        )

        when:
        mutationController.createPayment(dto)

        then:
        thrown(ConstraintViolationException)
    }

    def "createPayment mutation fails when source account invalid format"() {
        given:
        withUserRole()
        def dto = new PaymentInputDto(
                null,
                "ab",                         // invalid: too short / non-existent
                destName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when:
        mutationController.createPayment(dto)

        then:
        thrown(RuntimeException)
    }

    def "createPayment mutation fails when destination is debit account"() {
        given:
        withUserRole()
        // Create an extra debit account to use as invalid destination
        String debitDest = testDataManager.createAccountFor(testOwner, "savings", "debit", true)
        def dto = new PaymentInputDto(
                null,
                srcName,
                debitDest,                      // invalid destination: debit account
                Date.valueOf("2024-01-15"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when:
        mutationController.createPayment(dto)

        then:
        thrown(RuntimeException)
    }

    def "deletePayment mutation returns true for existing payment"() {
        given:
        withUserRole()
        def createDto = new PaymentInputDto(
                null,                           // paymentId
                srcName,                        // sourceAccount
                destName,                       // destinationAccount
                Date.valueOf("2024-01-15"),     // transactionDate
                new BigDecimal("55.00"),        // amount
                null,                           // guidSource
                null,                           // guidDestination
                null                            // activeStatus
        )
        def created = mutationController.createPayment(createDto)

        when:
        def deleted = mutationController.deletePayment(created.paymentId)

        then:
        deleted == true
    }

    def "deletePayment mutation returns false for missing payment id"() {
        given:
        withUserRole()

        expect:
        mutationController.deletePayment(-9999L) == false
    }
}
