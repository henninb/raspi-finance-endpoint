package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.CategoryInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Category
import finance.services.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException

class CategoryMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    CategoryService categoryService

    def "createCategory mutation succeeds with valid input"() {
        given:
        withUserRole()
        def categoryInput = new CategoryInputDto(
                null,
                "test_create_category",
                true
        )

        when:
        def result = mutationController.createCategory(categoryInput)

        then:
        result != null
        result.categoryId > 0
        result.categoryName == "test_create_category"
        result.activeStatus == true
    }

    def "createCategory mutation fails validation for empty category name"() {
        given:
        withUserRole()
        def categoryInput = new CategoryInputDto(
                null,
                "",                      // invalid: empty
                true
        )

        when:
        mutationController.createCategory(categoryInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createCategory mutation fails validation for category name too long"() {
        given:
        withUserRole()
        def categoryInput = new CategoryInputDto(
                null,
                "a" * 51,                // invalid: exceeds 50 character limit
                true
        )

        when:
        mutationController.createCategory(categoryInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createCategory mutation fails validation for category name with spaces"() {
        given:
        withUserRole()
        def categoryInput = new CategoryInputDto(
                null,
                "invalid category",      // invalid: contains space
                true
        )

        when:
        mutationController.createCategory(categoryInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateCategory mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestCategory("test_update_category")
        def categoryInput = new CategoryInputDto(
                created.categoryId,
                "test_update_category",
                false                   // change active status
        )

        when:
        def result = mutationController.updateCategory(categoryInput, null)

        then:
        result != null
        result.categoryId == created.categoryId
        result.categoryName == "test_update_category"
        result.activeStatus == false
    }

    def "updateCategory mutation fails for non-existent category"() {
        given:
        withUserRole()
        def categoryInput = new CategoryInputDto(
                999999L,                // non-existent ID
                "nonexistent",
                true
        )

        when:
        mutationController.updateCategory(categoryInput, null)

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
