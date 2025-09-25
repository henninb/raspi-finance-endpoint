package finance.domain

import spock.lang.Specification

import java.math.BigDecimal

class TotalsSpec extends Specification {

    def "test Totals data class creation with all parameters"() {
        given:
        BigDecimal totalsFuture = BigDecimal.valueOf(100.00)
        BigDecimal totalsCleared = BigDecimal.valueOf(200.00)
        BigDecimal totals = BigDecimal.valueOf(300.00)
        BigDecimal totalsOutstanding = BigDecimal.valueOf(50.00)

        when:
        Totals result = new Totals(totalsFuture, totalsCleared, totals, totalsOutstanding)

        then:
        result.totalsFuture == totalsFuture
        result.totalsCleared == totalsCleared
        result.totals == totals
        result.totalsOutstanding == totalsOutstanding
    }

    def "test Totals toString method returns valid JSON"() {
        given:
        Totals totals = new Totals(
            BigDecimal.valueOf(100.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(50.00)
        )

        when:
        String jsonString = (String)(totals)

        then:
        jsonString != null
        jsonString.contains("totalsFuture")
        jsonString.contains("totalsCleared")
        jsonString.contains("totals")
        jsonString.contains("totalsOutstanding")
        jsonString.startsWith("{")
        jsonString.endsWith("}")
    }

    def "test Totals equals and hashCode with same values"() {
        given:
        Totals totals1 = new Totals(
            BigDecimal.valueOf(100.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(50.00)
        )
        Totals totals2 = new Totals(
            BigDecimal.valueOf(100.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(50.00)
        )

        expect:
        totals1 == totals2
        totals1.hashCode() == totals2.hashCode()
    }

    def "test Totals equals returns false with different values"() {
        given:
        Totals totals1 = new Totals(
            BigDecimal.valueOf(100.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(50.00)
        )
        Totals totals2 = new Totals(
            BigDecimal.valueOf(150.00),
            BigDecimal.valueOf(200.00),
            BigDecimal.valueOf(300.00),
            BigDecimal.valueOf(50.00)
        )

        expect:
        totals1 != totals2
    }

    def "test Totals with zero values"() {
        when:
        Totals totals = new Totals(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )

        then:
        totals.totalsFuture == BigDecimal.ZERO
        totals.totalsCleared == BigDecimal.ZERO
        totals.totals == BigDecimal.ZERO
        totals.totalsOutstanding == BigDecimal.ZERO
    }

    def "test Totals with negative values"() {
        when:
        Totals totals = new Totals(
            BigDecimal.valueOf(-100.00),
            BigDecimal.valueOf(-200.00),
            BigDecimal.valueOf(-300.00),
            BigDecimal.valueOf(-50.00)
        )

        then:
        totals.totalsFuture == BigDecimal.valueOf(-100.00)
        totals.totalsCleared == BigDecimal.valueOf(-200.00)
        totals.totals == BigDecimal.valueOf(-300.00)
        totals.totalsOutstanding == BigDecimal.valueOf(-50.00)
    }
}
