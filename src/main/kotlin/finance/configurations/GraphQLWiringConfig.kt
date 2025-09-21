package finance.configurations

import finance.configurations.SqlDateScalar
import finance.configurations.TimestampScalar
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import org.apache.logging.log4j.LogManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

/**
 * Modern Spring Boot 4.0 GraphQL Configuration
 *
 * Only handles custom scalars - all queries/mutations are automatically discovered
 * via @QueryMapping/@MutationMapping annotations in controller classes.
 */
@Configuration
open class GraphQLWiringConfig {

    @Bean
    open fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder
                // Custom scalars for financial data types
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(SqlDateScalar.INSTANCE)
                .scalar(TimestampScalar.INSTANCE)
                // Note: Query and Mutation mappings are automatically discovered
                // from @QueryMapping/@MutationMapping annotations in @Controller classes
        }
    }

    companion object {
        val logger = LogManager.getLogger()
    }
}
