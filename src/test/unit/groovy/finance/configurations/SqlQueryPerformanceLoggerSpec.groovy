package finance.configurations

import org.slf4j.MDC
import spock.lang.Specification

class SqlQueryPerformanceLoggerSpec extends Specification {

    SqlQueryPerformanceLogger logger

    def setup() {
        logger = new SqlQueryPerformanceLogger()
        MDC.clear()
    }

    def cleanup() {
        MDC.clear()
    }

    def "logQueryCompletion with fast query does not throw"() {
        when:
        logger.logQueryCompletion("fast-query-1", 10L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion with slow query at 100ms threshold does not throw"() {
        when:
        logger.logQueryCompletion("slow-query-1", 100L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion with slow query above 100ms threshold does not throw"() {
        when:
        logger.logQueryCompletion("slow-query-2", 250L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion with very slow query at 500ms threshold does not throw"() {
        when:
        logger.logQueryCompletion("very-slow-1", 500L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion with very slow query above 500ms threshold does not throw"() {
        when:
        logger.logQueryCompletion("very-slow-2", 1500L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion uses correlationId from MDC"() {
        given:
        MDC.put("correlationId", "trace-123")

        when:
        logger.logQueryCompletion("mdcquery-1", 50L)

        then:
        noExceptionThrown()
    }

    def "logQueryCompletion uses N/A when no correlationId in MDC"() {
        given:
        MDC.remove("correlationId")

        when:
        logger.logQueryCompletion("nomdcquery-1", 50L)

        then:
        noExceptionThrown()
    }

    def "getQueryStats returns non-null map"() {
        when:
        def stats = logger.getQueryStats()

        then:
        stats != null
    }

    def "getQueryStats accumulates execution times by query prefix"() {
        given:
        String uniquePrefix = "unique${System.nanoTime()}"
        logger.logQueryCompletion("${uniquePrefix}-1", 100L)
        logger.logQueryCompletion("${uniquePrefix}-2", 200L)
        logger.logQueryCompletion("${uniquePrefix}-3", 300L)

        when:
        def stats = logger.getQueryStats()

        then:
        stats.containsKey(uniquePrefix)
        stats[uniquePrefix].count == 3
        stats[uniquePrefix].totalMs == 600L
        stats[uniquePrefix].minMs == 100L
        stats[uniquePrefix].maxMs == 300L
        stats[uniquePrefix].avgMs == 200.0
    }

    def "QueryStats data class holds correct field values"() {
        when:
        def stats = new SqlQueryPerformanceLogger.QueryStats(5, 120.5d, 50L, 250L, 602L)

        then:
        stats.count == 5
        stats.avgMs == 120.5d
        stats.minMs == 50L
        stats.maxMs == 250L
        stats.totalMs == 602L
    }
}
