package finance.configurations

import finance.resolvers.GraphQLDataFetchers
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import com.google.common.io.Resources
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.AsyncSerialExecutionStrategy
import graphql.scalars.ExtendedScalars
//import graphql.scalars.datetime.DateScalar
import graphql.schema.GraphQLScalarType
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource

@Component
class GraphqlProvider(private val graphQLDataFetcher: GraphQLDataFetchers)  {
//    @Value("\${classpath:schema.graphql}")
//    private lateinit var schema: Resource

    @Bean
    fun graphql(): GraphQL {
        //1. Parse schema
        //val schemaParser = SchemaParser()
        val url = Resources.getResource("graphql/schema.graphqls")
        val sdl = Resources.toString(url, Charsets.UTF_8)
        val typeDefinitionRegistry = SchemaParser().parse(sdl)
        //val typeDefinitionRegistry = schemaParser.parse(schema.inputStream)
        //2. Add Data fetcher
        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .type("Query") {
                it.dataFetcher("descriptions", graphQLDataFetcher.descriptions)
            }
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.Date)
            .build()
        //3. Build GraphQL instance
        val schema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        return GraphQL.newGraphQL(schema)
            .queryExecutionStrategy(AsyncExecutionStrategy())
            .mutationExecutionStrategy(AsyncSerialExecutionStrategy())
            .build()
    }

}
