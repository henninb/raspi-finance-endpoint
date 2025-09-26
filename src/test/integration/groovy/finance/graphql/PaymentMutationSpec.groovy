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

class PaymentMutationSpec extends BaseIntegrationSpec {

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
                null,
                srcName,
                destName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("123.45"),
                null
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
                null,
                srcName,
                destName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("-5.00"),
                null
        )

        when:
        mutationController.createPayment(dto)

        then:
        thrown(ConstraintViolationException)
    }
}
