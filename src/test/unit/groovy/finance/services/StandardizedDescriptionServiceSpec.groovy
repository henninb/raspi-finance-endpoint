package finance.services

import finance.domain.Description
import finance.domain.ServiceResult
import finance.helpers.DescriptionBuilder
import finance.repositories.DescriptionRepository
import finance.repositories.TransactionRepository
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import jakarta.persistence.EntityNotFoundException

/**
 * TDD Specification for StandardizedDescriptionService
 * Tests the Description service using new ServiceResult pattern with comprehensive error handling
 */
class StandardizedDescriptionServiceSpec extends BaseServiceSpec {

    def descriptionRepositoryMock = Mock(DescriptionRepository)
    def transactionRepositoryMock = Mock(TransactionRepository)
    def standardizedDescriptionService = new StandardizedDescriptionService(descriptionRepositoryMock, transactionRepositoryMock)

    void setup() {
        standardizedDescriptionService.meterService = meterService
        standardizedDescriptionService.validator = validatorMock
    }

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return Success with descriptions when found"() {
        given: "existing active descriptions"
        def descriptions = [
            DescriptionBuilder.builder().withDescriptionName("groceries").build(),
            DescriptionBuilder.builder().withDescriptionName("utilities").build()
        ]

        when: "finding all active descriptions"
        def result = standardizedDescriptionService.findAllActive()

        then: "should return Success with descriptions"
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> descriptions
        1 * transactionRepositoryMock.countByDescriptionName("groceries") >> 5L
        1 * transactionRepositoryMock.countByDescriptionName("utilities") >> 3L
        result instanceof ServiceResult.Success
        result.data.size() == 2
        result.data[0].descriptionName == "groceries"
        result.data[0].descriptionCount == 5L
        result.data[1].descriptionName == "utilities"
        result.data[1].descriptionCount == 3L
        0 * _
    }

    def "findAllActive should return Success with empty list when no descriptions found"() {
        when: "finding all active descriptions with none existing"
        def result = standardizedDescriptionService.findAllActive()

        then: "should return Success with empty list"
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> []
        result instanceof ServiceResult.Success
        result.data.isEmpty()
        0 * _
    }

    // ===== TDD Tests for findById() =====

    def "findById should return Success with description when found"() {
        given: "existing description"
        def description = DescriptionBuilder.builder().withDescriptionId(1L).build()

        when: "finding by valid ID"
        def result = standardizedDescriptionService.findById(1L)

        then: "should return Success with description"
        1 * descriptionRepositoryMock.findByDescriptionId(1L) >> Optional.of(description)
        result instanceof ServiceResult.Success
        result.data.descriptionId == 1L
        0 * _
    }

    def "findById should return NotFound when description does not exist"() {
        when: "finding by non-existent ID"
        def result = standardizedDescriptionService.findById(999L)

        then: "should return NotFound result"
        1 * descriptionRepositoryMock.findByDescriptionId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Description not found: 999")
        0 * _
    }

    // ===== TDD Tests for save() =====

    def "save should return Success with saved description when valid"() {
        given: "valid description"
        def description = DescriptionBuilder.builder().build()
        def savedDescription = DescriptionBuilder.builder().withDescriptionId(1L).build()
        Set<ConstraintViolation<Description>> noViolations = [] as Set

        when: "saving description"
        def result = standardizedDescriptionService.save(description)

        then: "should return Success with saved description"
        1 * validatorMock.validate(description) >> noViolations
        1 * descriptionRepositoryMock.saveAndFlush(description) >> savedDescription
        result instanceof ServiceResult.Success
        result.data.descriptionId == 1L
        0 * _
    }

    def "save should return ValidationError when description has constraint violations"() {
        given: "invalid description"
        def description = DescriptionBuilder.builder().withDescriptionName("").build()
        ConstraintViolation<Description> violation = Mock(ConstraintViolation)
        def mockPath = Mock(javax.validation.Path)
        mockPath.toString() >> "descriptionName"
        violation.propertyPath >> mockPath
        violation.message >> "size must be between 1 and 50"
        Set<ConstraintViolation<Description>> violations = [violation] as Set

        when: "saving invalid description"
        def result = standardizedDescriptionService.save(description)

        then: "should return ValidationError result"
        1 * validatorMock.validate(description) >> { throw new ConstraintViolationException("Validation failed", violations) }
        result instanceof ServiceResult.ValidationError
        result.errors.size() == 1
        result.errors.values().contains("size must be between 1 and 50")
    }

    def "save should return BusinessError when duplicate description exists"() {
        given: "description that will cause duplicate key violation"
        def description = DescriptionBuilder.builder().withDescriptionName("duplicate").build()
        Set<ConstraintViolation<Description>> noViolations = [] as Set

        when: "saving duplicate description"
        def result = standardizedDescriptionService.save(description)

        then: "should return BusinessError result"
        1 * validatorMock.validate(description) >> noViolations
        1 * descriptionRepositoryMock.saveAndFlush(description) >> {
            throw new DataIntegrityViolationException("Duplicate entry")
        }
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
        0 * _
    }

    // ===== TDD Tests for update() =====

    def "update should return Success with updated description when exists"() {
        given: "existing description to update"
        def existingDescription = DescriptionBuilder.builder().withDescriptionId(1L).withDescriptionName("old").build()
        def updatedDescription = DescriptionBuilder.builder().withDescriptionId(1L).withDescriptionName("new").build()

        when: "updating existing description"
        def result = standardizedDescriptionService.update(updatedDescription)

        then: "should return Success with updated description"
        1 * descriptionRepositoryMock.findByDescriptionId(1L) >> Optional.of(existingDescription)
        1 * descriptionRepositoryMock.saveAndFlush(_ as Description) >> { Description desc ->
            assert desc.descriptionName == "new"
            return desc
        }
        result instanceof ServiceResult.Success
        result.data.descriptionName == "new"
        0 * _
    }

    def "update should return NotFound when description does not exist"() {
        given: "description with non-existent ID"
        def description = DescriptionBuilder.builder().withDescriptionId(999L).build()

        when: "updating non-existent description"
        def result = standardizedDescriptionService.update(description)

        then: "should return NotFound result"
        1 * descriptionRepositoryMock.findByDescriptionId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Description not found: 999")
        0 * _
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should return Success when description exists"() {
        given: "existing description"
        def description = DescriptionBuilder.builder().withDescriptionId(1L).build()

        when: "deleting existing description"
        def result = standardizedDescriptionService.deleteById(1L)

        then: "should return Success"
        1 * descriptionRepositoryMock.findByDescriptionId(1L) >> Optional.of(description)
        1 * descriptionRepositoryMock.delete(description)
        result instanceof ServiceResult.Success
        result.data == true
        0 * _
    }

    def "deleteById should return NotFound when description does not exist"() {
        when: "deleting non-existent description"
        def result = standardizedDescriptionService.deleteById(999L)

        then: "should return NotFound result"
        1 * descriptionRepositoryMock.findByDescriptionId(999L) >> Optional.empty()
        result instanceof ServiceResult.NotFound
        result.message.contains("Description not found: 999")
        0 * _
    }

    // ===== TDD Tests for Legacy Method Support =====

    def "fetchAllDescriptions should delegate to findAllActive and return data"() {
        given: "existing descriptions"
        def descriptions = [DescriptionBuilder.builder().build()]

        when: "calling legacy fetchAllDescriptions method"
        def result = standardizedDescriptionService.fetchAllDescriptions()

        then: "should return description list"
        1 * descriptionRepositoryMock.findByActiveStatusOrderByDescriptionName(true) >> descriptions
        1 * transactionRepositoryMock.countByDescriptionName("foo") >> 2L
        result.size() == 1
        result[0].descriptionCount == 2L
        0 * _
    }

    def "insertDescription should delegate to save and return data"() {
        given: "valid description"
        def description = DescriptionBuilder.builder().build()
        def savedDescription = DescriptionBuilder.builder().withDescriptionId(1L).build()
        Set<ConstraintViolation<Description>> noViolations = [] as Set

        when: "calling legacy insertDescription method"
        def result = standardizedDescriptionService.insertDescription(description)

        then: "should return saved description"
        1 * validatorMock.validate(description) >> noViolations
        1 * descriptionRepositoryMock.saveAndFlush(description) >> savedDescription
        result.descriptionId == 1L
        0 * _
    }

    def "updateDescription should delegate to update and return data"() {
        given: "existing description to update"
        def existingDescription = DescriptionBuilder.builder().withDescriptionId(1L).withDescriptionName("old").build()
        def updatedDescription = DescriptionBuilder.builder().withDescriptionId(1L).withDescriptionName("new").build()

        when: "calling legacy updateDescription method"
        def result = standardizedDescriptionService.updateDescription(updatedDescription)

        then: "should return updated description"
        1 * descriptionRepositoryMock.findByDescriptionId(1L) >> Optional.of(existingDescription)
        1 * descriptionRepositoryMock.saveAndFlush(_ as Description) >> { Description desc -> return desc }
        result.descriptionName == "new"
        0 * _
    }

    def "findByDescriptionName should return description when found"() {
        given: "existing description"
        def description = DescriptionBuilder.builder().withDescriptionName("test").build()

        when: "finding by description name"
        def result = standardizedDescriptionService.findByDescriptionName("test")

        then: "should return description optional"
        1 * descriptionRepositoryMock.findByDescriptionName("test") >> Optional.of(description)
        result.isPresent()
        result.get().descriptionName == "test"
        0 * _
    }

    def "description should delegate to findByDescriptionName"() {
        given: "existing description"
        def description = DescriptionBuilder.builder().withDescriptionName("test").build()

        when: "calling legacy description method"
        def result = standardizedDescriptionService.description("test")

        then: "should return description optional"
        1 * descriptionRepositoryMock.findByDescriptionName("test") >> Optional.of(description)
        result.isPresent()
        result.get().descriptionName == "test"
        0 * _
    }

    def "deleteByDescriptionName should return true when description exists"() {
        given: "existing description"
        def description = DescriptionBuilder.builder().withDescriptionName("test").build()

        when: "deleting by description name"
        def result = standardizedDescriptionService.deleteByDescriptionName("test")

        then: "should return true"
        1 * descriptionRepositoryMock.findByDescriptionName("test") >> Optional.of(description)
        1 * descriptionRepositoryMock.delete(description)
        result == true
        0 * _
    }

    def "deleteByDescriptionName should return false when description does not exist"() {
        when: "deleting non-existent description"
        def result = standardizedDescriptionService.deleteByDescriptionName("missing")

        then: "should return false"
        1 * descriptionRepositoryMock.findByDescriptionName("missing") >> Optional.empty()
        result == false
        0 * _
    }

    // ===== TDD Tests for mergeDescriptions() =====

    def "mergeDescriptions should successfully merge multiple source descriptions into target"() {
        given: "target description and source descriptions"
        def targetDescription = DescriptionBuilder.builder().withDescriptionName("target").withDescriptionCount(10L).build()
        def sourceDescription1 = DescriptionBuilder.builder().withDescriptionName("source1").withDescriptionCount(5L).build()
        def sourceDescription2 = DescriptionBuilder.builder().withDescriptionName("source2").withDescriptionCount(3L).build()
        def sourceNames = ["source1", "source2"]
        def transactions1 = [] // Empty list for simplicity
        def transactions2 = [] // Empty list for simplicity

        when: "merging descriptions"
        def result = standardizedDescriptionService.mergeDescriptions("target", sourceNames)

        then: "should merge successfully"
        1 * descriptionRepositoryMock.findByDescriptionName("target") >> Optional.of(targetDescription)
        1 * descriptionRepositoryMock.findByDescriptionName("source1") >> Optional.of(sourceDescription1)
        1 * descriptionRepositoryMock.findByDescriptionName("source2") >> Optional.of(sourceDescription2)
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc("source1", true) >> transactions1
        1 * transactionRepositoryMock.findByDescriptionAndActiveStatusOrderByTransactionDateDesc("source2", true) >> transactions2
        1 * descriptionRepositoryMock.saveAndFlush(sourceDescription1) >> sourceDescription1
        1 * descriptionRepositoryMock.saveAndFlush(sourceDescription2) >> sourceDescription2
        1 * descriptionRepositoryMock.saveAndFlush(targetDescription) >> targetDescription
        result.descriptionName == "target"
        result.descriptionCount == 18L // 10 + 5 + 3
        !sourceDescription1.activeStatus // source1 should be deactivated
        !sourceDescription2.activeStatus // source2 should be deactivated
        0 * _
    }

    // ===== TDD Tests for Error Handling in Legacy Methods =====

    def "insertDescription should throw ValidationException for invalid description"() {
        given: "invalid description"
        def description = DescriptionBuilder.builder().withDescriptionName("").build()
        ConstraintViolation<Description> violation = Mock(ConstraintViolation)
        violation.invalidValue >> ""
        violation.message >> "size must be between 1 and 50"
        Set<ConstraintViolation<Description>> violations = [violation] as Set

        when: "calling legacy insertDescription with invalid data"
        standardizedDescriptionService.insertDescription(description)

        then: "should throw ValidationException"
        1 * validatorMock.validate(description) >> { throw new ConstraintViolationException("Validation failed", violations) }
        thrown(jakarta.validation.ValidationException)
    }

    def "updateDescription should throw RuntimeException when description not found"() {
        given: "description with non-existent ID"
        def description = DescriptionBuilder.builder().withDescriptionId(999L).build()

        when: "calling legacy updateDescription with non-existent description"
        standardizedDescriptionService.updateDescription(description)

        then: "should throw RuntimeException"
        1 * descriptionRepositoryMock.findByDescriptionId(999L) >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }

    def "mergeDescriptions should throw RuntimeException when target description not found"() {
        when: "merging with non-existent target description"
        standardizedDescriptionService.mergeDescriptions("missingTarget", ["source1"])

        then: "should throw RuntimeException"
        1 * descriptionRepositoryMock.findByDescriptionName("missingtarget") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }

    def "mergeDescriptions should throw RuntimeException when source description not found"() {
        given: "existing target description"
        def targetDescription = DescriptionBuilder.builder().withDescriptionName("target").build()

        when: "merging with non-existent source description"
        standardizedDescriptionService.mergeDescriptions("target", ["missingSource"])

        then: "should throw RuntimeException"
        1 * descriptionRepositoryMock.findByDescriptionName("target") >> Optional.of(targetDescription)
        1 * descriptionRepositoryMock.findByDescriptionName("missingSource") >> Optional.empty()
        thrown(RuntimeException)
        0 * _
    }
}