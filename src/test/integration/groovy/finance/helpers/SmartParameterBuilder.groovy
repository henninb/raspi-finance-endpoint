package finance.helpers

import finance.domain.Parameter
import groovy.util.logging.Slf4j
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartParameterBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String parameterName
    private String parameterValue
    private Boolean activeStatus = true

    private SmartParameterBuilder(String testOwner) {
        this.testOwner = testOwner
        // Generate unique, constraint-compliant parameter name and value
        generateUniqueParameterFields()
    }

    static SmartParameterBuilder builderForOwner(String testOwner) {
        return new SmartParameterBuilder(testOwner)
    }

    private void generateUniqueParameterFields() {
        int counter = COUNTER.incrementAndGet()

        // Generate parameterName (1-50 chars, lowercase)
        String baseName = "param_${counter}_${testOwner}"
        this.parameterName = ensureValidLength(baseName.toLowerCase())

        // Generate parameterValue (1-50 chars, lowercase)
        String baseValue = "value_${counter}_${testOwner}"
        this.parameterValue = ensureValidLength(baseValue.toLowerCase())

        log.debug("Generated parameter: name=${parameterName}, value=${parameterValue} for test owner: ${testOwner}")
    }

    private String ensureValidLength(String input) {
        // Ensure constraints: min 1, max 50 chars
        if (input.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            String shortened = input.replace(testOwner, shortOwner)
            if (shortened.length() > 50) {
                shortened = shortened[0..49]
            }
            return shortened
        }

        if (input.length() < 1) {
            return "param${COUNTER.get()}"
        }

        return input
    }

    Parameter build() {
        Parameter parameter = new Parameter().with {
            owner = this.testOwner
            parameterName = this.parameterName
            parameterValue = this.parameterValue
            activeStatus = this.activeStatus
            return it
        }
        return parameter
    }

    Parameter buildAndValidate() {
        Parameter parameter = build()
        validateConstraints(parameter)
        return parameter
    }

    private void validateConstraints(Parameter parameter) {
        // Validate parameterName constraints
        if (parameter.parameterName.length() < 1 || parameter.parameterName.length() > 50) {
            throw new IllegalStateException("Parameter name '${parameter.parameterName}' violates length constraints (1-50 chars)")
        }

        // Validate parameterValue constraints
        if (parameter.parameterValue.length() < 1 || parameter.parameterValue.length() > 50) {
            throw new IllegalStateException("Parameter value '${parameter.parameterValue}' violates length constraints (1-50 chars)")
        }

        log.debug("Parameter passed constraint validation: ${parameter.parameterName}=${parameter.parameterValue}")
    }

    // Fluent API methods
    SmartParameterBuilder withParameterName(String parameterName) {
        this.parameterName = ensureValidLength(parameterName.toLowerCase())
        return this
    }

    SmartParameterBuilder withParameterValue(String parameterValue) {
        this.parameterValue = ensureValidLength(parameterValue.toLowerCase())
        return this
    }

    SmartParameterBuilder withUniqueParameterName(String prefix = "test") {
        String baseName = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}"
        this.parameterName = ensureValidLength(baseName.toLowerCase())
        return this
    }

    SmartParameterBuilder withUniqueParameterValue(String prefix = "value") {
        String baseValue = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}"
        this.parameterValue = ensureValidLength(baseValue.toLowerCase())
        return this
    }

    SmartParameterBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience methods for common scenarios
    SmartParameterBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartParameterBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    // Common parameter patterns for testing
    SmartParameterBuilder asPaymentAccountParameter() {
        this.parameterName = "payment_account_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.parameterValue = "bank_${testOwner}".toLowerCase()

        // Ensure length constraints
        this.parameterName = ensureValidLength(this.parameterName)
        this.parameterValue = ensureValidLength(this.parameterValue)

        return this
    }

    SmartParameterBuilder asConfigParameter(String configType) {
        this.parameterName = "${configType}_config_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.parameterValue = "${configType}_value_${testOwner}".toLowerCase()

        // Ensure length constraints
        this.parameterName = ensureValidLength(this.parameterName)
        this.parameterValue = ensureValidLength(this.parameterValue)

        return this
    }

    SmartParameterBuilder withKeyValuePair(String key, String value) {
        this.parameterName = "${key}_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.parameterValue = "${value}_${testOwner}".toLowerCase()

        // Ensure length constraints
        this.parameterName = ensureValidLength(this.parameterName)
        this.parameterValue = ensureValidLength(this.parameterValue)

        return this
    }
}