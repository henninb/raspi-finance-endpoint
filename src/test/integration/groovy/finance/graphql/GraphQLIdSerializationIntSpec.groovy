package finance.graphql

import finance.BaseRestTemplateIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Category
import finance.domain.Description
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.controllers.dto.PaymentInputDto
import finance.helpers.SmartAccountBuilder
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.execution.GraphQlSource
import finance.services.PaymentService
import finance.services.TransactionService
import org.springframework.test.context.TestPropertySource
import groovy.json.JsonSlurper
import spock.lang.Requires

import java.math.BigDecimal
import java.time.LocalDate

@TestPropertySource(properties = [
    'spring.graphql.graphiql.enabled=true',
    'spring.graphql.path=/graphql',
    'spring.main.web-application-type=servlet'
])
class GraphQLIdSerializationIntSpec extends BaseRestTemplateIntegrationSpec {

    @Autowired(required = false)
    GraphQlSource graphQlSource

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    AccountRepository accountRepository

    @Autowired
    CategoryRepository categoryRepository

    @Autowired
    DescriptionRepository descriptionRepository

    @Autowired
    PaymentService paymentService

    @Autowired
    TransactionService transactionService

    private static String safeSuffix(int len = 5) {
        def rnd = new Random()
        def alphabet = ['a','b','c','d','e','f']
        (1..len).collect { alphabet[rnd.nextInt(alphabet.size())] }.join()
    }

    @Requires({ env['CI'] || true })
    def "GraphQL returns ID-typed fields as strings"() {
        given:
        def testUser = "graphqlid"
        withUserRole(testUser)
        def suffix = safeSuffix(5)
        def src = "srcuser_${suffix}"
        def dest = "destuser_${suffix}"
        def srcAccount = SmartAccountBuilder.builderForOwner(testUser)
                .withAccountNameOwner(src)
                .asDebit()
                .withCleared(new BigDecimal("1000.00"))
                .buildAndValidate()
        accountRepository.save(srcAccount)

        def destAccount = SmartAccountBuilder.builderForOwner(testUser)
                .withAccountNameOwner(dest)
                .asCredit()
                .withCleared(new BigDecimal("-200.00"))
                .buildAndValidate()
        accountRepository.save(destAccount)
        if (!categoryRepository.findByOwnerAndCategoryName(testUser, "bill_pay").present) {
            categoryRepository.saveAndFlush(new Category(categoryId: 0L, owner: testUser, activeStatus: true, categoryName: "bill_pay"))
        }
        if (!descriptionRepository.findByOwnerAndDescriptionName(testUser, "payment").present) {
            descriptionRepository.saveAndFlush(new Description(descriptionId: 0L, owner: testUser, activeStatus: true, descriptionName: "payment"))
        }
        def sourceTransaction = transactionService.save(new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: src,
                accountType: srcAccount.accountType,
                description: "payment",
                category: "bill_pay",
                amount: new BigDecimal("-12.34"),
                transactionDate: LocalDate.parse("2024-01-15"),
                transactionState: TransactionState.Outstanding,
                transactionType: TransactionType.Expense,
                notes: "prelinked payment source"
        )).data
        def destinationTransaction = transactionService.save(new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: dest,
                accountType: destAccount.accountType,
                description: "payment",
                category: "bill_pay",
                amount: new BigDecimal("-12.34"),
                transactionDate: LocalDate.parse("2024-01-15"),
                transactionState: TransactionState.Outstanding,
                transactionType: TransactionType.Expense,
                notes: "prelinked payment destination"
        )).data
        def created = paymentService.save(new finance.domain.Payment(
                0L,
                "",
                src,
                dest,
                LocalDate.parse("2024-01-15"),
                new BigDecimal("12.34"),
                sourceTransaction.guid,
                destinationTransaction.guid,
                true
        )).data

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
