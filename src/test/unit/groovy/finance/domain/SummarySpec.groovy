package finance.domain

import spock.lang.Specification

import java.math.BigDecimal

class SummarySpec extends Specification {

    def "test Summary data class creation with all parameters"() {
        given:
        BigDecimal totals = BigDecimal.valueOf(300.00)
        BigDecimal totalsCleared = BigDecimal.valueOf(200.00)
        BigDecimal totalsOutstanding = BigDecimal.valueOf(50.00)
        BigDecimal totalsFuture = BigDecimal.valueOf(100.00)

        when:
        Summary result = new Summary(totals, totalsCleared, totalsOutstanding, totalsFuture)

        then:
        result.totals == totals
        result.totalsCleared == totalsCleared
        result.totalsOutstanding == totalsOutstanding
        result.totalsFuture == totalsFuture
    }

    def "test Summary default constructor creates zero values"() {
        when:
        Summary summary = new Summary()

        then:
        summary.totals == BigDecimal.valueOf(0.00)
        summary.totalsCleared == BigDecimal.valueOf(0.00)
        summary.totalsOutstanding == BigDecimal.valueOf(0.00)
        summary.totalsFuture == BigDecimal.valueOf(0.00)
    }

    def "test Summary toString method returns valid JSON"() {
        given:
        Summary summary = new Summary(
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(50.00),
            BigDecimal.valueOf(100.00)
        )

        when:
        String jsonString = (String)(summary)

        then:
        jsonString != null
        jsonString.contains("totals")
        jsonString.contains("totalsCleared")
        jsonString.contains("totalsOutstanding")
        jsonString.contains("totalsFuture")
        jsonString.startsWith("{")
        jsonString.endsWith("}")
    }

    def "test Summary equals and hashCode with same values"() {
        given:
        Summary summary1 = new Summary(
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(50.00),
            BigDecimal.valueOf(100.00)
        )
        Summary summary2 = new Summary(
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(50.00),
            BigDecimal.valueOf(100.00)
        )

        expect:
        summary1 == summary2
        summary1.hashCode() == summary2.hashCode()
    }

    def "test Summary equals returns false with different values"() {
        given:
        Summary summary1 = new Summary(
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(50.00),
            BigDecimal.valueOf(100.00)
        )
        Summary summary2 = new Summary(
            BigDecimal.valueOf(350.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(50.00),
            BigDecimal.valueOf(100.00)
        )

        expect:
        summary1 != summary2
    }

    def "test Summary with zero values"() {
        when:
        Summary summary = new Summary(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )

        then:
        summary.totals == BigDecimal.ZERO
        summary.totalsCleared == BigDecimal.ZERO
        summary.totalsOutstanding == BigDecimal.ZERO
        summary.totalsFuture == BigDecimal.ZERO
    }

    def "test Summary with negative values"() {
        when:
        Summary summary = new Summary(
            BigDecimal.valueOf(-300.00),
            BigDecimal.valueOf(-200.00),
            BigDecimal.valueOf(-50.00),
            BigDecimal.valueOf(-100.00)
        )

        then:
        summary.totals == BigDecimal.valueOf(-300.00)
        summary.totalsCleared == BigDecimal.valueOf(-200.00)
        summary.totalsOutstanding == BigDecimal.valueOf(-50.00)
        summary.totalsFuture == BigDecimal.valueOf(-100.00)
    }

    def "test Summary property mutation"() {
        given:
        Summary summary = new Summary()

        when:
        summary.totals = BigDecimal.valueOf(500.00)
        summary.totalsCleared = BigDecimal.valueOf(400.00)
        summary.totalsOutstanding = BigDecimal.valueOf(75.00)
        summary.totalsFuture = BigDecimal.valueOf(125.00)

        then:
        summary.totals == BigDecimal.valueOf(500.00)
        summary.totalsCleared == BigDecimal.valueOf(400.00)
        summary.totalsOutstanding == BigDecimal.valueOf(75.00)
        summary.totalsFuture == BigDecimal.valueOf(125.00)
    }
}
