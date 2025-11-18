package finance.configurations

import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import spock.lang.Specification

import java.time.LocalDate

class SqlDateScalarSpec extends Specification {
    def scalar = SqlDateScalar.INSTANCE.INSTANCE

    def "serialize supports LocalDate and String"() {
        given:
        def d = LocalDate.parse("2020-01-02")

        expect:
        scalar.coercing.serialize(d) == "2020-01-02"
        scalar.coercing.serialize("2021-12-31") == "2021-12-31"
    }

    def "serialize rejects unsupported types"() {
        when:
        scalar.coercing.serialize(123)

        then:
        thrown(CoercingSerializeException)
    }

    def "parseValue supports String date"() {
        expect:
        scalar.coercing.parseValue("2020-01-02") == LocalDate.parse("2020-01-02")
    }

    def "parseValue rejects unsupported input"() {
        when:
        scalar.coercing.parseValue(new Object())

        then:
        thrown(CoercingParseValueException)
    }

    def "parseLiteral supports StringValue in yyyy-MM-dd format"() {
        expect:
        scalar.coercing.parseLiteral(new StringValue("2020-01-02")) == LocalDate.parse("2020-01-02")
    }

    def "parseLiteral rejects unsupported literal"() {
        when:
        scalar.coercing.parseLiteral(123)

        then:
        thrown(CoercingParseLiteralException)
    }
}
