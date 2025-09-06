package finance.helpers

import finance.domain.Description
import groovy.util.logging.Slf4j
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartDescriptionBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String descriptionName
    private Boolean activeStatus = true

    private SmartDescriptionBuilder(String testOwner) {
        this.testOwner = testOwner
        // Generate a unique, constraint-compliant description name
        this.descriptionName = generateUniqueDescriptionName()
    }

    static SmartDescriptionBuilder builderForOwner(String testOwner) {
        return new SmartDescriptionBuilder(testOwner)
    }

    private String generateUniqueDescriptionName() {
        String baseName = "desc_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        String result = ensureValidLength(baseName)
        log.debug("Generated description name: ${result} for test owner: ${testOwner}")
        return result
    }

    Description build() {
        Description description = new Description().with {
            descriptionName = this.descriptionName
            activeStatus = this.activeStatus
            return it
        }
        return description
    }

    Description buildAndValidate() {
        Description description = build()
        validateConstraints(description)
        return description
    }

    private void validateConstraints(Description description) {
        // Validate description name constraints
        if (description.descriptionName.length() < 1 || description.descriptionName.length() > 50) {
            throw new IllegalStateException("Description name '${description.descriptionName}' violates length constraints (1-50 chars)")
        }

        log.debug("Description passed constraint validation: ${description.descriptionName}")
    }

    // Fluent API methods
    SmartDescriptionBuilder withDescriptionName(String descriptionName) {
        this.descriptionName = ensureValidLength(descriptionName.toLowerCase())
        return this
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
            return "desc${COUNTER.get()}"
        }

        return input
    }

    SmartDescriptionBuilder withUniqueDescriptionName(String prefix = "test") {
        this.descriptionName = generateUniqueDescriptionName(prefix)
        return this
    }

    private String generateUniqueDescriptionName(String prefix) {
        String baseName = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        return ensureValidLength(baseName)
    }

    SmartDescriptionBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience methods for common scenarios
    SmartDescriptionBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartDescriptionBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    // Common description patterns for testing
    SmartDescriptionBuilder asStoreDescription() {
        String baseName = "store_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.descriptionName = ensureValidLength(baseName)
        return this
    }

    SmartDescriptionBuilder asRestaurantDescription() {
        String baseName = "restaurant_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.descriptionName = ensureValidLength(baseName)
        return this
    }

    SmartDescriptionBuilder asServiceDescription() {
        String baseName = "service_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.descriptionName = ensureValidLength(baseName)
        return this
    }

    SmartDescriptionBuilder asOnlineDescription() {
        String baseName = "online_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.descriptionName = ensureValidLength(baseName)
        return this
    }

    SmartDescriptionBuilder withBusinessDescription(String businessType) {
        String baseName = "${businessType}_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()
        this.descriptionName = ensureValidLength(baseName)
        return this
    }
}