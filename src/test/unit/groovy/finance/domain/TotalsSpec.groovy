package finance.domain

import spock.lang.Specification
import java.math.BigDecimal

class TotalsSpec extends Specification {

    def "Totals - default constructor"() {
        when:
        def totals = new Totals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

        then:
        totals.totals == BigDecimal.ZERO
        totals.totalsCleared == BigDecimal.ZERO
        totals.totalsOutstanding == BigDecimal.ZERO
        totals.totalsFuture == BigDecimal.ZERO
    }

    def "Totals - equals and hashCode"() {
        given:
        def t1 = new Totals(1.0G, 2.0G, 3.0G, 4.0G)
        def t2 = new Totals(1.0G, 2.0G, 3.0G, 4.0G)
        def t3 = new Totals(1.0G, 2.0G, 3.0G, 5.0G)

        expect:
        t1 == t2
        t1.hashCode() == t2.hashCode()
        t1 != t3
        t1 != null
    }

    def "Totals - toString returns valid JSON"() {
        given:
        def t = new Totals(1.1G, 2.2G, 3.3G, 4.4G)

        when:
        String result = t.toString()

        then:
        result.contains('"totalsFuture":1.1')
        result.contains('"totalsCleared":2.2')
        result.contains('"totals":3.3')
        result.contains('"totalsOutstanding":4.4')
    }
}
