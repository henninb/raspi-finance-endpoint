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
        String baseName = "desc_${COUNTER.incrementAndGet()}_${testOwner}"

        // Ensure it meets constraints:
        // - Size: min 1, max 50
        // - Converted to lowercase

        if (baseName.length() > 50) {
            // Truncate while keeping testOwner identifier
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            baseName = "desc_${COUNTER.get()}_${shortOwner}"
        }

        // Ensure minimum length
        if (baseName.length() < 1) {
            baseName = "d_${COUNTER.get()}"
        }

        // Clean and convert to lowercase
        String cleaned = baseName.toLowerCase()

        // Ensure we still have a valid name after processing
        if (cleaned.length() < 1) {
            cleaned = "description${COUNTER.get()}"
        }

        log.debug("Generated description name: ${cleaned} for test owner: ${testOwner}")
        return cleaned
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
        this.descriptionName = descriptionName.toLowerCase()
        return this
    }

    SmartDescriptionBuilder withUniqueDescriptionName(String prefix = "test") {
        this.descriptionName = generateUniqueDescriptionName(prefix)
        return this
    }

    private String generateUniqueDescriptionName(String prefix) {
        String baseName = "${prefix}_${COUNTER.incrementAndGet()}_${testOwner}"

        if (baseName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            baseName = "${prefix}_${COUNTER.get()}_${shortOwner}"
        }

        return baseName.toLowerCase()
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
        this.descriptionName = "store_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (this.descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            this.descriptionName = "store_${COUNTER.get()}_${shortOwner}".toLowerCase()
        }

        return this
    }

    SmartDescriptionBuilder asRestaurantDescription() {
        this.descriptionName = "restaurant_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (this.descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            this.descriptionName = "rest_${COUNTER.get()}_${shortOwner}".toLowerCase()
        }

        return this
    }

    SmartDescriptionBuilder asServiceDescription() {
        this.descriptionName = "service_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (this.descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            this.descriptionName = "svc_${COUNTER.get()}_${shortOwner}".toLowerCase()
        }

        return this
    }

    SmartDescriptionBuilder asOnlineDescription() {
        this.descriptionName = "online_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (this.descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            this.descriptionName = "online_${COUNTER.get()}_${shortOwner}".toLowerCase()
        }

        return this
    }

    SmartDescriptionBuilder withBusinessDescription(String businessType) {
        this.descriptionName = "${businessType}_${COUNTER.incrementAndGet()}_${testOwner}".toLowerCase()

        // Ensure length constraints
        if (this.descriptionName.length() > 50) {
            String shortOwner = testOwner.length() > 8 ? testOwner[0..7] : testOwner
            this.descriptionName = "${businessType}_${COUNTER.get()}_${shortOwner}".toLowerCase()
        }

        return this
    }
}