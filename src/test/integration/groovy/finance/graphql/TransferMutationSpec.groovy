package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.helpers.GraphQLIntegrationContext
import finance.helpers.TransferTestScenario
import finance.domain.Transfer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import java.math.BigDecimal
import java.sql.Date
import jakarta.validation.ConstraintViolationException

class TransferMutationSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @spock.lang.Shared
    GraphQLIntegrationContext gqlCtx
    @spock.lang.Shared
    TransferTestScenario transferScenario

    def setupSpec() {
        gqlCtx = testFixtures.createGraphQLIntegrationContext(testOwner)
        transferScenario = gqlCtx.createTransferScenario()
    }

    private String getSrcName() { transferScenario.sourceAccountName }
    private String getDestName() { transferScenario.destinationAccountName }

    def setup() {
        // Ensure accounts exist inside the test transaction
        testDataManager.createAccountFor(testOwner, "source", "debit", true)
        testDataManager.createAccountFor(testOwner, "dest", "debit", true)
    }

    private static void withUserAuthority() {
        def auth = new UsernamePasswordAuthenticationToken(
                "test-user",
                "N/A",
                [new SimpleGrantedAuthority("USER")]
        )
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def "createTransfer mutation succeeds with valid input"() {
        given:
        withUserAuthority()

        when:
        def result = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        Date.valueOf("2024-02-01"),
                        new BigDecimal("300.00"),
                        null
                )
        )

        then:
        result != null
        result.transferId > 0
        result.sourceAccount == srcName
        result.destinationAccount == destName
        result.amount == new BigDecimal("300.00")
        result.transactionDate == Date.valueOf("2024-02-01")
        result.activeStatus == true
    }

    def "createTransfer mutation fails validation for negative amount"() {
        given:
        withUserAuthority()

        when:
        mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        Date.valueOf("2024-02-01"),
                        new BigDecimal("-1.00"),
                        null
                )
        )

        then:
        thrown(ConstraintViolationException)
    }

    def "deleteTransfer mutation returns true for existing transfer"() {
        given:
        withUserAuthority()
        def created = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        Date.valueOf("2024-02-02"),
                        new BigDecimal("25.00"),
                        null
                )
        )

        when:
        def deleted = mutationController.deleteTransfer(created.transferId)

        then:
        deleted == true
    }

    def "deleteTransfer mutation returns false for missing id"() {
        given:
        withUserAuthority()

        expect:
        mutationController.deleteTransfer(-9999L) == false
    }
}
