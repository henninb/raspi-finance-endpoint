package finance.graphql

import finance.Application
import finance.domain.Transfer
import finance.services.ITransferService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import spock.lang.Specification

import java.math.BigDecimal
import java.sql.Date
import java.util.Optional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@ActiveProfiles("int")
@WithMockUser(authorities = ["USER"]) // satisfies @PreAuthorize on mutations
class TransferGraphQLIntegrationSpec extends Specification {

    // @Autowired(required = false)
    // GraphQlTester graphQlTester

    @Autowired
    finance.controllers.TransferGraphQLController transferGraphQLController

    @Autowired
    Environment environment

    // Mock the service to keep the test focused on the GraphQL layer
    @SpringBean
    ITransferService transferService = Mock()

    WebTestClient webTestClient

    def setup() {
        int port = environment.getProperty("local.server.port", Integer.class, 8080)
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:${port}")
                .build()
    }

    def "should fetch all transfers via GraphQL"() {
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
        webTestClient.post()
            .uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue([query: doc])
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath('$.data.transfers[0].transferId').isEqualTo(1)
            .jsonPath('$.data.transfers[0].sourceAccount').isEqualTo('source_account')
            .jsonPath('$.data.transfers[0].destinationAccount').isEqualTo('dest_account')
            .jsonPath('$.data.transfers[0].amount').isEqualTo(100.00)
    }

    def "should fetch transfer by ID via GraphQL"() {
        given:
        def transferId = 1L
        def t = createTransfer(transferId, "source_account", "dest_account", new BigDecimal("100.00"))
        transferService.findByTransferId(transferId) >> Optional.of(t)

        when:
        def result = transferGraphQLController.transfer(transferId)

        then:
        result != null
        result.transferId == transferId
        result.sourceAccount == 'source_account'
        result.destinationAccount == 'dest_account'
        result.amount == new BigDecimal('100.00')
    }

    def "should create transfer via GraphQL mutation"() {
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
        def input = new finance.controllers.TransferGraphQLController.TransferInput(
            'source_account', 'dest_account', '2023-12-01', new BigDecimal('150.00'), true
        )
        def createdRes = transferGraphQLController.createTransfer(input)

        then:
        createdRes.transferId == 1L
        createdRes.sourceAccount == 'source_account'
        createdRes.destinationAccount == 'dest_account'
        createdRes.amount == new BigDecimal('150.00')
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
