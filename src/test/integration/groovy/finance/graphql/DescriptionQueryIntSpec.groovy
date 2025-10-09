package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Description
import finance.services.StandardizedDescriptionService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

class DescriptionQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    StandardizedDescriptionService descriptionService

    @Shared @Autowired
    GraphQLQueryController queryController

    def "fetch all descriptions via query controller"() {
        given:
        createTestDescription("test_amazon")
        createTestDescription("test_walmart")

        when:
        def descriptions = queryController.descriptions()

        then:
        descriptions != null
        descriptions.size() >= 2
        descriptions.any { it.descriptionName == "test_amazon" }
        descriptions.any { it.descriptionName == "test_walmart" }
    }

    def "fetch description by name via query controller"() {
        given:
        def savedDescription = createTestDescription("test_target")

        when:
        def result = queryController.description("test_target")

        then:
        result != null
        result.descriptionId == savedDescription.descriptionId
        result.descriptionName == "test_target"
        result.activeStatus == true
    }

    def "handle description not found via query controller"() {
        expect:
        queryController.description("nonexistent_description") == null
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
