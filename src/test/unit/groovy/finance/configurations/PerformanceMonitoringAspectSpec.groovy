package finance.configurations

import finance.services.MeterService
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.slf4j.MDC
import spock.lang.Specification

class PerformanceMonitoringAspectSpec extends Specification {

    MeterRegistry meterRegistry
    MeterService meterService
    PerformanceMonitoringAspect aspect
    ProceedingJoinPoint joinPoint
    Signature signature

    def setup() {
        meterRegistry = new SimpleMeterRegistry()
        meterService = Mock(MeterService)
        aspect = new PerformanceMonitoringAspect(meterRegistry, meterService)
        joinPoint = Mock(ProceedingJoinPoint)
        signature = Mock(Signature)
        joinPoint.signature >> signature
        MDC.clear()
    }

    def cleanup() {
        MDC.clear()
    }

    def "monitorServiceMethodPerformance returns result for successful execution"() {
        given:
        signature.declaringTypeName >> "finance.services.AccountService"
        signature.name >> "findAll"
        joinPoint.proceed() >> ["account1", "account2"]

        when:
        Object result = aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        result == ["account1", "account2"]
    }

    def "monitorServiceMethodPerformance records timer metric on success"() {
        given:
        signature.declaringTypeName >> "finance.services.AccountService"
        signature.name >> "findAll"
        joinPoint.proceed() >> "result"

        when:
        aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        meterRegistry.find("method.execution.time")
            .tag("layer", "service")
            .tag("status", "success")
            .timer() != null
    }

    def "monitorRepositoryMethodPerformance returns result for successful execution"() {
        given:
        signature.declaringTypeName >> "finance.repositories.AccountRepository"
        signature.name >> "findById"
        joinPoint.proceed() >> Optional.empty()

        when:
        Object result = aspect.monitorRepositoryMethodPerformance(joinPoint)

        then:
        result == Optional.empty()
    }

    def "monitorRepositoryMethodPerformance records timer metric on success"() {
        given:
        signature.declaringTypeName >> "finance.repositories.AccountRepository"
        signature.name >> "save"
        joinPoint.proceed() >> "saved"

        when:
        aspect.monitorRepositoryMethodPerformance(joinPoint)

        then:
        meterRegistry.find("method.execution.time")
            .tag("layer", "repository")
            .tag("status", "success")
            .timer() != null
    }

    def "monitorServiceMethodPerformance rethrows exception and records failure metric"() {
        given:
        signature.declaringTypeName >> "finance.services.TransactionService"
        signature.name >> "insert"
        joinPoint.proceed() >> { throw new RuntimeException("service error") }

        when:
        aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        thrown(RuntimeException)
        meterRegistry.find("method.execution.time")
            .tag("layer", "service")
            .tag("status", "failure")
            .timer() != null
    }

    def "monitorRepositoryMethodPerformance rethrows exception and records failure metric"() {
        given:
        signature.declaringTypeName >> "finance.repositories.TransactionRepository"
        signature.name >> "findAll"
        joinPoint.proceed() >> { throw new RuntimeException("db error") }

        when:
        aspect.monitorRepositoryMethodPerformance(joinPoint)

        then:
        thrown(RuntimeException)
        meterRegistry.find("method.execution.time")
            .tag("layer", "repository")
            .tag("status", "failure")
            .timer() != null
    }

    def "monitorServiceMethodPerformance uses class simple name from declaring type"() {
        given:
        signature.declaringTypeName >> "finance.services.AccountService"
        signature.name >> "findByOwner"
        joinPoint.proceed() >> []

        when:
        aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        meterRegistry.find("method.execution.time")
            .tag("class", "AccountService")
            .timer() != null
    }

    def "monitorServiceMethodPerformance uses correlationId from MDC in logging"() {
        given:
        MDC.put("correlationId", "trace-xyz-789")
        signature.declaringTypeName >> "finance.services.AccountService"
        signature.name >> "findAll"
        joinPoint.proceed() >> "result"

        when:
        aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        noExceptionThrown()
    }

    def "monitorServiceMethodPerformance handles null result"() {
        given:
        signature.declaringTypeName >> "finance.services.AccountService"
        signature.name >> "deleteById"
        joinPoint.proceed() >> null

        when:
        Object result = aspect.monitorServiceMethodPerformance(joinPoint)

        then:
        result == null
        noExceptionThrown()
    }
}
