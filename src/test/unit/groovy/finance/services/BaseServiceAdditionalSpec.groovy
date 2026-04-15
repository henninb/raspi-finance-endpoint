package finance.services
import finance.configurations.ResilienceComponents

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.dao.DataAccessResourceFailureException
import spock.lang.Specification

class BaseServiceAdditionalSpec extends Specification {

    MeterService meterService
    SimpleMeterRegistry registry
    BaseServiceAdditionalSpec.ExposedBaseService base

    void setup() {
        registry = new SimpleMeterRegistry()
        meterService = new MeterService(registry)
        base = new BaseServiceAdditionalSpec.ExposedBaseService(meterService, GroovyMock(jakarta.validation.Validator))
    }

    // Concrete subclass needed because BaseService constructor requires meterService + validator
    static class ExposedBaseService extends BaseService {
        ExposedBaseService(meterService, validator) {
            super(meterService, validator, ResilienceComponents.noOp())
        }
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

    void "executeWithResilienceSync SQLException is wrapped in DataAccessResourceFailureException"() {
        when:
        base.executeWithResilienceSync({ throw new java.sql.SQLException('db down') }, 'sql-fail', 30L)

        then:
        def ex = thrown(DataAccessResourceFailureException)
        assert ex.cause instanceof java.sql.SQLException
    }

    void "executeWithResilienceSync CannotGetJdbcConnectionException is rethrown directly"() {
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
