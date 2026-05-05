package finance.controllers

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

    def "handleConstraintViolation returns BAD_REQUEST GraphQLError"() {
        given:
        def field = graphql.language.Field.newField("test").build()
        def stepInfo = GroovyMock(graphql.execution.ExecutionStepInfo)
        stepInfo.getPath() >> graphql.execution.ResultPath.rootPath()
        def env = Mock(graphql.schema.DataFetchingEnvironment)
        env.getField() >> field
        env.getExecutionStepInfo() >> stepInfo
        def violation = Mock(jakarta.validation.ConstraintViolation)
        violation.propertyPath >> Mock(jakarta.validation.Path)
        violation.message >> "must not be blank"
        def ex = new jakarta.validation.ConstraintViolationException("Validation failed", [violation] as Set)

        when:
        def error = handler.handleConstraintViolation(ex, env)

        then:
        error != null
        error.errorType == org.springframework.graphql.execution.ErrorType.BAD_REQUEST
        error.message.contains("Validation failed")
    }

    def "handleIllegalArgument returns BAD_REQUEST GraphQLError with message"() {
        given:
        def field = graphql.language.Field.newField("test").build()
        def stepInfo = GroovyMock(graphql.execution.ExecutionStepInfo)
        stepInfo.getPath() >> graphql.execution.ResultPath.rootPath()
        def env = Mock(graphql.schema.DataFetchingEnvironment)
        env.getField() >> field
        env.getExecutionStepInfo() >> stepInfo
        def ex = new IllegalArgumentException("bad value provided")

        when:
        def error = handler.handleIllegalArgument(ex, env)

        then:
        error != null
        error.errorType == org.springframework.graphql.execution.ErrorType.BAD_REQUEST
        error.message == "bad value provided"
    }

    def "handleIllegalArgument returns BAD_REQUEST GraphQLError when message is null"() {
        given:
        def field = graphql.language.Field.newField("test").build()
        def stepInfo = GroovyMock(graphql.execution.ExecutionStepInfo)
        stepInfo.getPath() >> graphql.execution.ResultPath.rootPath()
        def env = Mock(graphql.schema.DataFetchingEnvironment)
        env.getField() >> field
        env.getExecutionStepInfo() >> stepInfo
        def ex = new IllegalArgumentException((String) null)

        when:
        def error = handler.handleIllegalArgument(ex, env)

        then:
        error != null
        error.errorType == org.springframework.graphql.execution.ErrorType.BAD_REQUEST
        error.message == "Bad request"
    }

    def "handleGeneric returns INTERNAL_ERROR GraphQLError"() {
        given:
        def field = graphql.language.Field.newField("test").build()
        def stepInfo = GroovyMock(graphql.execution.ExecutionStepInfo)
        stepInfo.getPath() >> graphql.execution.ResultPath.rootPath()
        def env = Mock(graphql.schema.DataFetchingEnvironment)
        env.getField() >> field
        env.getExecutionStepInfo() >> stepInfo
        def ex = new Exception("something went wrong")

        when:
        def error = handler.handleGeneric(ex, env)

        then:
        error != null
        error.errorType == org.springframework.graphql.execution.ErrorType.INTERNAL_ERROR
        error.message == "Internal server error"
    }
}
