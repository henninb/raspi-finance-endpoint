package finance.graphql

import finance.controllers.GraphQLExceptionHandler
import spock.lang.Specification

class GraphQLExceptionHandlerSpec extends Specification {

    GraphQLExceptionHandler handler

    def setup() {
        handler = new GraphQLExceptionHandler()
    }

    def "handler can be instantiated"() {
        expect:
        handler != null
    }

    def "handler has logger companion object"() {
        expect:
        GraphQLExceptionHandler.logger != null
    }

    def "handler has handleConstraintViolation method"() {
        expect: "handler has the method for constraint violation handling"
        handler.class.getDeclaredMethod("handleConstraintViolation",
            jakarta.validation.ConstraintViolationException.class,
            graphql.schema.DataFetchingEnvironment.class) != null
    }

    def "handler has handleIllegalArgument method"() {
        expect: "handler has the method for illegal argument handling"
        handler.class.getDeclaredMethod("handleIllegalArgument",
            IllegalArgumentException.class,
            graphql.schema.DataFetchingEnvironment.class) != null
    }

    def "handler has handleGeneric method"() {
        expect: "handler has the method for generic exception handling"
        handler.class.getDeclaredMethod("handleGeneric",
            Exception.class,
            graphql.schema.DataFetchingEnvironment.class) != null
    }

    def "handler methods should have GraphQlExceptionHandler annotation"() {
        when: "checking method annotations"
        def constraintMethod = handler.class.getDeclaredMethod("handleConstraintViolation",
            jakarta.validation.ConstraintViolationException.class,
            graphql.schema.DataFetchingEnvironment.class)
        def illegalArgMethod = handler.class.getDeclaredMethod("handleIllegalArgument",
            IllegalArgumentException.class,
            graphql.schema.DataFetchingEnvironment.class)
        def genericMethod = handler.class.getDeclaredMethod("handleGeneric",
            Exception.class,
            graphql.schema.DataFetchingEnvironment.class)

        then: "all methods have @GraphQlExceptionHandler annotation"
        constraintMethod.isAnnotationPresent(org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler.class)
        illegalArgMethod.isAnnotationPresent(org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler.class)
        genericMethod.isAnnotationPresent(org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler.class)
    }

    def "handler class should have Controller annotation"() {
        expect: "handler class is annotated with @Controller"
        handler.class.isAnnotationPresent(org.springframework.stereotype.Controller.class)
    }

    def "handler methods return GraphQLError type"() {
        when: "checking return types"
        def constraintMethod = handler.class.getDeclaredMethod("handleConstraintViolation",
            jakarta.validation.ConstraintViolationException.class,
            graphql.schema.DataFetchingEnvironment.class)
        def illegalArgMethod = handler.class.getDeclaredMethod("handleIllegalArgument",
            IllegalArgumentException.class,
            graphql.schema.DataFetchingEnvironment.class)
        def genericMethod = handler.class.getDeclaredMethod("handleGeneric",
            Exception.class,
            graphql.schema.DataFetchingEnvironment.class)

        then: "all methods return GraphQLError"
        constraintMethod.returnType == graphql.GraphQLError.class
        illegalArgMethod.returnType == graphql.GraphQLError.class
        genericMethod.returnType == graphql.GraphQLError.class
    }
}
