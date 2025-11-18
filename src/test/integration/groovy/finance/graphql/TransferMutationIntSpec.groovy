package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.helpers.GraphQLIntegrationContext
import finance.helpers.TransferTestScenario
import finance.domain.Transfer
import org.springframework.beans.factory.annotation.Autowired

import java.math.BigDecimal
import java.time.LocalDate
import jakarta.validation.ConstraintViolationException

class TransferMutationIntSpec extends BaseIntegrationSpec {

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


    def "createTransfer mutation succeeds with valid input"() {
        given:
        withUserRole()

        when:
        def result = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        LocalDate.parse("2024-02-01"),
                        new BigDecimal("300.00"),
                        null,
                        null,
                        null
                )
        )

        then:
        result != null
        result.transferId > 0
        result.sourceAccount == srcName
        result.destinationAccount == destName
        result.amount == new BigDecimal("300.00")
        result.transactionDate == LocalDate.parse("2024-02-01")
        result.activeStatus == true
    }

    def "createTransfer mutation fails validation for negative amount"() {
        given:
        withUserRole()

        when:
        mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        LocalDate.parse("2024-02-01"),
                        new BigDecimal("-1.00"),
                        null,
                        null,
                        null
                )
        )

        then:
        thrown(ConstraintViolationException)
    }

    def "createTransfer mutation fails when source account missing"() {
        given:
        withUserRole()

        when:
        mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        "nonexistent_${java.util.UUID.randomUUID().toString().take(8)}",
                        destName,
                        LocalDate.parse("2024-02-01"),
                        new BigDecimal("300.00"),
                        null,
                        null,
                        null
                )
        )

        then:
        thrown(RuntimeException)
    }

    def "deleteTransfer mutation returns true for existing transfer"() {
        given:
        withUserRole()
        def created = mutationController.createTransfer(
                new finance.controllers.dto.TransferInputDto(
                        null,
                        srcName,
                        destName,
                        LocalDate.parse("2024-02-02"),
                        new BigDecimal("25.00"),
                        null,
                        null,
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
        withUserRole()

        expect:
        mutationController.deleteTransfer(-9999L) == false
    }
}
