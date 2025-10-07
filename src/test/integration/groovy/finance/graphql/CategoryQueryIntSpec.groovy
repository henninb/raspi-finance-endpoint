package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Category
import finance.services.StandardizedCategoryService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

class CategoryQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    StandardizedCategoryService categoryService

    @Shared @Autowired
    GraphQLQueryController queryController

    def "fetch all categories via query controller"() {
        given:
        createTestCategory("test_groceries")
        createTestCategory("test_utilities")

        when:
        def categories = queryController.categories()

        then:
        categories != null
        categories.size() >= 2
        categories.any { it.categoryName == "test_groceries" }
        categories.any { it.categoryName == "test_utilities" }
    }

    def "fetch category by name via query controller"() {
        given:
        def savedCategory = createTestCategory("test_entertainment")

        when:
        def result = queryController.category("test_entertainment")

        then:
        result != null
        result.categoryId == savedCategory.categoryId
        result.categoryName == "test_entertainment"
        result.activeStatus == true
    }

    def "handle category not found via query controller"() {
        expect:
        queryController.category("nonexistent_category") == null
    }

    private Category createTestCategory(String name) {
        Category category = new Category(
            0L,
            true,
            name
        )
        def result = categoryService.save(category)
        return result.data
    }
}
