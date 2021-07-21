package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

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
        categoryInserted.category == category.category
        1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryRepositoryMock.saveAndFlush(category) >> category
        0 * _
    }

    void 'test - insert category empty categoryName'() {
        given:
        Category category = CategoryBuilder.builder().withCategory('').build()
        Set<ConstraintViolation<Category>> constraintViolations = validator.validate(category)

        when:
        categoryService.insertCategory(category)

        then:
        constraintViolations.size() == 1
        thrown(ValidationException)
        1 * validatorMock.validate(category) >> constraintViolations
        1 * meterRegistryMock.counter(validationExceptionThrownMeter) >> counter
        1 * counter.increment()
        0 * _
    }

    void 'test - delete category'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        categoryService.deleteByCategoryName(category.category)

        then:
        1 * categoryRepositoryMock.deleteByCategory(category.category)
        0 * _
    }
}
