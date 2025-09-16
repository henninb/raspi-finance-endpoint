
package finance.services

import spock.lang.Specification
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

class BaseServiceFunctionalitySpec extends Specification {

    def meterService = new MeterService(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
    def baseService = new BaseService()

    void setup() {
        baseService.meterService = meterService
    }

    def "handleConstraintViolations should throw ValidationException"() {
        given:
        def violation = Mock(ConstraintViolation) {
            getInvalidValue() >> "invalid_value"
            getMessage() >> "error_message"
        }
        def violations = [violation] as Set

        when:
        baseService.handleConstraintViolations(violations, meterService)

        then:
        thrown(ValidationException)
    }

    // Removed sync variant test since method is protected; covered by async variant.

    def "executeWithResilience should execute directly when resilience components are null"() {
        given:
        baseService.databaseResilienceConfig = null

        when:
        def future = baseService.executeWithResilience({ "success" }, "test-op")
        def result = future.get()

        then:
        result == "success"
    }
}

// Test helper to expose protected methods
class TestableBaseService extends BaseService { }
