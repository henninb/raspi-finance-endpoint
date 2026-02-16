package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLMutationController
import finance.domain.Parameter
import finance.services.ParameterService
import org.springframework.beans.factory.annotation.Autowired
import jakarta.validation.ConstraintViolationException

class ParameterMutationIntSpec extends BaseIntegrationSpec {

    @Autowired
    GraphQLMutationController mutationController

    @Autowired
    ParameterService parameterService

    def "createParameter mutation succeeds with valid input"() {
        given:
        withUserRole()
        def parameter = new Parameter(
                0L,
                "",
                "test_create_param",
                "test_create_value",
                true
        )

        when:
        def result = mutationController.createParameter(parameter)

        then:
        result != null
        result.parameterId > 0
        result.parameterName == "test_create_param"
        result.parameterValue == "test_create_value"
        result.activeStatus == true
    }

    def "createParameter mutation fails validation for empty parameter name"() {
        given:
        withUserRole()
        def parameter = new Parameter(
                0L,
                "",
                "",                     // invalid: empty
                "test_value",
                true
        )

        when:
        mutationController.createParameter(parameter)

        then:
        thrown(ConstraintViolationException)
    }

    def "createParameter mutation fails validation for parameter name too long"() {
        given:
        withUserRole()
        def parameter = new Parameter(
                0L,
                "",
                "a" * 51,               // invalid: exceeds 50 character limit
                "test_value",
                true
        )

        when:
        mutationController.createParameter(parameter)

        then:
        thrown(ConstraintViolationException)
    }

    def "createParameter mutation fails validation for empty parameter value"() {
        given:
        withUserRole()
        def parameter = new Parameter(
                0L,
                "",
                "test_param",
                "",                     // invalid: empty
                true
        )

        when:
        mutationController.createParameter(parameter)

        then:
        thrown(ConstraintViolationException)
    }

    def "updateParameter mutation succeeds with valid input"() {
        given:
        withUserRole()
        def created = createTestParameter("test_update_param", "original_value")
        def updated = new Parameter(
                created.parameterId,
                "",
                "test_update_param",
                "updated_value",
                true
        )

        when:
        def result = mutationController.updateParameter(updated)

        then:
        result != null
        result.parameterId == created.parameterId
        result.parameterName == "test_update_param"
        result.parameterValue == "updated_value"
        result.activeStatus == true
    }

    def "updateParameter mutation fails for non-existent parameter"() {
        given:
        withUserRole()
        def parameter = new Parameter(
                999999L,                // non-existent ID
                "",
                "test_param",
                "test_value",
                true
        )

        when:
        mutationController.updateParameter(parameter)

        then:
        thrown(RuntimeException)
    }

    def "deleteParameter mutation returns true for existing parameter"() {
        given:
        withUserRole()
        def created = createTestParameter("test_delete_param", "delete_value")

        when:
        def deleted = mutationController.deleteParameter(created.parameterId)

        then:
        deleted == true
    }

    def "deleteParameter mutation returns false for missing parameter id"() {
        given:
        withUserRole()

        expect:
        mutationController.deleteParameter(999999L) == false
    }

    private Parameter createTestParameter(String name, String value) {
        Parameter parameter = new Parameter(
                0L,
                "",
                name,
                value,
                true
        )
        def result = parameterService.save(parameter)
        return result.data
    }
}
