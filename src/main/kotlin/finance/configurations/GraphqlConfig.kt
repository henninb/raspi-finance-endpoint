package finance.configurations

import finance.resolvers.*
import graphql.kickstart.tools.SchemaParser
import graphql.language.StringValue
import graphql.scalar.GraphqlIntCoercing
import graphql.schema.*
import org.apache.groovy.datetime.extensions.DateTimeExtensions.toLocalDateTime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Configuration
open class GraphqlConfig {

//    @Bean
//    open fun timestampType(): GraphQLScalarType {
//        return Timestamp
//    }

//    open fun <T> temporalScalar(
//        type: Class<T>?,
//        name: String?,
//        description: String?,
//        fromString: ThrowingFunction<String?, T>?,
//        fromDate: ThrowingFunction<Instant?, T>?
//    ): GraphQLScalarType? {
//        return temporalScalar(type, name, description, fromString, fromDate, { obj: Any -> obj.toString() })
//    }


//    val TIMESTAMP = GraphQLScalarType.newScalar()
//        .name("Timestamp")
//        .description("A scalar type representing a point in time.")
//        .coercing(object : Coercing<LocalDateTime?, String?> {
//            @Throws(CoercingSerializeException::class)
//            override fun serialize(dataFetcherResult: Any): String? {
//                try {
//                    val d: LocalDateTime = toLocalDateTime(dataFetcherResult)
//                    if (d != null) {
//                        return d.format(DATE_TIME_FORMAT)
//                    }
//                } catch (rte: RuntimeException) {
//                    throw CoercingSerializeException("Unable to serialize $dataFetcherResult as a timestamp", rte)
//                }
//                throw CoercingSerializeException("Unable to serialize $dataFetcherResult as a timestamp.")
//            }
//
//            @Throws(CoercingParseValueException::class)
//            override fun parseValue(input: Any): LocalDateTime? {
//                try {
//                    val d: LocalDateTime = toLocalDateTime(input)
//                    if (d != null) {
//                        return d
//                    }
//                } catch (rte: RuntimeException) {
//                    throw CoercingParseValueException("Unable to parse value $input as a timestamp.", rte)
//                }
//                throw CoercingParseValueException("Unable to parse value $input as a timestamp.")
//            }
//
//            @Throws(CoercingParseLiteralException::class)
//            override fun parseLiteral(input: Any): LocalDateTime? {
//                try {
//                    val d: LocalDateTime = toLocalDateTime(input)
//                    if (d != null) {
//                        return d
//                    }
//                } catch (rte: RuntimeException) {
//                    throw CoercingParseLiteralException("Unable to parse literal $input as a timestamp.", rte)
//                }
//                throw CoercingParseLiteralException("Unable to parse literal $input as a timestamp.")
//            }
//        })
//        .build()

//val GraphQLLong: GraphQLScalarType = GraphQLScalarType.newScalar()
//    .name("Long").description("Long Scalar").coercing(GraphqlIntCoercing()).build()
//
//    val graphqlLocalDateType = GraphQLScalarType("LocalDate",
//        " ISO date format without an offset, such as '2011-12-03' ",
//        object : Coercing<LocalDate, String> {
//            override fun parseLiteral(input: Any?): LocalDate {
//                return when (input) {
//                    is StringValue -> LocalDate.parse(input.value)
//                    else -> throw CoercingParseLiteralException("unable to parse literal: $input")
//                }
//            }
//
//            override fun serialize(dataFetcherResult: Any): String {
//                return when (dataFetcherResult) {
//                    is LocalDate -> DateTimeFormatter.ISO_LOCAL_DATE.format(dataFetcherResult)
//                    else -> throw CoercingSerializeException("unable to serialize ${dataFetcherResult.javaClass.name}: $dataFetcherResult")
//                }
//            }
//
//            override fun parseValue(input: Any?): LocalDate? {
//                throw RuntimeException("not implemented")
//            }
//
//        }
//    )

//    val GraphQLSqlDate: GraphQLScalarType = temporalScalar(
//        Date::class.java, "SqlDate", "a SQL compliant local date",
//        { s -> Date.valueOf(LocalDate.parse(s)) }, { i -> Date.valueOf(i.atZone(ZoneOffset.UTC).toLocalDate()) }) { d ->
//        d.toLocalDate().toString()
//    }

    //scalarsRegistry.put(java.sql.Date.class, newScalarType("SqlDate", "SQL Date type", GraphQLSqlDateCoercing()));
    //scalarsRegistry.put(java.sql.Timestamp.class, newScalarType("SqlTimestamp", "SQL Timestamp type", GraphQLSqlTimestampCoercing()));


//    @Bean
//    open fun dateScalar(): GraphQLScalarType {
//        return GraphQLScalarType.newScalar()
//            .name("Date")
//            .description("sql date as scalar.")
//            .coercing(object : Coercing<Date, String> {
//                override fun serialize(dataFetcherResult: Any): String {
//                    return (dataFetcherResult as? Date)?.toString()
//                        ?: throw CoercingSerializeException("Expected a sql date object.")
//                }
//
//                override fun parseValue(input: Any): Date {
//                    return try {
//                        if (input is String) {
//                            //LocalDate.parse(input)
//                            Date(0L)
//                        } else {
//                            throw CoercingParseValueException("Expected a String")
//                        }
//                    } catch (e: DateTimeParseException) {
//                        throw CoercingParseValueException(
//                            String.format("Not a valid date: '%s'.", input), e
//                        )
//                    }
//                }
//
//                override fun parseLiteral(input: Any): Date {
//                    return if (input is StringValue) {
//                        try {
//                            LocalDate.parse((input as StringValue).getValue())
//                        } catch (e: DateTimeParseException) {
//                            throw CoercingParseLiteralException(e)
//                        }
//                    } else {
//                        throw CoercingParseLiteralException("Expected a StringValue.")
//                    }
//                }
//            }).build()
//    }

    @Bean
    open fun graphqlSchema(
        descriptionQueryResolver: DescriptionQueryResolver,
        categoryQueryResolver: CategoryQueryResolver,
        accountQueryResolver: AccountQueryResolver,
        parameterQueryResolver: ParameterQueryResolver,
        paymentQueryResolver: PaymentQueryResolver,
        transactionQueryResolver: TransactionQueryResolver,
        validationAmountQueryResolver: ValidationAmountQueryResolver

    ): GraphQLSchema {
        return SchemaParser.newParser()
            .file("graphql/schema.graphqls")
            .resolvers(accountQueryResolver, descriptionQueryResolver, categoryQueryResolver, parameterQueryResolver, paymentQueryResolver,validationAmountQueryResolver,transactionQueryResolver)
            .scalars()
            .build()
            .makeExecutableSchema()
    }
}