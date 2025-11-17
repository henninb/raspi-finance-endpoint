package finance.configurations

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object SqlDateScalar {
    val INSTANCE: GraphQLScalarType =
        GraphQLScalarType
            .newScalar()
            .name("Date")
            .description("A custom scalar that handles java.time.LocalDate as String in YYYY-MM-DD format")
            .coercing(
                object : Coercing<LocalDate, String> {
                    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun serialize(dataFetcherResult: Any): String =
                        when (dataFetcherResult) {
                            is LocalDate -> dataFetcherResult.format(dateFormatter)
                            is String -> dataFetcherResult
                            else -> throw CoercingSerializeException("Unable to serialize $dataFetcherResult as LocalDate")
                        }

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun parseValue(input: Any): LocalDate =
                        when (input) {
                            is String -> LocalDate.parse(input, dateFormatter)
                            else -> throw CoercingParseValueException("Unable to parse $input as LocalDate")
                        }

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun parseLiteral(input: Any): LocalDate =
                        when (input) {
                            is StringValue -> LocalDate.parse(input.value, dateFormatter)
                            else -> throw CoercingParseLiteralException("Unable to parse literal $input as LocalDate")
                        }
                },
            ).build()
}
