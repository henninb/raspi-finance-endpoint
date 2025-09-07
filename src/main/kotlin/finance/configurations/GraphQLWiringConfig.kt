package finance.configurations

import finance.configurations.SqlDateScalar
import finance.configurations.TimestampScalar
import finance.resolvers.AccountGraphQLResolver
import finance.resolvers.PaymentGraphQLResolver
import finance.resolvers.TransferGraphQLResolver
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
open class GraphQLWiringConfig(
    private val paymentGraphQLResolver: PaymentGraphQLResolver,
    private val transferGraphQLResolver: TransferGraphQLResolver,
    private val accountGraphQLResolver: AccountGraphQLResolver
) {

    @Bean
    open fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder
                // Custom scalars for Spring Boot 4.0 GraphQL
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(SqlDateScalar.INSTANCE)
                .scalar(TimestampScalar.INSTANCE)
                // Mutations are still handled via DataFetchers
                .type("Mutation") { typeBuilder ->
                    typeBuilder
                        // Payment mutations
                        .dataFetcher("createPayment", paymentGraphQLResolver.createPayment())
                        .dataFetcher("deletePayment", paymentGraphQLResolver.deletePayment())
                        // Transfer mutations
                        .dataFetcher("createTransfer", transferGraphQLResolver.createTransfer())
                        .dataFetcher("deleteTransfer", transferGraphQLResolver.deleteTransfer())
                }
        }
    }

}
