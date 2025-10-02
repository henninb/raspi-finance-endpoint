package finance.configurations

import graphql.ExecutionResult
import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import reactor.core.publisher.Mono
import spock.lang.Specification

class GraphQLInterceptorSpec extends Specification {

    GraphQLInterceptor interceptor

    def setup() {
        interceptor = new GraphQLInterceptor()
    }

    def "intercept should pass request through chain and log response"() {
        given:
        def chain = Mock(WebGraphQlInterceptor.Chain)
        def request = Mock(WebGraphQlRequest)
        def response = Mock(WebGraphQlResponse)
        def executionResult = Mock(ExecutionResult)
        def responseData = ["accounts": [["id": "123", "name": "checking"]]]

        response.executionResult >> executionResult
        executionResult.errors >> []
        executionResult.getData() >> responseData
        chain.next(request) >> Mono.just(response)

        when:
        def result = interceptor.intercept(request, chain)

        then:
        result.block() == response
    }

    def "intercept should log GraphQL errors when present"() {
        given:
        def chain = Mock(WebGraphQlInterceptor.Chain)
        def request = Mock(WebGraphQlRequest)
        def response = Mock(WebGraphQlResponse)
        def executionResult = Mock(ExecutionResult)
        def mockError = Mock(GraphQLError)

        mockError.message >> "Test error message"
        mockError.locations >> [Mock(SourceLocation)]
        mockError.path >> ["accounts", 0, "id"]
        mockError.errorType >> ErrorType.ValidationError
        mockError.extensions >> ["code": "VALIDATION_FAILED"]

        response.executionResult >> executionResult
        executionResult.errors >> [mockError]
        executionResult.getData() >> ["accounts": []]
        chain.next(request) >> Mono.just(response)

        when:
        def result = interceptor.intercept(request, chain)

        then:
        result.block() == response
    }

    def "intercept should handle empty list responses with warning"() {
        given:
        def chain = Mock(WebGraphQlInterceptor.Chain)
        def request = Mock(WebGraphQlRequest)
        def response = Mock(WebGraphQlResponse)
        def executionResult = Mock(ExecutionResult)

        response.executionResult >> executionResult
        executionResult.errors >> []
        executionResult.getData() >> ["accounts": []]
        chain.next(request) >> Mono.just(response)

        when:
        def result = interceptor.intercept(request, chain)

        then:
        result.block() == response
    }

    def "intercept should handle null response data"() {
        given:
        def chain = Mock(WebGraphQlInterceptor.Chain)
        def request = Mock(WebGraphQlRequest)
        def response = Mock(WebGraphQlResponse)
        def executionResult = Mock(ExecutionResult)

        response.executionResult >> executionResult
        executionResult.errors >> []
        executionResult.getData() >> null
        chain.next(request) >> Mono.just(response)

        when:
        def result = interceptor.intercept(request, chain)

        then:
        result.block() == response
    }

    def "intercept should handle chain errors"() {
        given:
        def chain = Mock(WebGraphQlInterceptor.Chain)
        def request = Mock(WebGraphQlRequest)
        def error = new RuntimeException("Chain error")

        chain.next(request) >> Mono.error(error)

        when:
        def result = interceptor.intercept(request, chain)
        result.block()

        then:
        thrown(RuntimeException)
    }

    def "should be Spring Component"() {
        expect:
        interceptor.class.isAnnotationPresent(org.springframework.stereotype.Component)
    }

    def "should implement WebGraphQlInterceptor"() {
        expect:
        interceptor instanceof WebGraphQlInterceptor
    }
}