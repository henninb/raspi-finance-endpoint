package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Category
import finance.services.StandardizedCategoryService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException

class CategoryMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    StandardizedCategoryService categoryService

    def "createCategory mutation succeeds with valid input"() {
        given:
        withUserRole()
        def category = new Category(
                0L,
                true,
                "test_create_category"
        )

        when:
        def result = mutationController.createCategory(category)

        then:
        result != null
        result.categoryId > 0
        result.categoryName == "test_create_category"
        result.activeStatus == true
    }

    def "createCategory mutation fails validation for empty category name"() {
        given:
        withUserRole()
        def category = new Category(
                0L,
                true,
                ""                      // invalid: empty
        )

        when:
        mutationController.createCategory(category)

        then:
        thrown(ConstraintViolationException)
    }

    def "createCategory mutation fails validation for category name too long"() {
        given:
        withUserRole()
        def category = new Category(
                0L,
                true,
                "a" * 51                // invalid: exceeds 50 character limit
        )

        when:
        mutationController.createCategory(category)

        then:
        thrown(ConstraintViolationException)
    }

    def "createCategory mutation fails validation for category name with spaces"() {
        given:
        withUserRole()
        def category = new Category(
                0L,
                true,
                "invalid category"      // invalid: contains space
        )

        when:
        mutationController.createCategory(category)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateCategory mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestCategory("test_update_category")
        def updated = new Category(
                created.categoryId,
                false,                  // change active status
                "test_update_category"
        )

        when:
        def result = mutationController.updateCategory(updated)

        then:
        result != null
        result.categoryId == created.categoryId
        result.categoryName == "test_update_category"
        result.activeStatus == false
    }

    def "updateCategory mutation fails for non-existent category"() {
        given:
        withUserRole()
        def category = new Category(
                999999L,                // non-existent ID
                true,
                "nonexistent"
        )

        when:
        mutationController.updateCategory(category)

        then:
        thrown(RuntimeException)
    }

    def "deleteCategory mutation returns true for existing category"() {
        given:
        withUserRole()
        def created = createTestCategory("test_delete_category")

        when:
        def deleted = mutationController.deleteCategory(created.categoryName)

        then:
        deleted == true
    }

    def "deleteCategory mutation returns false for missing category"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteCategory("nonexistent_category") == false
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
