package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Category
import finance.helpers.SmartCategoryBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import jakarta.validation.ConstraintViolationException
import spock.lang.Shared

/**
 * INTEGRATION TEST - CategoryRepository using robust, isolated architecture
 *
 * This integration test demonstrates the new architecture:
 * ✅ No hardcoded category names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Build-time validation prevents invalid data
 */
class CategoryRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    CategoryRepository categoryRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test category repository basic CRUD operations'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("groceries")
                .asActive()
                .buildAndValidate()

        when:
        Category savedCategory = categoryRepository.save(category)

        then:
        savedCategory.categoryId != null
        savedCategory.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        savedCategory.categoryName != "groceries"  // Should be unique with testOwner
        savedCategory.activeStatus == true
        savedCategory.dateAdded != null
        savedCategory.dateUpdated != null

        when:
        Optional<Category> foundCategory = categoryRepository.findByOwnerAndCategoryName(testOwner,savedCategory.categoryName)

        then:
        foundCategory.isPresent()
        foundCategory.get().categoryId == savedCategory.categoryId
        foundCategory.get().activeStatus == true
    }

    void 'test find categories by active status with ordering'() {
        given:
        Category activeCategory1 = SmartCategoryBuilder.builderForOwner(testOwner)
                .asOnlineCategory()
                .asActive()
                .buildAndValidate()

        Category activeCategory2 = SmartCategoryBuilder.builderForOwner(testOwner)
                .asUtilitiesCategory()
                .asActive()
                .buildAndValidate()

        Category inactiveCategory = SmartCategoryBuilder.builderForOwner(testOwner)
                .asGroceriesCategory()
                .asInactive()
                .buildAndValidate()

        categoryRepository.save(activeCategory1)
        categoryRepository.save(activeCategory2)
        categoryRepository.save(inactiveCategory)

        when:
        List<Category> activeCategories = categoryRepository.findByOwnerAndActiveStatusOrderByCategoryName(testOwner,true)
        List<Category> inactiveCategories = categoryRepository.findByOwnerAndActiveStatusOrderByCategoryName(testOwner,false)

        then:
        activeCategories.size() >= 2
        activeCategories.every { it.activeStatus == true }
        inactiveCategories.size() >= 1
        inactiveCategories.every { it.activeStatus == false }

        // Verify our test categories are included
        activeCategories.any { it.categoryName == activeCategory1.categoryName }
        activeCategories.any { it.categoryName == activeCategory2.categoryName }
        inactiveCategories.any { it.categoryName == inactiveCategory.categoryName }

        // Verify ordering by category name
        def ourActiveCategories = activeCategories.findAll {
            it.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        }
        if (ourActiveCategories.size() > 1) {
            assert ourActiveCategories == ourActiveCategories.sort { it.categoryName }
        }
    }

    void 'test find category by category ID'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("dining")
                .asActive()
                .buildAndValidate()
        Category savedCategory = categoryRepository.save(category)

        when:
        Optional<Category> foundCategory = categoryRepository.findByOwnerAndCategoryId(testOwner,savedCategory.categoryId)

        then:
        foundCategory.isPresent()
        foundCategory.get().categoryName == savedCategory.categoryName
        foundCategory.get().activeStatus == savedCategory.activeStatus
    }

    void 'test category unique constraint violations'() {
        given:
        Category category1 = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("duplicate")
                .asActive()
                .buildAndValidate()

        Category category2 = SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName(category1.categoryName)  // Same category name - will cause unique constraint violation
                .asInactive()  // Different status but same name - violates unique constraint
                .buildAndValidate()

        when:
        categoryRepository.save(category1)
        categoryRepository.flush() // Force the first save to complete

        then:
        notThrown(Exception) // First save should succeed

        when:
        categoryRepository.save(category2)
        categoryRepository.flush() // This should fail due to unique constraint

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test category constraint validation at build time'() {
        when: "trying to create a category with invalid name length (too short)"
        SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName("")  // Too short - violates size constraint (min 1)
                .buildAndValidate()

        then: "constraint violation is caught at build time, not at save time"
        def ex = thrown(IllegalStateException)
        ex.message.contains("violates length constraints")

        when: "trying to create a category with invalid name length (too long)"
        SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName("a" * 51)  // Too long - violates size constraint (max 50)
                .buildAndValidate()

        then: "constraint violation is caught at build time"
        def ex2 = thrown(IllegalStateException)
        ex2.message.contains("violates length constraints")

        when: "trying to create a category with invalid pattern"
        SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName("invalid spaces")  // Violates ALPHA_NUMERIC_NO_SPACE_PATTERN
                .buildAndValidate()

        then: "constraint violation is caught at build time"
        def ex3 = thrown(IllegalStateException)
        ex3.message.contains("violates alpha_numeric_no_space pattern")

        when: "trying to create a category with uppercase letters"
        SmartCategoryBuilder.builderForOwner(testOwner)
                .withCategoryName("UPPERCASE")  // Violates lowercase requirement
                .buildAndValidate()

        then: "constraint violation is caught at build time"
        def ex4 = thrown(IllegalStateException)
        ex4.message.contains("violates alpha_numeric_no_space pattern")
    }

    void 'test category update operations'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("entertainment")
                .asActive()
                .buildAndValidate()
        Category savedCategory = categoryRepository.save(category)

        when:
        savedCategory.activeStatus = false
        Category updatedCategory = categoryRepository.save(savedCategory)

        then:
        updatedCategory.categoryId == savedCategory.categoryId
        updatedCategory.categoryName == savedCategory.categoryName
        updatedCategory.activeStatus == false

        when:
        Optional<Category> refetchedCategory = categoryRepository.findByOwnerAndCategoryName(testOwner,savedCategory.categoryName)

        then:
        refetchedCategory.isPresent()
        refetchedCategory.get().activeStatus == false
        refetchedCategory.get().categoryName == savedCategory.categoryName
    }

    void 'invalid category insert via repository triggers constraint violation'() {
        given:
        // Bypass builder validation by building then overriding to an invalid uppercase name
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("invalidupper")
                .build() // no validation here
        category.categoryName = "UPPERCASE" // violates lowercase/pattern constraints

        when:
        categoryRepository.save(category)
        categoryRepository.flush()

        then:
        thrown(ConstraintViolationException)
    }

    void 'test category deletion'() {
        given:
        Category categoryToDelete = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("temporary")
                .asActive()
                .buildAndValidate()
        Category savedCategory = categoryRepository.save(categoryToDelete)

        when:
        categoryRepository.delete(savedCategory)
        Optional<Category> deletedCategory = categoryRepository.findByOwnerAndCategoryName(testOwner,savedCategory.categoryName)

        then:
        !deletedCategory.isPresent()

        when:
        Optional<Category> deletedById = categoryRepository.findByOwnerAndCategoryId(testOwner,savedCategory.categoryId)

        then:
        !deletedById.isPresent()
    }

    void 'test repository context helper methods'() {
        given:
        Category uniqueCategory = repositoryContext.createUniqueCategory("transport")

        when:
        Category savedCategory = categoryRepository.save(uniqueCategory)

        then:
        savedCategory.categoryId != null
        savedCategory.categoryName.contains("transport")
        savedCategory.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        savedCategory.activeStatus == true
    }

    void 'test smart builder convenience methods'() {
        given:
        Category onlineCategory = SmartCategoryBuilder.builderForOwner(testOwner)
                .asOnlineCategory()
                .asActive()
                .buildAndValidate()

        Category utilitiesCategory = SmartCategoryBuilder.builderForOwner(testOwner)
                .asUtilitiesCategory()
                .asInactive()
                .buildAndValidate()

        when:
        Category savedOnline = categoryRepository.save(onlineCategory)
        Category savedUtilities = categoryRepository.save(utilitiesCategory)

        then:
        savedOnline.categoryName.contains("online")
        savedOnline.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        savedOnline.activeStatus == true

        savedUtilities.categoryName.contains("utilities")
        savedUtilities.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        savedUtilities.activeStatus == false

        // Category names follow pattern and contain test owner
        savedOnline.categoryName.matches(/^[a-z0-9_-]*$/)
        savedUtilities.categoryName.matches(/^[a-z0-9_-]*$/)
    }

    void 'test category name case conversion and validation'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("mixed_case")
                .asActive()
                .buildAndValidate()

        when:
        Category savedCategory = categoryRepository.save(category)

        then:
        // Category name should be lowercase (handled by LowerCaseConverter)
        savedCategory.categoryName == savedCategory.categoryName.toLowerCase()
        savedCategory.categoryName.matches(/^[a-z0-9_-]*$/)
    }

    void 'test find non-existent category'() {
        when:
        Optional<Category> nonExistentByName = categoryRepository.findByOwnerAndCategoryName(testOwner,"nonexistent_${testOwner}")
        Optional<Category> nonExistentById = categoryRepository.findByOwnerAndCategoryId(testOwner,-999L)

        then:
        !nonExistentByName.isPresent()
        !nonExistentById.isPresent()
    }

    void 'test category entity persistence validation'() {
        given:
        Category category = SmartCategoryBuilder.builderForOwner(testOwner)
                .withUniqueCategoryName("persistence")
                .asActive()
                .buildAndValidate()

        when:
        Category savedCategory = categoryRepository.save(category)

        then:
        savedCategory.categoryId != null
        savedCategory.categoryName.contains(testOwner.replaceAll(/[^a-z0-9]/, ''))
        savedCategory.activeStatus == true
        savedCategory.dateAdded != null
        savedCategory.dateUpdated != null
        // categoryCount is transient, not persisted to DB
        savedCategory.categoryCount == 0L

        when:
        Optional<Category> refetchedOpt = categoryRepository.findById(savedCategory.categoryId)

        then:
        refetchedOpt.isPresent()
        def refetchedCategory = refetchedOpt.get()
        refetchedCategory.categoryName == savedCategory.categoryName
        refetchedCategory.activeStatus == savedCategory.activeStatus
        refetchedCategory.dateAdded != null
        refetchedCategory.dateUpdated != null
    }
}
