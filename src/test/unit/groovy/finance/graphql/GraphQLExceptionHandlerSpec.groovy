package finance.graphql

import finance.controllers.GraphQLExceptionHandler
import spock.lang.Specification

class GraphQLExceptionHandlerSpec extends Specification {

    def handler = new GraphQLExceptionHandler()

    def "handler can be instantiated"() {
        expect:
        handler != null
    }

    def "handler has logger companion object"() {
        expect:
        GraphQLExceptionHandler.logger != null
    }
}