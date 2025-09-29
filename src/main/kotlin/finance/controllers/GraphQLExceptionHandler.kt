package finance.controllers

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Controller

@Controller
class GraphQLExceptionHandler {
    companion object {
        val logger: Logger = LogManager.getLogger()
    }

    @GraphQlExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        env: DataFetchingEnvironment,
    ): GraphQLError {
        logger.warn("GraphQL validation failed: {}", ex.message)
        val msg =
            ex.constraintViolations.joinToString(
                separator = "; ",
            ) { v -> "${v.propertyPath}: ${v.message}" }
        return GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message("Validation failed: $msg")
            .build()
    }

    @GraphQlExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        env: DataFetchingEnvironment,
    ): GraphQLError {
        logger.warn("GraphQL illegal argument: {}", ex.message)
        return GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.BAD_REQUEST)
            .message(ex.message ?: "Bad request")
            .build()
    }

    @GraphQlExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        env: DataFetchingEnvironment,
    ): GraphQLError {
        logger.error("GraphQL error", ex)
        return GraphqlErrorBuilder.newError(env)
            .errorType(ErrorType.INTERNAL_ERROR)
            .message("Internal server error")
            .build()
    }
}
