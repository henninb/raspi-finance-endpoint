package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.dto.DescriptionInputDto
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Description
import finance.services.DescriptionService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException

class DescriptionMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    DescriptionService descriptionService

    def "createDescription mutation succeeds with valid input"() {
        given:
        withUserRole()
        def descriptionInput = new DescriptionInputDto(
                null,
                "test_create_description",
                true
        )

        when:
        def result = mutationController.createDescription(descriptionInput)

        then:
        result != null
        result.descriptionId > 0
        result.descriptionName == "test_create_description"
        result.activeStatus == true
    }

    def "createDescription mutation fails validation for empty description name"() {
        given:
        withUserRole()
        def descriptionInput = new DescriptionInputDto(
                null,
                "",                      // invalid: empty
                true
        )

        when:
        mutationController.createDescription(descriptionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createDescription mutation fails validation for description name too long"() {
        given:
        withUserRole()
        def descriptionInput = new DescriptionInputDto(
                null,
                "a" * 51,                // invalid: exceeds 50 character limit
                true
        )

        when:
        mutationController.createDescription(descriptionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "createDescription mutation fails validation for description name with spaces"() {
        given:
        withUserRole()
        def descriptionInput = new DescriptionInputDto(
                null,
                "invalid description",   // invalid: contains space
                true
        )

        when:
        mutationController.createDescription(descriptionInput)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateDescription mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestDescription("test_update_description")
        def descriptionInput = new DescriptionInputDto(
                created.descriptionId,
                "test_update_description",
                false                   // change active status
        )

        when:
        def result = mutationController.updateDescription(descriptionInput, null)

        then:
        result != null
        result.descriptionId == created.descriptionId
        result.descriptionName == "test_update_description"
        result.activeStatus == false
    }

    def "updateDescription mutation fails for non-existent description"() {
        given:
        withUserRole()
        def descriptionInput = new DescriptionInputDto(
                999999L,                // non-existent ID
                "nonexistent",
                true
        )

        when:
        mutationController.updateDescription(descriptionInput, null)

        then:
        thrown(RuntimeException)
    }

    def "deleteDescription mutation returns true for existing description"() {
        given:
        withUserRole()
        def created = createTestDescription("test_delete_description")

        when:
        def deleted = mutationController.deleteDescription(created.descriptionName)

        then:
        deleted == true
    }

    def "deleteDescription mutation returns false for missing description"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteDescription("nonexistent_description") == false
    }

    private Description createTestDescription(String name) {
        Description description = new Description(
                0L,
                true,
                name
        )
        def result = descriptionService.save(description)
        return result.data
    }
}
