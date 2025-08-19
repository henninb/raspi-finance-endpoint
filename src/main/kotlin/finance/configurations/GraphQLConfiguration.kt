package finance.configurations

import com.google.common.io.Resources
import finance.resolvers.PaymentGraphQLResolver
import finance.resolvers.TransferGraphQLResolver
import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.AsyncSerialExecutionStrategy
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLConfiguration(
    private val paymentGraphQLResolver: PaymentGraphQLResolver,
    private val transferGraphQLResolver: TransferGraphQLResolver
) {

    @Bean
    open fun graphQL(): GraphQL {
        val url = Resources.getResource("graphql/schema.graphqls")
        val sdl = Resources.toString(url, Charsets.UTF_8)
        val typeDefinitionRegistry = SchemaParser().parse(sdl)

        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query") { builder ->
                builder
                    .dataFetcher("payments", paymentGraphQLResolver.payments)
                    .dataFetcher("payment", paymentGraphQLResolver.payment())
                    .dataFetcher("transfers", transferGraphQLResolver.transfers)
                    .dataFetcher("transfer", transferGraphQLResolver.transfer())
            }
            .type("Mutation") { builder ->
                builder
                    .dataFetcher("createPayment", paymentGraphQLResolver.createPayment())
                    .dataFetcher("deletePayment", paymentGraphQLResolver.deletePayment())
                    .dataFetcher("createTransfer", transferGraphQLResolver.createTransfer())
                    .dataFetcher("deleteTransfer", transferGraphQLResolver.deleteTransfer())
            }
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.Date)
            .build()

        val schema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .mutationExecutionStrategy(AsyncSerialExecutionStrategy())
            .build()
    }
}