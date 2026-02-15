package finance.repositories

import finance.BaseIntegrationSpec
import finance.domain.Parameter
import finance.helpers.SmartParameterBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared

/**
 * INTEGRATION TEST - ParameterRepository using robust, isolated architecture
 *
 * This integration test demonstrates the new architecture:
 * ✅ No hardcoded parameter names - all use testOwner for uniqueness
 * ✅ SmartBuilder pattern with constraint validation
 * ✅ Test isolation - each test gets its own test data
 * ✅ Constraint-aware test data creation
 * ✅ Build-time validation prevents invalid data
 * ✅ System parameter specific testing for configuration management
 */
class ParameterRepositoryIntSpec extends BaseIntegrationSpec {

    @Autowired
    ParameterRepository parameterRepository

    @Shared
    def repositoryContext

    def setupSpec() {
        repositoryContext = testFixtures.createRepositoryTestContext(testOwner)
    }

    void 'test parameter repository basic CRUD operations'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("database")
                .withUniqueParameterValue("postgres")
                .asActive()
                .buildAndValidate()

        when:
        Parameter savedParameter = parameterRepository.save(parameter)

        then:
        savedParameter.parameterId != null
        savedParameter.parameterId > 0
        savedParameter.parameterName.contains(testOwner)
        savedParameter.parameterValue.contains(testOwner)
        savedParameter.activeStatus == true
        savedParameter.dateAdded != null
        savedParameter.dateUpdated != null

        when:
        Optional<Parameter> foundParameter = parameterRepository.findByOwnerAndParameterName(testOwner,savedParameter.parameterName)

        then:
        foundParameter.isPresent()
        foundParameter.get().parameterId == savedParameter.parameterId
        foundParameter.get().parameterValue == savedParameter.parameterValue
        foundParameter.get().activeStatus == true
    }

    void 'test find parameter by parameter ID'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("timeout")
                .withUniqueParameterValue("30000")
                .asActive()
                .buildAndValidate()
        Parameter savedParameter = parameterRepository.save(parameter)

        when:
        Optional<Parameter> foundParameter = parameterRepository.findByOwnerAndParameterId(testOwner,savedParameter.parameterId)

        then:
        foundParameter.isPresent()
        foundParameter.get().parameterName == savedParameter.parameterName
        foundParameter.get().parameterValue == savedParameter.parameterValue
        foundParameter.get().activeStatus == savedParameter.activeStatus
    }

    void 'test find active parameters'() {
        given:
        Parameter activeParameter1 = SmartParameterBuilder.builderForOwner(testOwner)
                .asPaymentAccountParameter()
                .asActive()
                .buildAndValidate()

        Parameter activeParameter2 = SmartParameterBuilder.builderForOwner(testOwner)
                .asConfigParameter("logging")
                .asActive()
                .buildAndValidate()

        Parameter inactiveParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withKeyValuePair("deprecated", "oldvalue")
                .asInactive()
                .buildAndValidate()

        parameterRepository.save(activeParameter1)
        parameterRepository.save(activeParameter2)
        parameterRepository.save(inactiveParameter)

        when:
        List<Parameter> activeParameters = parameterRepository.findByOwnerAndActiveStatusIsTrue(testOwner)

        then:
        activeParameters.size() >= 2
        activeParameters.every { it.activeStatus == true }

        // Verify our test parameters are included
        activeParameters.any { it.parameterName == activeParameter1.parameterName }
        activeParameters.any { it.parameterName == activeParameter2.parameterName }

        // Verify inactive parameter is not included
        !activeParameters.any { it.parameterName == inactiveParameter.parameterName }
    }

    void 'test parameter unique constraint on parameter name'() {
        given:
        String uniqueParameterName = "unique_param_${testOwner}".toLowerCase()

        Parameter parameter1 = SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName(uniqueParameterName)
                .withUniqueParameterValue("value1")
                .asActive()
                .buildAndValidate()

        when:
        Parameter savedParameter1 = parameterRepository.save(parameter1)

        then:
        savedParameter1.parameterId != null

        when: "trying to save parameter with same parameter name"
        Parameter parameter2 = SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName(uniqueParameterName)  // Same parameter name
                .withUniqueParameterValue("value2")      // Different value
                .asActive()
                .buildAndValidate()

        parameterRepository.save(parameter2)

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test parameter unique constraint on parameter value'() {
        given:
        String uniqueParameterValue = "unique_value_${testOwner}".toLowerCase()

        Parameter parameter1 = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("param1")
                .withParameterValue(uniqueParameterValue)
                .asActive()
                .buildAndValidate()

        when:
        Parameter savedParameter1 = parameterRepository.save(parameter1)

        then:
        savedParameter1.parameterId != null

        when: "trying to save parameter with same parameter value"
        Parameter parameter2 = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("param2")      // Different name
                .withParameterValue(uniqueParameterValue) // Same value
                .asActive()
                .buildAndValidate()

        parameterRepository.save(parameter2)

        then:
        thrown(DataIntegrityViolationException)
    }

    void 'test parameter update operations'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("feature")
                .withUniqueParameterValue("enabled")
                .asActive()
                .buildAndValidate()
        Parameter savedParameter = parameterRepository.save(parameter)

        when:
        savedParameter.activeStatus = false
        savedParameter.parameterValue = "disabled_${testOwner}".toLowerCase()
        Parameter updatedParameter = parameterRepository.save(savedParameter)

        then:
        updatedParameter.parameterId == savedParameter.parameterId
        updatedParameter.parameterName == savedParameter.parameterName
        updatedParameter.parameterValue == "disabled_${testOwner}".toLowerCase()
        updatedParameter.activeStatus == false

        when:
        Optional<Parameter> refetchedParameter = parameterRepository.findByOwnerAndParameterName(testOwner,savedParameter.parameterName)

        then:
        refetchedParameter.isPresent()
        refetchedParameter.get().activeStatus == false
        refetchedParameter.get().parameterValue == "disabled_${testOwner}".toLowerCase()
    }

    void 'test parameter deletion'() {
        given:
        Parameter parameterToDelete = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("temporary")
                .withUniqueParameterValue("tempvalue")
                .asActive()
                .buildAndValidate()
        Parameter savedParameter = parameterRepository.save(parameterToDelete)

        when:
        parameterRepository.delete(savedParameter)
        Optional<Parameter> deletedParameter = parameterRepository.findByOwnerAndParameterName(testOwner,savedParameter.parameterName)

        then:
        !deletedParameter.isPresent()

        when:
        Optional<Parameter> deletedById = parameterRepository.findByOwnerAndParameterId(testOwner,savedParameter.parameterId)

        then:
        !deletedById.isPresent()
    }

    void 'test smart builder auto-fixes constraint violations'() {
        given: "SmartParameterBuilder auto-fixes invalid length constraints"
        // The SmartParameterBuilder is designed to auto-fix constraint violations
        // rather than throw exceptions, which ensures valid test data creation

        when: "creating a parameter with potentially invalid name length"
        Parameter parameter1 = SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName("a" * 51)  // Would be too long, but auto-fixed
                .withUniqueParameterValue("valid")
                .buildAndValidate()

        then: "the name is automatically truncated to valid length"
        parameter1.parameterName.length() <= 50
        parameter1.parameterName.length() >= 1

        when: "creating a parameter with potentially invalid value length"
        Parameter parameter2 = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("valid")
                .withParameterValue("b" * 51)  // Would be too long, but auto-fixed
                .buildAndValidate()

        then: "the value is automatically truncated to valid length"
        parameter2.parameterValue.length() <= 50
        parameter2.parameterValue.length() >= 1

        when: "saving these auto-fixed parameters"
        Parameter saved1 = parameterRepository.save(parameter1)
        Parameter saved2 = parameterRepository.save(parameter2)

        then: "they save successfully due to constraint compliance"
        saved1.parameterId != null
        saved2.parameterId != null
    }

    void 'test parameter with maximum allowed lengths'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withParameterName("a" * 50)  // Maximum allowed length (50 chars)
                .withParameterValue("b" * 50)  // Maximum allowed length (50 chars)
                .asActive()
                .buildAndValidate()

        when:
        Parameter savedParameter = parameterRepository.save(parameter)

        then:
        savedParameter.parameterId != null
        savedParameter.parameterName.length() == 50
        savedParameter.parameterValue.length() == 50
    }

    void 'test parameter active status functionality'() {
        given:
        Parameter activeParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("active")
                .withUniqueParameterValue("running")
                .asActive()
                .buildAndValidate()

        Parameter inactiveParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("inactive")
                .withUniqueParameterValue("stopped")
                .asInactive()
                .buildAndValidate()

        when:
        Parameter savedActive = parameterRepository.save(activeParameter)
        Parameter savedInactive = parameterRepository.save(inactiveParameter)

        then:
        savedActive.activeStatus == true
        savedInactive.activeStatus == false

        when:
        List<Parameter> allParameters = parameterRepository.findAll()

        then:
        allParameters.any { it.parameterId == savedActive.parameterId && it.activeStatus == true }
        allParameters.any { it.parameterId == savedInactive.parameterId && it.activeStatus == false }
    }

    void 'test find non-existent parameter'() {
        when:
        Optional<Parameter> nonExistentByName = parameterRepository.findByOwnerAndParameterName(testOwner,"nonexistent_${testOwner}")
        Optional<Parameter> nonExistentById = parameterRepository.findByOwnerAndParameterId(testOwner,-999L)

        then:
        !nonExistentByName.isPresent()
        !nonExistentById.isPresent()
    }

    void 'test parameter entity persistence validation'() {
        given:
        Parameter parameter = SmartParameterBuilder.builderForOwner(testOwner)
                .withUniqueParameterName("persistence")
                .withUniqueParameterValue("test")
                .asActive()
                .buildAndValidate()

        when:
        Parameter savedParameter = parameterRepository.save(parameter)

        then:
        savedParameter.parameterId != null
        savedParameter.parameterName != null
        savedParameter.parameterName.length() >= 1
        savedParameter.parameterName.length() <= 50
        savedParameter.parameterValue != null
        savedParameter.parameterValue.length() >= 1
        savedParameter.parameterValue.length() <= 50
        savedParameter.activeStatus == true
        savedParameter.dateAdded != null
        savedParameter.dateUpdated != null

        when:
        Optional<Parameter> refetchedOpt = parameterRepository.findById(savedParameter.parameterId)

        then:
        refetchedOpt.isPresent()
        def refetchedParameter = refetchedOpt.get()
        refetchedParameter.parameterName == savedParameter.parameterName
        refetchedParameter.parameterValue == savedParameter.parameterValue
        refetchedParameter.activeStatus == savedParameter.activeStatus
        refetchedParameter.dateAdded != null
        refetchedParameter.dateUpdated != null
    }

    void 'test smart builder convenience methods'() {
        given:
        Parameter paymentParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .asPaymentAccountParameter()
                .asActive()
                .buildAndValidate()

        Parameter configParameter = SmartParameterBuilder.builderForOwner(testOwner)
                .asConfigParameter("security")
                .asInactive()
                .buildAndValidate()

        when:
        Parameter savedPayment = parameterRepository.save(paymentParameter)
        Parameter savedConfig = parameterRepository.save(configParameter)

        then:
        savedPayment.parameterName.contains("payment_account")
        savedPayment.parameterValue.contains("bank")
        savedPayment.activeStatus == true

        savedConfig.parameterName.contains("security_config")
        savedConfig.parameterValue.contains("security_value")
        savedConfig.activeStatus == false

        // Parameter names and values follow lowercase conversion
        savedPayment.parameterName == savedPayment.parameterName.toLowerCase()
        savedConfig.parameterName == savedConfig.parameterName.toLowerCase()
        savedPayment.parameterValue == savedPayment.parameterValue.toLowerCase()
        savedConfig.parameterValue == savedConfig.parameterValue.toLowerCase()

        // Verify constraint compliance
        savedPayment.parameterName.length() >= 1
        savedPayment.parameterName.length() <= 50
        savedConfig.parameterName.length() >= 1
        savedConfig.parameterName.length() <= 50
    }
}