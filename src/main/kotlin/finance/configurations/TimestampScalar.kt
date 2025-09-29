package finance.configurations

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import java.sql.Timestamp

object TimestampScalar {
    val INSTANCE: GraphQLScalarType =
        GraphQLScalarType.newScalar()
            .name("Timestamp")
            .description("A custom scalar that handles java.sql.Timestamp as Long milliseconds")
            .coercing(
                object : Coercing<Timestamp, Long> {
                    override fun serialize(dataFetcherResult: Any): Long {
                        return when (dataFetcherResult) {
                            is Timestamp -> dataFetcherResult.time
                            is Long -> dataFetcherResult
                            is Number -> dataFetcherResult.toLong()
                            else -> throw CoercingSerializeException("Unable to serialize $dataFetcherResult as Timestamp")
                        }
                    }

                    override fun parseValue(input: Any): Timestamp {
                        return when (input) {
                            is Long -> Timestamp(input)
                            is Number -> Timestamp(input.toLong())
                            is String -> Timestamp(input.toLong())
                            else -> throw CoercingParseValueException("Unable to parse $input as Timestamp")
                        }
                    }

                    override fun parseLiteral(input: Any): Timestamp {
                        return when (input) {
                            is StringValue -> Timestamp(input.value.toLong())
                            else -> throw CoercingParseLiteralException("Unable to parse literal $input as Timestamp")
                        }
                    }
                },
            )
            .build()
}
