package finance.configurations

import graphql.analysis.MaxQueryComplexityInstrumentation
import graphql.analysis.MaxQueryDepthInstrumentation
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * GraphQL guardrails: sets max query depth and complexity via instrumentation.
 * Uses GraphQlSourceBuilderCustomizer so it is applied to the GraphQL builder.
 */
@Configuration
open class GraphQLGuardrailsConfig {
    /**
     * Factory method exposed for unit testing and reuse.
     */
    open fun createGuardrailInstrumentation(
        maxDepth: Int,
        maxComplexity: Int,
    ): Instrumentation {
        val instrumentations =
            listOf(
                MaxQueryDepthInstrumentation(maxDepth),
                MaxQueryComplexityInstrumentation(maxComplexity),
            )
        return ChainedInstrumentation(instrumentations)
    }

    /**
     * Registers instrumentation as a bean. Spring GraphQL auto-config collects Instrumentation
     * beans and applies a chained instrumentation to the GraphQL builder.
     */
    @Bean
    open fun graphQlGuardrailsInstrumentation(): Instrumentation = createGuardrailInstrumentation(maxDepth = 12, maxComplexity = 300)
}
