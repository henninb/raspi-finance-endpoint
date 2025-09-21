package finance.graphql

import finance.controllers.TransferGraphQLController
import finance.domain.Transfer
import finance.services.ITransferService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.security.test.context.support.WithMockUser
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.util.Optional

@GraphQlTest(TransferGraphQLController)
@WithMockUser(authorities = ["USER"]) // satisfy @PreAuthorize on mutations
class TransferGraphQLSliceSpec extends Specification {

    @Autowired
    GraphQlTester graphQlTester

    @SpringBean
    ITransferService transferService = Mock()

    @SpringBean
    MeterRegistry meterRegistry = Mock()

    def setup() {
        meterRegistry.counter(_ as String) >> Mock(Counter)
    }

    def "slice: should fetch all transfers"() {
        given:
        def t1 = createTransfer(1L, "source_account", "dest_account", new BigDecimal("100.00"))
        def t2 = createTransfer(2L, "account_a", "account_b", new BigDecimal("250.00"))
        transferService.findAllTransfers() >> [t1, t2]

        when:
        def doc = """
        query {
          transfers {
            transferId
            sourceAccount
            destinationAccount
            amount
          }
        }
        """.stripIndent()

        then:
        graphQlTester.document(doc)
            .execute()
            .path("transfers").entityList(Transfer).hasSize(2)
            .path("transfers[0].transferId").entity(Long).isEqualTo(1L)
            .path("transfers[0].sourceAccount").entity(String).isEqualTo("source_account")
            .path("transfers[0].destinationAccount").entity(String).isEqualTo("dest_account")
            .path("transfers[0].amount").entity(BigDecimal).isEqualTo(new BigDecimal("100.00"))
    }

    def "slice: should create transfer mutation"() {
        given:
        def created = createTransfer(1L, "source_account", "dest_account", new BigDecimal("150.00"))
        created.transactionDate = Date.valueOf("2023-12-01")
        transferService.insertTransfer(_ as Transfer) >> { Transfer input ->
            assert input.sourceAccount == "source_account"
            assert input.destinationAccount == "dest_account"
            assert input.amount == new BigDecimal("150.00")
            assert input.transactionDate == Date.valueOf("2023-12-01")
            return created
        }

        when:
        def doc = """
        mutation(${'$'}t: TransferInput!) {
          createTransfer(transfer: ${'$'}t) {
            transferId
            sourceAccount
            destinationAccount
            amount
          }
        }
        """.stripIndent()

        def vars = [
            t: [
                sourceAccount   : "source_account",
                destinationAccount: "dest_account",
                transactionDate : "2023-12-01",
                amount          : new BigDecimal("150.00")
            ]
        ]

        then:
        graphQlTester.document(doc)
            .variables(vars)
            .execute()
            .path("createTransfer.transferId").entity(Long).isEqualTo(1L)
            .path("createTransfer.sourceAccount").entity(String).isEqualTo("source_account")
            .path("createTransfer.destinationAccount").entity(String).isEqualTo("dest_account")
            .path("createTransfer.amount").entity(BigDecimal).isEqualTo(new BigDecimal("150.00"))
    }

    private static Transfer createTransfer(Long id, String src, String dest, BigDecimal amt) {
        def t = new Transfer()
        t.transferId = id
        t.sourceAccount = src
        t.destinationAccount = dest
        t.amount = amt
        t.transactionDate = Date.valueOf("2023-12-01")
        t.activeStatus = true
        return t
    }
}

