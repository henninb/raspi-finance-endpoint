package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.helpers.GraphQLIntegrationContext
import finance.helpers.PaymentTestScenario
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.math.BigDecimal
import java.sql.Date

class PaymentDeleteMutationSpec extends BaseIntegrationSpec {

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

    private static void withUserAuthority() {
        def auth = new UsernamePasswordAuthenticationToken(
                "test-user",
                "N/A",
                [new SimpleGrantedAuthority("USER")]
        )
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def "deletePayment mutation returns true for existing payment"() {
        given:
        withUserAuthority()
        def createDto = new PaymentInputDto(
                null,
                srcName,
                destName,
                Date.valueOf("2024-01-15"),
                new BigDecimal("55.00"),
                null
        )
        def created = mutationController.createPayment(createDto)

        when:
        def deleted = mutationController.deletePayment(created.paymentId)

        then:
        deleted == true
    }

    def "deletePayment mutation returns false for missing payment id"() {
        given:
        withUserAuthority()

        expect:
        mutationController.deletePayment(-9999L) == false
    }
}
