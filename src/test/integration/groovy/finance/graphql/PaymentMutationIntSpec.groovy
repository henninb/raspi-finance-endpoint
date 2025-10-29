package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
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

    def "createPayment mutation succeeds when destination is asset account (TRANSFER behavior)"() {
        given:
        withUserRole()
        // Create a savings (asset/debit) account as destination
        String savingsAccount = testDataManager.createAccountFor(testOwner, "savings", "savings", true)
        def dto = new PaymentInputDto(
                null,
                srcName,
                savingsAccount,                 // asset to asset = TRANSFER
                Date.valueOf("2024-01-15"),
                new BigDecimal("100.00"),
                null,
                null,
                null
        )

        when:
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.paymentId > 0
        result.sourceAccount == srcName
        result.destinationAccount == savingsAccount
        result.amount == new BigDecimal("100.00")
    }

    def "createPayment mutation succeeds with liability to asset (CASH_ADVANCE behavior)"() {
        given:
        withUserRole()
        // Create credit card (liability) source and checking (asset) destination
        String creditCardAccount = testDataManager.createAccountFor(testOwner, "credit_card", "credit_card", true)
        String checkingAccount = testDataManager.createAccountFor(testOwner, "checking", "checking", true)
        def dto = new PaymentInputDto(
                null,
                creditCardAccount,              // liability source
                checkingAccount,                // asset destination = CASH_ADVANCE
                Date.valueOf("2024-01-15"),
                new BigDecimal("200.00"),
                null,
                null,
                null
        )

        when:
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.paymentId > 0
        result.sourceAccount == creditCardAccount
        result.destinationAccount == checkingAccount
        result.amount == new BigDecimal("200.00")
    }

    def "createPayment mutation succeeds with liability to liability (BALANCE_TRANSFER behavior)"() {
        given:
        withUserRole()
        // Create two credit card (liability) accounts
        String creditCard1 = testDataManager.createAccountFor(testOwner, "credit_card_1", "credit_card", true)
        String creditCard2 = testDataManager.createAccountFor(testOwner, "credit_card_2", "credit_card", true)
        def dto = new PaymentInputDto(
                null,
                creditCard1,                    // liability source
                creditCard2,                    // liability destination = BALANCE_TRANSFER
                Date.valueOf("2024-01-15"),
                new BigDecimal("500.00"),
                null,
                null,
                null
        )

        when:
        def result = mutationController.createPayment(dto)

        then:
        result != null
        result.paymentId > 0
        result.sourceAccount == creditCard1
        result.destinationAccount == creditCard2
        result.amount == new BigDecimal("500.00")
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
