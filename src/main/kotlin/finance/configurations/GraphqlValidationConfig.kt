package finance.configurations

import finance.controllers.resolver.CategoryQueryResolver
import finance.controllers.resolver.DescriptionQueryResolver
import graphql.Scalars
import graphql.kickstart.tools.SchemaParser
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLSchema
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.sql.Timestamp


@Configuration
open class GraphqlValidationConfig {

//    @Bean
//    open fun timestampType(): GraphQLScalarType {
//        return Timestamp
//    }

    @Bean
    open fun graphqlSchema(
        descriptionQueryResolver: DescriptionQueryResolver,
        categoryQueryResolver: CategoryQueryResolver

    ): GraphQLSchema {
        return SchemaParser.newParser()
            .file("graphql/description.graphqls")
            .resolvers(descriptionQueryResolver, categoryQueryResolver)
            .build()
            .makeExecutableSchema()
    }
}