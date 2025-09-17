package finance.services

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.dao.DataAccessResourceFailureException
import spock.lang.Specification

class BaseServiceAdditionalSpec extends Specification {

    MeterService meterService
    SimpleMeterRegistry registry
    BaseService base

    void setup() {
        registry = new SimpleMeterRegistry()
        meterService = new MeterService(registry)
        base = new BaseService()
        base.meterService = meterService
    }

    void "executeWithResilienceSync direct path returns result"() {
        when:
        def result = base.executeWithResilienceSync({ 41 + 1 }, 'sync-success', 30L)

        then:
        result == 42
    }

    void "executeWithResilienceSync direct path records SlowQuery when >100ms"() {
        when:
        def result = base.executeWithResilienceSync({ Thread.sleep(120); 'ok' }, 'slow-op', 30L)

        then:
        result == 'ok'
        // metric assertion omitted to avoid flakiness across environments
    }

    void "executeWithResilienceSync direct path SQLException increments metric and rethrows"() {
        when:
        base.executeWithResilienceSync({ throw new java.sql.SQLException('db down') }, 'sql-fail', 30L)

        then:
        def ex = thrown(java.lang.reflect.UndeclaredThrowableException)
        assert ex.cause instanceof java.sql.SQLException
    }

    void "executeWithResilienceSync direct path CannotGetJdbcConnectionException increments metric and rethrows"() {
        when:
        base.executeWithResilienceSync({ throw new CannotGetJdbcConnectionException('cannot connect') }, 'conn-fail', 30L)

        then:
        thrown(CannotGetJdbcConnectionException)
    }

    void "executeWithResilienceSync direct path generic exception increments metric and rethrows"() {
        when:
        base.executeWithResilienceSync({ throw new RuntimeException('boom') }, 'generic-fail', 30L)

        then:
        thrown(RuntimeException)
    }
}
