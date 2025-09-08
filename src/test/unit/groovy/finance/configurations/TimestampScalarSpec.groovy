package finance.configurations

import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import spock.lang.Specification

import java.sql.Timestamp

class TimestampScalarSpec extends Specification {
    def scalar = TimestampScalar.INSTANCE.INSTANCE

    def "serialize supports Timestamp Long and Number"() {
        given:
        def ts = new Timestamp(12345L)

        expect:
        scalar.coercing.serialize(ts) == 12345L
        scalar.coercing.serialize(6789L) == 6789L
        scalar.coercing.serialize(Integer.valueOf(10)) == 10L
    }

    def "serialize rejects unsupported types"() {
        when:
        scalar.coercing.serialize("not a timestamp")

        then:
        thrown(CoercingSerializeException)
    }

    def "parseValue supports Long Number and numeric String"() {
        expect:
        scalar.coercing.parseValue(123L) instanceof Timestamp
        scalar.coercing.parseValue(Integer.valueOf(456)).time == 456L
        scalar.coercing.parseValue("789").time == 789L
    }

    def "parseValue rejects unsupported input"() {
        when:
        scalar.coercing.parseValue(new Object())

        then:
        thrown(CoercingParseValueException)
    }

    def "parseLiteral supports numeric StringValue"() {
        expect:
        scalar.coercing.parseLiteral(new StringValue("42")).time == 42L
    }

    def "parseLiteral rejects unsupported literals"() {
        when:
        scalar.coercing.parseLiteral(123)

        then:
        thrown(CoercingParseLiteralException)
    }
}
