package finance.configurations

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.sql.Date
import java.text.SimpleDateFormat

object SqlDateScalar {
    val INSTANCE: GraphQLScalarType =
        GraphQLScalarType.newScalar()
            .name("Date")
            .description("A custom scalar that handles java.sql.Date as String in YYYY-MM-DD format")
            .coercing(
                object : Coercing<Date, String> {
                    private val dateFormat =
                        SimpleDateFormat("yyyy-MM-dd").apply {
                            isLenient = false
                        }

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun serialize(dataFetcherResult: Any): String {
                        return when (dataFetcherResult) {
                            is Date -> dateFormat.format(dataFetcherResult)
                            is java.util.Date -> dateFormat.format(dataFetcherResult)
                            is String -> dataFetcherResult
                            else -> throw CoercingSerializeException("Unable to serialize $dataFetcherResult as Date")
                        }
                    }

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun parseValue(input: Any): Date {
                        return when (input) {
                            is String -> Date(dateFormat.parse(input).time)
                            is Long -> Date(input)
                            is Number -> Date(input.toLong())
                            else -> throw CoercingParseValueException("Unable to parse $input as Date")
                        }
                    }

                    @Deprecated("Deprecated in GraphQL Extended Scalars")
                    override fun parseLiteral(input: Any): Date {
                        return when (input) {
                            is StringValue -> Date(dateFormat.parse(input.value).time)
                            else -> throw CoercingParseLiteralException("Unable to parse literal $input as Date")
                        }
                    }
                },
            )
            .build()
}
