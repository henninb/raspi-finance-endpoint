package finance.graphql

import finance.BaseIntegrationSpec
import finance.controllers.graphql.GraphQLQueryController
import finance.domain.Parameter
import finance.services.StandardizedParameterService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

class ParameterQueryIntSpec extends BaseIntegrationSpec {

    @Shared @Autowired
    StandardizedParameterService parameterService

    @Shared @Autowired
    GraphQLQueryController queryController

    def "fetch all parameters via query controller"() {
        given:
        createTestParameter("test_param_1", "value_1")
        createTestParameter("test_param_2", "value_2")

        when:
        def parameters = queryController.parameters()

        then:
        parameters != null
        parameters.size() >= 2
        parameters.any { it.parameterName == "test_param_1" && it.parameterValue == "value_1" }
        parameters.any { it.parameterName == "test_param_2" && it.parameterValue == "value_2" }
    }

    def "fetch parameter by ID via query controller"() {
        given:
        def savedParameter = createTestParameter("test_param_fetch", "fetch_value")

        when:
        def result = queryController.parameter(savedParameter.parameterId)

        then:
        result != null
        result.parameterId == savedParameter.parameterId
        result.parameterName == "test_param_fetch"
        result.parameterValue == "fetch_value"
        result.activeStatus == true
    }

    def "handle parameter not found via query controller"() {
        expect:
        queryController.parameter(999999L) == null
    }

    def "fetch parameter should include dateAdded and dateUpdated fields"() {
        given:
        def savedParameter = createTestParameter("test_param_dates", "dates_value")

        when:
        def result = queryController.parameter(savedParameter.parameterId)

        then:
        result != null
        result.parameterId == savedParameter.parameterId
        result.parameterName == "test_param_dates"
        result.parameterValue == "dates_value"
        result.activeStatus == true
        result.dateAdded != null
        result.dateUpdated != null
    }

    private Parameter createTestParameter(String name, String value) {
        Parameter parameter = new Parameter(
            0L,
            name,
            value,
            true
        )
        def result = parameterService.save(parameter)
        return result.data
    }
}
