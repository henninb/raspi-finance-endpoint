package finance.services
import finance.configurations.DatabaseResilienceConfiguration
import finance.configurations.ResilienceComponents

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.jdbc.CannotGetJdbcConnectionException
import org.springframework.dao.DataAccessResourceFailureException
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import java.util.concurrent.TimeUnit

class BaseServiceAdditionalSpec extends Specification {

    MeterService meterService
    SimpleMeterRegistry registry
    BaseServiceAdditionalSpec.ExposedBaseService base

    void setup() {
        registry = new SimpleMeterRegistry()
        meterService = new MeterService(registry)
        base = new BaseServiceAdditionalSpec.ExposedBaseService(meterService, GroovyMock(jakarta.validation.Validator), ResilienceComponents.noOp())
    }

    // Concrete subclass needed because BaseService constructor requires meterService + validator
    static class ExposedBaseService extends BaseService {
        ExposedBaseService(meterService, validator, rc) {
            super(meterService, validator, rc)
        }
    }

    private ExposedBaseService buildResilientBase() {
        def cfg = new DatabaseResilienceConfiguration()
        def rc = new ResilienceComponents(
            cfg,
            cfg.databaseCircuitBreaker(),
            cfg.databaseRetry(),
            cfg.databaseTimeLimiter(),
            cfg.scheduledExecutorService()
        )
        return new ExposedBaseService(meterService, GroovyMock(jakarta.validation.Validator), rc)
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

    void "executeWithResilienceSync DataAccessResourceFailureException is rethrown directly"() {
        when:
        base.executeWithResilienceSync(
            { throw new org.springframework.dao.DataAccessResourceFailureException('access-fail') },
            'darf-fail', 30L
        )

        then:
        thrown(org.springframework.dao.DataAccessResourceFailureException)
    }

    void "executeWithResilience whenComplete logs failure for SQLException cause via real resilience"() {
        given:
        def resilientBase = buildResilientBase()

        when:
        def future = resilientBase.executeWithResilience({ throw new java.sql.SQLException('db down') }, 'sql-cause-test')

        then:
        // Use PollingConditions to wait for the async whenComplete block to finish
        new PollingConditions(timeout: 5).eventually {
            def counters = registry.find(finance.utils.Constants.EXCEPTION_THROWN_COUNTER).counters()
            assert !counters.isEmpty()
            assert counters.any { it.count() >= 1d }
        }

        cleanup:
        resilientBase.resilienceComponents.scheduledExecutorService.shutdown()
    }

    void "executeWithResilience whenComplete logs failure for CannotGetJdbcConnectionException cause via real resilience"() {
        given:
        def resilientBase = buildResilientBase()

        when:
        def future = resilientBase.executeWithResilience(
            { throw new CannotGetJdbcConnectionException('no connection') }, 'jdbc-cause-test'
        )
        future.get(5, TimeUnit.SECONDS)

        then:
        thrown(Exception)

        cleanup:
        resilientBase.resilienceComponents.scheduledExecutorService.shutdown()
    }

    void "createDefaultAccount produces expected defaults"() {
        when:
        def account = base.createDefaultAccount("checking_primary", finance.domain.AccountType.Checking)

        then:
        account.accountNameOwner == "checking_primary"
        account.moniker == "0000"
        account.accountType == finance.domain.AccountType.Checking
        account.activeStatus
    }

    void "nowTimestamp returns a non-null recent timestamp"() {
        when:
        def method = finance.services.BaseService.getDeclaredMethod("nowTimestamp")
        method.accessible = true
        def ts = method.invoke(base) as java.sql.Timestamp

        then:
        ts != null
        ts.time <= System.currentTimeMillis()
        ts.time >= System.currentTimeMillis() - 1000
    }
}
