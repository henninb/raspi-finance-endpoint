//package finance.configurations
//
////import graphql.scalars.datetime.DateScalar
//
//import com.google.common.io.Resources
//import finance.resolvers.GraphQLDataFetchers
//import graphql.GraphQL
//import graphql.GraphQLContext
//import graphql.execution.AsyncExecutionStrategy
//import graphql.execution.AsyncSerialExecutionStrategy
//import graphql.scalars.ExtendedScalars
//import graphql.schema.idl.RuntimeWiring
//import graphql.schema.idl.SchemaGenerator
//import graphql.schema.idl.SchemaParser
//import org.dataloader.DataLoaderRegistry
//import org.springframework.context.annotation.Bean
//import org.springframework.stereotype.Component
//import org.springframework.web.servlet.config.annotation.CorsRegistry
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
//import javax.servlet.http.HttpServletRequest
//import javax.servlet.http.HttpServletResponse
//import javax.websocket.Session
//import javax.websocket.server.HandshakeRequest
//
//@Component
//class GraphqlProvider(private val graphQLDataFetcher: GraphQLDataFetchers) {
//
//    @Bean
//    fun corsConfigure(): WebMvcConfigurer {
//
//        return object : WebMvcConfigurer {
//            override fun addCorsMappings(registry: CorsRegistry) {
//                registry.addMapping("/**").allowedOrigins("*")
//            }
//        }
//    }
//
////    @Bean
////    fun contextBuilder(dataLoaderRegistry: DataLoaderRegistry): GraphQLServletContextBuilder {
////        return object : GraphQLServletContextBuilder() {
////            fun build(request: HttpServletRequest?, response: HttpServletResponse?): GraphQLContext? {
////                return DefaultBCOGraphQLContext(dataLoaderRegistry, null, request)
////            }
////
////            fun build(): GraphQLContext? {
////                return DefaultGraphQLContext(dataLoaderRegistry, null)
////            }
////
////            fun build(session: Session?, request: HandshakeRequest?): GraphQLContext? {
////                return BCOGraphQLWebsocketContext(dataLoaderRegistry, null, session, request)
////            }
////        }
////    }
//
//
//    @Bean
//    fun graphql(): GraphQL {
//        //1. Parse schema
//
//        //val url = File(GraphqlProvider::class.java.getResource("/graphql").toURI())
//
//        //val schemaParser = SchemaParser()
//        val url = Resources.getResource("graphql/schema.graphqls")
//        val sdl = Resources.toString(url, Charsets.UTF_8)
//        val typeDefinitionRegistry = SchemaParser().parse(sdl)
//        //val typeDefinitionRegistry = schemaParser.parse(schema.inputStream)
//        //2. Add Data fetcher
//        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
//            .type("Query") {
//                it.dataFetcher("descriptions", graphQLDataFetcher.descriptions)
//                it.dataFetcher("description", graphQLDataFetcher.description)
//                it.dataFetcher("accounts", graphQLDataFetcher.accounts)
//                it.dataFetcher("categories", graphQLDataFetcher.categories)
//                it.dataFetcher("payments", graphQLDataFetcher.payments)
//                it.dataFetcher("payment", graphQLDataFetcher.payment())
//                it.dataFetcher("account", graphQLDataFetcher.account())
//            }
//            .type("Mutation") {
//                it.dataFetcher("createDescription", graphQLDataFetcher.createDescription())
//                it.dataFetcher("createCategory", graphQLDataFetcher.createCategory())
//                it.dataFetcher("createPayment", graphQLDataFetcher.createPayment())
//            }
//            .scalar(ExtendedScalars.GraphQLLong)
//            .scalar(ExtendedScalars.GraphQLBigDecimal)
//            .scalar(ExtendedScalars.Date)
//            .build()
//        //3. Build GraphQL instance
//        val schema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
//        return GraphQL.newGraphQL(schema)
//            .queryExecutionStrategy(AsyncExecutionStrategy())
//            .mutationExecutionStrategy(AsyncSerialExecutionStrategy())
//            .build()
//    }
//
//}
