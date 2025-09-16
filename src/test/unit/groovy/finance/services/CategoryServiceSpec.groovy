package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class CategoryServiceSpec extends BaseServiceSpec {
    io.micrometer.core.instrument.simple.SimpleMeterRegistry registry

    void setup() {
        categoryService.validator = validatorMock
        registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        categoryService.meterService = new MeterService(registry)
    }

    void 'test - insert category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        Set<ConstraintViolation<Category>> constraintViolations = validator.validate(category)

        when:
        Category categoryInserted = categoryService.insertCategory(category)

        then:
        categoryInserted.categoryName == category.categoryName
        1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryRepositoryMock.saveAndFlush(category) >> category
        0 * _
    }

    void 'test - insert category empty categoryName'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()

        // Create mock constraint violation
        ConstraintViolation<Category> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 3 and 40"

        Set<ConstraintViolation<Category>> constraintViolations = [violation] as Set

        when:
        categoryService.insertCategory(category)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(category) >> constraintViolations
        def c = registry.find('exception.thrown.counter').tag('exception.name.tag','ValidationException').counter()
        assert c != null && c.count() >= 1
    }

    void 'test - delete category'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        categoryService.deleteCategory(category.categoryName)

        then:
        1 * categoryRepositoryMock.findByCategoryName(category.categoryName) >> Optional.of(category)
        1 * categoryRepositoryMock.delete(category)
        0 * _
    }

    void 'updateCategory - success updates fields and saves'() {
        given:
        def existing = CategoryBuilder.builder().withCategory('old').build()
        existing.categoryId = 10L
        def patch = new Category(categoryId: 10L, categoryName: 'new', activeStatus: false)

        when:
        def result = categoryService.updateCategory(patch)

        then:
        1 * categoryRepositoryMock.findByCategoryId(10L) >> Optional.of(existing)
        1 * categoryRepositoryMock.saveAndFlush({ it.categoryId == 10L && it.categoryName == 'new' && it.activeStatus == false }) >> { it[0] }
        result.categoryName == 'new'
        !result.activeStatus
    }

    void 'updateCategory - not found throws RuntimeException'() {
        given:
        def patch = new Category(categoryId: 99L, categoryName: 'x')

        when:
        categoryService.updateCategory(patch)

        then:
        1 * categoryRepositoryMock.findByCategoryId(99L) >> Optional.empty()
        thrown(RuntimeException)
    }

    void 'mergeCategories - both exist, reassigns transactions and deactivates source'() {
        given:
        def c1 = CategoryBuilder.builder().withCategory('keep').build()
        c1.categoryCount = 3
        def c2 = CategoryBuilder.builder().withCategory('drop').build()
        c2.categoryCount = 2
        def tx1 = finance.helpers.TransactionBuilder.builder().withCategory('drop').build()
        def tx2 = finance.helpers.TransactionBuilder.builder().withCategory('drop').build()

        when:
        def merged = categoryService.mergeCategories('keep','drop')

        then:
        1 * categoryRepositoryMock.findByCategoryName('keep') >> Optional.of(c1)
        1 * categoryRepositoryMock.findByCategoryName('drop') >> Optional.of(c2)
        1 * transactionRepositoryMock.findByCategoryAndActiveStatusOrderByTransactionDateDesc('drop', true) >> [tx1, tx2]
        2 * transactionRepositoryMock.saveAndFlush({ it.category == 'keep' }) >> { it[0] }
        1 * categoryRepositoryMock.saveAndFlush({ it.is(c1) && it.categoryCount == 5 }) >> { it[0] }
        !c2.activeStatus
        merged.is(c1)
    }

    void 'mergeCategories - target not found throws'() {
        when:
        categoryService.mergeCategories('missing','drop')

        then:
        1 * categoryRepositoryMock.findByCategoryName('missing') >> Optional.empty()
        thrown(RuntimeException)
    }

    void 'mergeCategories - source not found throws'() {
        given:
        def keep = CategoryBuilder.builder().withCategory('keep').build()

        when:
        categoryService.mergeCategories('keep','missing')

        then:
        1 * categoryRepositoryMock.findByCategoryName('keep') >> Optional.of(keep)
        1 * categoryRepositoryMock.findByCategoryName('missing') >> Optional.empty()
        thrown(RuntimeException)
    }
}
