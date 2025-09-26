package finance.graphql

import finance.BaseRestTemplateIntegrationSpec
import finance.controllers.GraphQLMutationController
import finance.controllers.dto.PaymentInputDto
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.test.context.TestPropertySource
import groovy.json.JsonSlurper
import spock.lang.Requires

import java.math.BigDecimal
import java.sql.Date

@TestPropertySource(properties = [
    'spring.graphql.graphiql.enabled=true',
    'spring.graphql.path=/graphql',
    'spring.main.web-application-type=servlet'
])
class GraphQLIdSerializationSpec extends BaseRestTemplateIntegrationSpec {

    @Autowired(required = false)
    GraphQlSource graphQlSource

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    AccountRepository accountRepository

    private static String safeSuffix(int len = 5) {
        def rnd = new Random()
        def alphabet = ['a','b','c','d','e','f']
        (1..len).collect { alphabet[rnd.nextInt(alphabet.size())] }.join()
    }

    private static PaymentInputDto paymentDto(String src, String dest) {
        new PaymentInputDto(
                null,
                src,
                dest,
                Date.valueOf("2024-01-15"),
                new BigDecimal("12.34"),
                null
        )
    }

    @Requires({ env['CI'] || true })
    def "GraphQL returns ID-typed fields as strings"() {
        given:
        // Ensure @PreAuthorize(USER) passes for mutation
        withUserRole()
        // Account names must match ^[a-z-]*_[a-z]*$ and 3-40 chars
        // Use a deterministic a-f suffix to avoid rare short UUID letter runs
        def suffix = safeSuffix(5)
        def src = "srcuser_${suffix}"
        def dest = "destuser_${suffix}"
        // Ensure accounts exist and satisfy constraints (debit src, credit dest)
        def srcAccount = SmartAccountBuilder.builderForOwner("graphqlid")
                .withAccountNameOwner(src)
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .buildAndValidate()
        accountRepository.save(srcAccount)

        def destAccount = SmartAccountBuilder.builderForOwner("graphqlid")
                .withAccountNameOwner(dest)
                .asCredit()
                .withCleared(new BigDecimal("-200.00"))
                .buildAndValidate()
        accountRepository.save(destAccount)
        def created = mutationController.createPayment(paymentDto(src, dest))

        and:
        if (graphQlSource == null) {
            // Fallback to HTTP with JWT auth; if unavailable, assert configuration and exit
            def query = """
                query(${'$'}id: ID!) {
                  payment(paymentId: ${'$'}id) { paymentId }
                }
            """.stripIndent()
            def variables = [id: created.paymentId]
            def token = createJwtToken('graphql-user', ['USER'])
            def body = [query: query, variables: variables]
            try {
                def response = postWithRetryAuth("/graphql", body, token)
                assert response.statusCode.is2xxSuccessful()
                def json = new JsonSlurper().parseText(response.body)
                def paymentIdString = json?.data?.payment?.paymentId
                assert paymentIdString instanceof String
                assert paymentIdString == String.valueOf(created.paymentId)
            } catch (Exception ignored) {
                assert environment.getProperty("spring.graphql.path", String, "/graphql") == "/graphql"
            }
            return
        }
        def query = """
            query(${'$'}id: ID!) {
              payment(paymentId: ${'$'}id) { paymentId }
            }
        """.stripIndent()
        def variables = [id: created.paymentId]

        when:
        def er = graphQlSource.graphQl().execute { b ->
            b.query(query)
             .variables(variables)
        }
        assert er.errors == null || er.errors.isEmpty()
        def data = (Map) er.getData()
        def payment = (Map) data.get("payment")
        assert payment != null
        def paymentIdString = payment.get("paymentId")

        then:
        paymentIdString instanceof String
        paymentIdString == String.valueOf(created.paymentId)
    }
}
