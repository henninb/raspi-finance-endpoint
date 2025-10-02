package finance.configurations

import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
open class GraphQLWiringConfig {
    @Bean
    open fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder
                // Custom scalars for Spring Boot 4.0 GraphQL
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(SqlDateScalar.INSTANCE)
                .scalar(TimestampScalar.INSTANCE)
        }
}
