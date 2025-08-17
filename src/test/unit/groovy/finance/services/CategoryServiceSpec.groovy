package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder
import jakarta.validation.ConstraintViolation
import jakarta.validation.ValidationException

@SuppressWarnings("GroovyAccessibility")
class CategoryServiceSpec extends BaseServiceSpec {

    void setup() {
        categoryService.validator = validatorMock
        categoryService.meterService = meterService
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
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        _ * _  // Allow any other interactions (logging, etc.)
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
}
