package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.domain.Account
import finance.helpers.CategoryBuilder
import finance.repositories.CategoryRepository
import spock.lang.Specification
import finance.domain.Category

import javax.validation.ConstraintViolation
import javax.validation.Validator

class CategoryServiceSpec extends BaseServiceSpec {

    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterServiceMock)

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
