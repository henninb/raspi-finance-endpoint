package finance.helpers

import finance.domain.Category
import groovy.util.logging.Slf4j
import java.util.concurrent.atomic.AtomicInteger

@Slf4j
class SmartCategoryBuilder {

    private static final AtomicInteger COUNTER = new AtomicInteger(0)

    private String testOwner
    private String categoryName
    private Boolean activeStatus = true

    private SmartCategoryBuilder(String testOwner) {
        this.testOwner = testOwner
        // Generate a unique, constraint-compliant category name
        this.categoryName = generateUniqueCategoryName()
    }

    static SmartCategoryBuilder builderForOwner(String testOwner) {
        return new SmartCategoryBuilder(testOwner)
    }

    private String generateUniqueCategoryName() {
        String counter = COUNTER.incrementAndGet().toString()
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        // Create base name following ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$"
        String baseName = "cat${counter}_${ownerPart}"

        // Ensure length constraints (1-50 chars)
        if (baseName.length() > 50) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "cat${counter}_${shortOwner}"
        }

        if (baseName.length() < 1) {
            baseName = "c${counter}"
        }

        String cleaned = baseName.toLowerCase()

        log.debug("Generated category name: ${cleaned} for test owner: ${testOwner}")
        return cleaned
    }

    Category build() {
        Category category = new Category().with {
            categoryName = this.categoryName
            activeStatus = this.activeStatus
            return it
        }
        return category
    }

    Category buildAndValidate() {
        Category category = build()
        validateConstraints(category)
        return category
    }

    private void validateConstraints(Category category) {
        // Validate category name constraints
        if (category.categoryName.length() < 1 || category.categoryName.length() > 50) {
            throw new IllegalStateException("Category name '${category.categoryName}' violates length constraints (1-50 chars)")
        }

        // ALPHA_NUMERIC_NO_SPACE_PATTERN = "^[a-z0-9_-]*$"
        if (!category.categoryName.matches(/^[a-z0-9_-]*$/)) {
            throw new IllegalStateException("Category name '${category.categoryName}' violates alpha_numeric_no_space pattern (must be letters/numbers/underscores/dashes)")
        }

        log.debug("Category passed constraint validation: ${category.categoryName}")
    }

    // Fluent API methods
    SmartCategoryBuilder withCategoryName(String categoryName) {
        this.categoryName = categoryName
        return this
    }

    SmartCategoryBuilder withUniqueCategoryName(String prefix = "test") {
        this.categoryName = generateUniqueCategoryName(prefix)
        return this
    }

    private String generateUniqueCategoryName(String prefix) {
        String cleanPrefix = prefix.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '') // Keep only alphanumeric

        if (cleanPrefix.isEmpty()) {
            cleanPrefix = "cat"
        }

        if (ownerPart.isEmpty()) {
            ownerPart = "test"
        }

        String baseName = "${cleanPrefix}_${ownerPart}"

        // Ensure length constraints (1-50 chars)
        if (baseName.length() > 50) {
            String shortOwner = ownerPart.length() > 8 ? ownerPart[0..7] : ownerPart
            baseName = "${cleanPrefix}_${shortOwner}"
        }

        if (baseName.length() < 1) {
            baseName = "cat"
        }

        return baseName.toLowerCase()
    }

    SmartCategoryBuilder withActiveStatus(Boolean activeStatus) {
        this.activeStatus = activeStatus
        return this
    }

    // Convenience methods for common scenarios
    SmartCategoryBuilder asActive() {
        this.activeStatus = true
        return this
    }

    SmartCategoryBuilder asInactive() {
        this.activeStatus = false
        return this
    }

    // Common category patterns for testing
    SmartCategoryBuilder asOnlineCategory() {
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
        if (ownerPart.isEmpty()) ownerPart = "test"
        this.categoryName = "online_${ownerPart}".toLowerCase()
        return this
    }

    SmartCategoryBuilder asGroceriesCategory() {
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
        if (ownerPart.isEmpty()) ownerPart = "test"
        this.categoryName = "groceries_${ownerPart}".toLowerCase()
        return this
    }

    SmartCategoryBuilder asUtilitiesCategory() {
        String ownerPart = testOwner.replaceAll(/[^a-z0-9]/, '')
        if (ownerPart.isEmpty()) ownerPart = "test"
        this.categoryName = "utilities_${ownerPart}".toLowerCase()
        return this
    }
}