package finance.configurations

import graphql.schema.idl.SchemaDirectiveWiring
import graphql.validation.rules.ValidationRules
import graphql.validation.schemawiring.ValidationSchemaWiring
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class GraphQLValidationConfig {

    @Bean
    open fun directiveWiring(): SchemaDirectiveWiring {
        val validationRules = ValidationRules.newValidationRules()
            //.onValidationErrorStrategy(ThrowExceptionStrategy())
            //.addRule(CheckWildcardConstraint())
            .build()
        return ValidationSchemaWiring(validationRules)
    }
}