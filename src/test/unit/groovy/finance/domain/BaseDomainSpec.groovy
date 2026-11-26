package finance.domain

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import spock.lang.Specification

import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

class BaseDomainSpec extends Specification {
    protected ValidatorFactory validatorFactory
    protected Validator validator
    protected ObjectMapper mapper = JsonMapper.builderWithJackson2Defaults().findAndAddModules().build()

    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.getValidator()
    }

    void cleanup() {
        validatorFactory.close()
    }
}
