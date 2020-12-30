package finance.services

import com.fasterxml.jackson.databind.ObjectMapper
import finance.helpers.CategoryBuilder
import finance.repositories.CategoryRepository
import spock.lang.Specification
import finance.domain.Category

import javax.validation.Validator

class CategoryServiceSpec extends BaseServiceSpec {

    protected CategoryService categoryService = new CategoryService(categoryRepositoryMock, validatorMock, meterServiceMock)

    void setup() {
    }

    void 'test - insert category'() {
        given:
        Category category = CategoryBuilder.builder().build()

        when:
        categoryService.insertCategory(category)

        then:
        1 * validatorMock.validate(category) >> ([] as Set)
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
