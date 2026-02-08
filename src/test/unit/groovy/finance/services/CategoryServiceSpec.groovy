package finance.services

import finance.domain.Category
import finance.domain.ServiceResult
import finance.helpers.CategoryBuilder
import finance.repositories.CategoryRepository
import finance.repositories.TransactionRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException

/**
 * TDD Specification for CategoryService
 * Tests the Category service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedCategoryServiceSpec extends BaseServiceSpec {

    def categoryRepositoryMock = Mock(CategoryRepository)
    def transactionRepositoryMock = Mock(TransactionRepository)
    def standardizedCategoryService = new CategoryService(categoryRepositoryMock, transactionRepositoryMock)

    void setup() {
        standardizedCategoryService.meterService = meterService
        standardizedCategoryService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with categories when found"() {
        given: "existing active categories"
        def categories = [
            CategoryBuilder.builder().withCategoryName("groceries").build(),
            CategoryBuilder.builder().withCategoryName("utilities").build()
        ]

        when: "finding all active categories"
        def result = standardizedCategoryService.findAllActive()

        then: "should return Success with categories"
        1 * categoryRepositoryMock.findByOwnerAndActiveStatusOrderByCategoryName(TEST_OWNER, true) >> categories
        1 * transactionRepositoryMock.countByOwnerAndCategoryNameIn(TEST_OWNER, ["groceries", "utilities"]) >> [
            ["groceries", 5L] as Object[],
            ["utilities", 3L] as Object[]
        ]
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].categoryName == "groceries"
        result.data[0].categoryCount == 5L
        result.data[1].categoryName == "utilities"
        result.data[1].categoryCount == 3L
        0 * _
    }

    def "findAllActive should return Success with empty list when no categories found"() {
        when: "finding all active categories with none existing"
        def result = standardizedCategoryService.findAllActive()

        then: "should return Success with empty list"
        1 * categoryRepositoryMock.findByOwnerAndActiveStatusOrderByCategoryName(TEST_OWNER, true) >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with category when found"() {
        given: "existing category"
        def category = CategoryBuilder.builder().withCategoryId(1L).build()

        when: "finding by valid ID"
        def result = standardizedCategoryService.findById(1L)

        then: "should return Success with category"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 1L) >> Optional.of(category)
        result instanceof ServiceResult.Success
        result.data.categoryId == 1L
        0 * _
    }

    def "findById should return NotFound when category does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedCategoryService.findById(999L)

        then: "should return NotFound result"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Category not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved category when valid"() {
        given: "valid category"
        def category = CategoryBuilder.builder().build()
        def savedCategory = CategoryBuilder.builder().withCategoryId(1L).build()
        Set<ConstraintViolation<Category>> noViolations = [] as Set

        when: "saving category"
        def result = standardizedCategoryService.save(category)

        then: "should return Success with saved category"
        1 * validatorMock.validate(category) >> noViolations
        1 * categoryRepositoryMock.saveAndFlush(category) >> savedCategory
        result instanceof ServiceResult.Success
        result.data.categoryId == 1L
        0 * _
    }

    def "save should return ValidationError when category has constraint violations"() {
        given: "invalid category"
        def category = CategoryBuilder.builder().withCategoryName("").build()
        ConstraintViolation<Category> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "categoryName"
        violation.propertyPath >> mockPath
        violation.message >> "size must be between 1 and 50"
        Set<ConstraintViolation<Category>> violations = [violation] as Set

        when: "saving invalid category"
        def result = standardizedCategoryService.save(category)

        then: "should return ValidationError result"
        1 * validatorMock.validate(category) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("size must be between 1 and 50")
    }

    def "save should return BusinessError when duplicate category exists"() {
        given: "category that will cause duplicate key violation"
        def category = CategoryBuilder.builder().withCategoryName("duplicate").build()
        Set<ConstraintViolation<Category>> noViolations = [] as Set

        when: "saving duplicate category"
        def result = standardizedCategoryService.save(category)

        then: "should return BusinessError result"
        1 * validatorMock.validate(category) >> noViolations
        1 * categoryRepositoryMock.saveAndFlush(category) >> {
            throw new DataIntegrityViolationException("Duplicate entry")
        }
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated category when exists"() {
        given: "existing category to update"
        def existingCategory = CategoryBuilder.builder().withCategoryId(1L).withCategoryName("old").build()
        def updatedCategory = CategoryBuilder.builder().withCategoryId(1L).withCategoryName("new").build()

        when: "updating existing category"
        def result = standardizedCategoryService.update(updatedCategory)

        then: "should return Success with updated category"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 1L) >> Optional.of(existingCategory)
        1 * categoryRepositoryMock.saveAndFlush(_ as Category) >> { Category cat ->
            assert cat.categoryName == "new"
            return cat
        }
        result instanceof ServiceResult.Success
        result.data.categoryName == "new"
        0 * _
    }

    def "update should return NotFound when category does not exist"() {
        given: "category with non-existent ID"
        def category = CategoryBuilder.builder().withCategoryId(999L).build()

        when: "updating non-existent category"
        def result = standardizedCategoryService.update(category)

        then: "should return NotFound result"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Category not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when category exists"() {
        given: "existing category"
        def category = CategoryBuilder.builder().withCategoryId(1L).build()

        when: "deleting existing category"
        def result = standardizedCategoryService.deleteById(1L)

        then: "should return Success"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 1L) >> Optional.of(category)
        1 * categoryRepositoryMock.delete(category)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when category does not exist"() {
        when: "deleting non-existent category"
        def result = standardizedCategoryService.deleteById(999L)

        then: "should return NotFound result"
        1 * categoryRepositoryMock.findByOwnerAndCategoryId(TEST_OWNER, 999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Category not found: 999")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====



    // ===== TDD Tests for mergeCategories() =====

    def "mergeCategories should successfully merge two existing categories"() {
        given: "two existing categories"
        def category1 = CategoryBuilder.builder().withCategoryName("category1").withCategoryCount(5L).build()
        def category2 = CategoryBuilder.builder().withCategoryName("category2").withCategoryCount(3L).build()
        def transactions = [] // Empty list for simplicity

        when: "merging categories"
        def result = standardizedCategoryService.mergeCategories("category1", "category2")

        then: "should merge successfully"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, "category1") >> Optional.of(category1)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, "category2") >> Optional.of(category2)
        1 * transactionRepositoryMock.findByOwnerAndCategoryAndActiveStatusOrderByTransactionDateDesc(TEST_OWNER, "category2", true) >> transactions
        1 * categoryRepositoryMock.saveAndFlush(category1) >> category1
        result.categoryName == "category1"
        result.categoryCount == 8L // 5 + 3
        !category2.activeStatus // category2 should be deactivated
        0 * _
    }

    // ===== TDD Tests for Error Handling in Business Methods =====


    def "mergeCategories should throw RuntimeException when first category not found"() {
        when: "merging with non-existent first category"
        standardizedCategoryService.mergeCategories("missing1", "category2")

        then: "should throw RuntimeException"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, "missing1") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }

    def "mergeCategories should throw RuntimeException when second category not found"() {
        given: "existing first category"
        def category1 = CategoryBuilder.builder().withCategoryName("category1").build()

        when: "merging with non-existent second category"
        standardizedCategoryService.mergeCategories("category1", "missing2")

        then: "should throw RuntimeException"
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, "category1") >> Optional.of(category1)
        1 * categoryRepositoryMock.findByOwnerAndCategoryName(TEST_OWNER, "missing2") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}