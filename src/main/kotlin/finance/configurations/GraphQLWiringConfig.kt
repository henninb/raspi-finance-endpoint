package finance.configurations

import finance.resolvers.PaymentGraphQLResolver
import finance.resolvers.TransferGraphQLResolver
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
open class GraphQLWiringConfig(
    private val paymentGraphQLResolver: PaymentGraphQLResolver,
    private val transferGraphQLResolver: TransferGraphQLResolver
) {

    @Bean
    open fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.Date)
                .type("Query") { typeBuilder ->
                    typeBuilder
                        .dataFetcher("payments", paymentGraphQLResolver.payments)
                        .dataFetcher("payment", paymentGraphQLResolver.payment())
                        .dataFetcher("transfers", transferGraphQLResolver.transfers)
                        .dataFetcher("transfer", transferGraphQLResolver.transfer())
                }
                .type("Mutation") { typeBuilder ->
                    typeBuilder
                        .dataFetcher("createPayment", paymentGraphQLResolver.createPayment())
                        .dataFetcher("deletePayment", paymentGraphQLResolver.deletePayment())
                        .dataFetcher("createTransfer", transferGraphQLResolver.createTransfer())
                        .dataFetcher("deleteTransfer", transferGraphQLResolver.deleteTransfer())
                }
        }
    }
}
