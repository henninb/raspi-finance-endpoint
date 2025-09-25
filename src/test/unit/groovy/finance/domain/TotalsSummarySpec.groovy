package finance.domain

import spock.lang.Specification

class TotalsSummarySpec extends Specification {

    def "test TotalsSummary creation with all parameters"() {
        given:
        Double totalAmount = 150.75
        Long transactionCount = 5L
        TransactionState transactionState = TransactionState.Cleared

        when:
        TotalsSummary summary = new TotalsSummary(totalAmount, transactionCount, transactionState)

        then:
        summary.totalAmount == totalAmount
        summary.transactionCount == transactionCount
        summary.transactionState == transactionState
    }

    def "test TotalsSummary with zero values"() {
        when:
        TotalsSummary summary = new TotalsSummary(0.0, 0L, TransactionState.Outstanding)

        then:
        summary.totalAmount == 0.0
        summary.transactionCount == 0L
        summary.transactionState == TransactionState.Outstanding
    }

    def "test TotalsSummary with negative total amount"() {
        when:
        TotalsSummary summary = new TotalsSummary(-250.50, 3L, TransactionState.Cleared)

        then:
        summary.totalAmount == -250.50
        summary.transactionCount == 3L
        summary.transactionState == TransactionState.Cleared
    }

    def "test TotalsSummary with all transaction states"() {
        expect:
        new TotalsSummary(100.0, 1L, state).transactionState == state

        where:
        state << [TransactionState.Cleared, TransactionState.Outstanding, TransactionState.Future]
    }

    def "test TotalsSummary equals and hashCode with same values"() {
        given:
        TotalsSummary summary1 = new TotalsSummary(100.0, 5L, TransactionState.Cleared)
        TotalsSummary summary2 = new TotalsSummary(100.0, 5L, TransactionState.Cleared)

        expect:
        summary1 == summary2
        summary1.hashCode() == summary2.hashCode()
    }

    def "test TotalsSummary equals returns false with different values"() {
        given:
        TotalsSummary summary1 = new TotalsSummary(100.0, 5L, TransactionState.Cleared)
        TotalsSummary summary2 = new TotalsSummary(150.0, 5L, TransactionState.Cleared)

        expect:
        summary1 != summary2
    }

    def "test TotalsSummary toString method"() {
        given:
        TotalsSummary summary = new TotalsSummary(123.45, 10L, TransactionState.Outstanding)

        when:
        String result = (String)(summary)

        then:
        result != null
        result.contains("123.45")
        result.contains("10")
    }

    def "test TotalsSummary with large transaction count"() {
        given:
        Long largeCount = Long.MAX_VALUE

        when:
        TotalsSummary summary = new TotalsSummary(999.99, largeCount, TransactionState.Future)

        then:
        summary.transactionCount == largeCount
        summary.totalAmount == 999.99
        summary.transactionState == TransactionState.Future
    }

    def "test TotalsSummary with very small decimal amount"() {
        given:
        Double smallAmount = 0.01

        when:
        TotalsSummary summary = new TotalsSummary(smallAmount, 1L, TransactionState.Cleared)

        then:
        summary.totalAmount == smallAmount
    }

    def "test TotalsSummary with very large decimal amount"() {
        given:
        Double largeAmount = Double.MAX_VALUE

        when:
        TotalsSummary summary = new TotalsSummary(largeAmount, 1L, TransactionState.Cleared)

        then:
        summary.totalAmount == largeAmount
    }
}
