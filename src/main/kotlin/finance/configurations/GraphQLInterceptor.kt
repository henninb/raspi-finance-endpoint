package finance.configurations

import org.apache.logging.log4j.LogManager
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class GraphQLInterceptor : WebGraphQlInterceptor {
    companion object {
        private val logger = LogManager.getLogger(GraphQLInterceptor::class.java)
    }

    override fun intercept(
        request: WebGraphQlRequest,
        chain: WebGraphQlInterceptor.Chain,
    ): Mono<WebGraphQlResponse> {
        return chain.next(request)
            .doOnNext { response ->
                try {
                    val executionResult = response.executionResult

                    // Log GraphQL response structure
                    logger.debug("GraphQL Response Debug:")
                    logger.debug("  - Errors: ${executionResult.errors?.size ?: 0}")
                    if (executionResult.errors?.isNotEmpty() == true) {
                        executionResult.errors.forEach { error ->
                            logger.error("GraphQL Error: ${error.message}")
                            logger.error("  - Locations: ${error.locations}")
                            logger.error("  - Path: ${error.path}")
                            logger.error("  - Error Type: ${error.errorType}")
                            val extensions = error.extensions
                            if (extensions != null && extensions.isNotEmpty()) {
                                logger.error("  - Extensions: $extensions")
                            }
                        }
                    }

                    // Log response data structure
                    val data = executionResult.getData<Map<String, Any?>>()
                    if (data != null) {
                        logger.debug("GraphQL Response Data Structure:")
                        data.forEach { (key, value) ->
                            when (value) {
                                is List<*> -> {
                                    logger.debug("  - $key: List with ${value.size} items")
                                    if (value.isEmpty()) {
                                        logger.warn("GraphQL: Query '$key' returned empty list - this may indicate serialization issues")
                                    }
                                }
                                is Map<*, *> -> {
                                    logger.debug("  - $key: Map with ${value.size} entries")
                                }
                                null -> {
                                    logger.warn("GraphQL: Query '$key' returned null - this may indicate an error")
                                }
                                else -> {
                                    logger.debug("  - $key: ${value::class.simpleName} = $value")
                                }
                            }
                        }

                        // Detailed logging for empty responses
                        if (data.values.all { it == null || (it is Collection<*> && it.isEmpty()) }) {
                            logger.warn("GraphQL: All response fields are null or empty - possible serialization failure")
                            logger.warn("GraphQL: Request query was: ${request.document}")
                        }
                    } else {
                        logger.error("GraphQL: Response data is null - this indicates a serious problem")
                    }
                } catch (e: Exception) {
                    logger.error("Error in GraphQL response interceptor", e)
                }
            }
            .doOnError { error ->
                logger.error("GraphQL request failed completely", error)
            }
    }
}
