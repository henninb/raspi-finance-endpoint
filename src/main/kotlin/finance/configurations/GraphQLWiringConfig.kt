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
    private val accountGraphQLResolver: AccountGraphQLResolver,
) {

    @Bean
    open fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(SqlDateScalar.INSTANCE)
                .scalar(TimestampScalar.INSTANCE)
                .type("Query") { typeBuilder ->
                    typeBuilder
                        .dataFetcher("accounts", accountGraphQLResolver.accounts)
                        .dataFetcher("account", accountGraphQLResolver.account())
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
