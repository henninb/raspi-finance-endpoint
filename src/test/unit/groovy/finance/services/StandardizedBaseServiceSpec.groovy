package finance.services

import finance.domain.ServiceResult
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

/**
 * TDD Specification for StandardizedBaseService
 * Tests the abstract base class with standardized error handling patterns
 */
class StandardizedBaseServiceSpec extends Specification {

    // Mock entity for testing
    static class TestEntity {
        Long id
        String name
        boolean active = true
    }

    // Concrete implementation for testing
    static class TestStandardizedService extends StandardizedBaseService<TestEntity, Long> {
        private Map<Long, TestEntity> mockData = [:]
        private Long nextId = 1L

        @Override
        protected String getEntityName() {
            return "TestEntity"
        }

        @Override
        ServiceResult<List<TestEntity>> findAllActive() {
            return handleServiceOperation("Find all active", null) {
                mockData.values().findAll { it.active }
            }
        }

        @Override
        ServiceResult<TestEntity> findById(Long id) {
            return handleServiceOperation("Find by ID", id) {
                TestEntity entity = mockData.get(id)
                if (entity == null) {
                    throw new EntityNotFoundException("TestEntity not found: $id")
                }
                entity
            }
        }

        @Override
        ServiceResult<TestEntity> save(TestEntity entity) {
            return handleServiceOperation("Save", entity.id) {
                if (entity.name?.trim()?.isEmpty()) {
                    // Simulate constraint violation
                    Set<ConstraintViolation<TestEntity>> violations = [
                        [getMessage: { "Name cannot be empty" }, getInvalidValue: { entity.name }] as ConstraintViolation<TestEntity>
                    ] as Set<ConstraintViolation<TestEntity>>
                    throw new ConstraintViolationException("Validation failed", violations)
                }
                if (entity.id == null) {
                    entity.id = nextId++
                }
                mockData.put(entity.id, entity)
                entity
            }
        }

        @Override
        ServiceResult<TestEntity> update(TestEntity entity) {
            return handleServiceOperation("Update", entity.id) {
                if (entity.id == null || !mockData.containsKey(entity.id)) {
                    throw new EntityNotFoundException("TestEntity not found for update: ${entity.id}")
                }
                mockData.put(entity.id, entity)
                entity
            }
        }

        @Override
        ServiceResult<Boolean> deleteById(Long id) {
            return handleServiceOperation("Delete", id) {
                if (!mockData.containsKey(id)) {
                    throw new EntityNotFoundException("TestEntity not found for deletion: $id")
                }
                if (id == 999L) {
                    // Simulate data integrity violation
                    throw new DataIntegrityViolationException("Cannot delete entity with dependencies")
                }
                mockData.remove(id)
                true
            }
        }

        // Test method to simulate system errors
        ServiceResult<String> simulateSystemError() {
            return handleServiceOperation("System error test", null) {
                throw new RuntimeException("Database connection failed")
            }
        }

        // Test method to simulate business errors
        ServiceResult<String> simulateBusinessError() {
            return handleServiceOperation("Business error test", null) {
                throw new IllegalStateException("Invalid business state")
            }
        }

        // Expose the protected method for testing
        ServiceResult<String> testHandleServiceOperation(String operation, Long entityId, Closure<String> block) {
            return handleServiceOperation(operation, entityId, block)
        }
    }

    def testService = new TestStandardizedService()

    def setup() {
        // Set up test service with proper dependencies (following BaseServiceSpec pattern)
        testService.meterService = new MeterService(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        testService.validator = GroovyMock(jakarta.validation.Validator)
    }

    // ===== TDD Tests for handleServiceOperation() Success Cases =====

    def "handleServiceOperation should return Success for successful operation"() {
        when: "executing successful operation"
        def result = testService.testHandleServiceOperation("Test operation", 1L) {
            "Success result"
        }

        then: "should return Success result"
        result instanceof ServiceResult.Success
        result.data == "Success result"
    }

    def "handleServiceOperation should handle null entity ID"() {
        when: "executing operation with null entity ID"
        def result = testService.testHandleServiceOperation("Test operation", null) {
            "Success with null ID"
        }

        then: "should return Success result"
        result instanceof ServiceResult.Success
        result.data == "Success with null ID"
    }

    // ===== TDD Tests for handleServiceOperation() Error Handling =====

    def "handleServiceOperation should convert EntityNotFoundException to NotFound"() {
        when: "operation throws EntityNotFoundException"
        def result = testService.findById(999L)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message.contains("TestEntity not found: 999")
    }

    def "handleServiceOperation should convert ConstraintViolationException to ValidationError"() {
        given: "entity with constraint violation"
        def entity = new TestEntity(name: "")

        when: "saving entity with validation errors"
        def result = testService.save(entity)

        then: "should return ValidationError result"
        result instanceof ServiceResult.ValidationError
        !result.errors.isEmpty()
    }

    def "handleServiceOperation should convert DataIntegrityViolationException to BusinessError"() {
        given: "existing entity that will cause integrity violation"
        def entity = new TestEntity(id: 999L, name: "Test")
        testService.save(entity) // First save the entity so it exists

        when: "attempting to delete entity with dependencies"
        def result = testService.deleteById(999L) // Special ID that triggers DataIntegrityViolationException

        then: "should return BusinessError result"
        result instanceof ServiceResult.BusinessError
        result.message.toLowerCase().contains("data integrity")
        result.errorCode == "DATA_INTEGRITY_VIOLATION"
    }

    def "handleServiceOperation should convert RuntimeException to SystemError"() {
        when: "operation throws system exception"
        def result = testService.simulateSystemError()

        then: "should return SystemError result"
        result instanceof ServiceResult.SystemError
        result.exception instanceof RuntimeException
        result.exception.message == "Database connection failed"
    }

    def "handleServiceOperation should convert other exceptions to BusinessError"() {
        when: "operation throws business logic exception"
        def result = testService.simulateBusinessError()

        then: "should return BusinessError result"
        result instanceof ServiceResult.BusinessError
        result.message.contains("business")
        result.errorCode == "BUSINESS_LOGIC_ERROR"
    }

    // ===== TDD Tests for CRUD Operations with Error Handling =====

    def "findAllActive should handle successful case"() {
        given: "some test entities"
        testService.save(new TestEntity(name: "Active Entity", active: true))
        testService.save(new TestEntity(name: "Inactive Entity", active: false))

        when: "finding all active entities"
        def result = testService.findAllActive()

        then: "should return Success with filtered entities"
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].name == "Active Entity"
    }

    def "save should handle successful creation"() {
        given: "valid entity"
        def entity = new TestEntity(name: "Valid Entity")

        when: "saving entity"
        def result = testService.save(entity)

        then: "should return Success with saved entity"
        result instanceof ServiceResult.Success
        result.data.id != null
        result.data.name == "Valid Entity"
    }

    def "update should handle entity not found"() {
        given: "entity with non-existent ID"
        def entity = new TestEntity(id: 999L, name: "Non-existent")

        when: "attempting to update"
        def result = testService.update(entity)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message.contains("TestEntity not found: 999")
    }

    def "deleteById should handle successful deletion"() {
        given: "existing entity"
        def entity = new TestEntity(name: "To Delete")
        def saveResult = testService.save(entity)
        def entityId = saveResult.data.id

        when: "deleting by ID"
        def result = testService.deleteById(entityId)

        then: "should return Success"
        result instanceof ServiceResult.Success
        result.data == true
    }

    // ===== TDD Tests for getEntityName() Abstract Method =====

    def "getEntityName should return correct entity name"() {
        expect: "entity name matches expected value"
        testService.getEntityName() == "TestEntity"
    }

    // ===== TDD Tests for Base Service Integration =====

    def "service should extend BaseService"() {
        expect: "service has BaseService functionality"
        testService instanceof BaseService
        testService.respondsTo("executeWithResilienceSync")
        testService.respondsTo("handleConstraintViolations")
    }

    def "service should implement StandardServiceInterface"() {
        expect: "service implements all required methods"
        testService instanceof StandardServiceInterface
        testService.respondsTo("findAllActive")
        testService.respondsTo("findById", Long)
        testService.respondsTo("save", TestEntity)
        testService.respondsTo("update", TestEntity)
        testService.respondsTo("deleteById", Long)
    }

    // ===== TDD Tests for Logging and Metrics Integration =====

    def "handleServiceOperation should integrate with logging and metrics"() {
        when: "executing operation"
        def result = testService.testHandleServiceOperation("Test logging", 1L) {
            "Success"
        }

        then: "should complete successfully"
        result instanceof ServiceResult.Success
        // Note: Actual logging verification would require spy/mock setup
        // This test ensures the operation completes without logging errors
    }
}