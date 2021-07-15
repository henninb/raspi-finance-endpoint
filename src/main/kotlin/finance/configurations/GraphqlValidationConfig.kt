package finance.configurations

import finance.resolvers.*
import graphql.kickstart.tools.SchemaParser
import graphql.schema.GraphQLSchema
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class GraphqlValidationConfig {

//    @Bean
//    open fun timestampType(): GraphQLScalarType {
//        return Timestamp
//    }

    @Bean
    open fun graphqlSchema(
        descriptionQueryResolver: DescriptionQueryResolver,
        categoryQueryResolver: CategoryQueryResolver,
        accountQueryResolver: AccountQueryResolver,
        parameterQueryResolver: ParameterQueryResolver,
        paymentQueryResolver: PaymentQueryResolver,
        transactionQueryResolver: TransactionQueryResolver,
        validationAmountQueryResolver: ValidationAmountQueryResolver

    ): GraphQLSchema {
        return SchemaParser.newParser()
            .file("graphql/schema.graphqls")
            .resolvers(accountQueryResolver, descriptionQueryResolver, categoryQueryResolver, parameterQueryResolver, paymentQueryResolver,validationAmountQueryResolver,transactionQueryResolver)
            .build()
            .makeExecutableSchema()
    }
}