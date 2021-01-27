package finance.services

import finance.domain.Category
import finance.helpers.CategoryBuilder

import javax.validation.ConstraintViolation
import javax.validation.ValidationException

class CategoryServiceSpec extends BaseServiceSpec {

    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterService)

    void setup() {
    }

    void 'test - insert category'() {
        given:
        Category category = CategoryBuilder.builder().build()
        Set<ConstraintViolation<Category>> constraintViolations = validator.validate(category)

        when:
        categoryService.insertCategory(category)

        then:
        1 * validatorMock.validate(category) >> constraintViolations
        1 * categoryRepositoryMock.saveAndFlush(category)
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
        //1 * meterService.incrementExceptionThrownCounter('ValidationException')
        1 * meterRegistryMock.counter(_) >> counter
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
