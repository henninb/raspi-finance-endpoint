package finance.services

import finance.domain.ServiceResult
import spock.lang.Specification

/**
 * TDD Specification for StandardServiceInterface
 * Tests the generic CRUD operations interface for standardized service patterns
 */
class StandardServiceInterfaceSpec extends Specification {

    // Mock entity for testing
    static class TestEntity {
        Long id
        String name
        boolean active = true
    }

    // Concrete implementation for testing
    static class TestStandardService implements StandardServiceInterface<TestEntity, Long> {
        private Map<Long, TestEntity> mockData = [:]
        private Long nextId = 1L

        @Override
        ServiceResult<List<TestEntity>> findAllActive() {
            return ServiceResult.Success.of(mockData.values().findAll { it.active })
        }

        @Override
        ServiceResult<TestEntity> findById(Long id) {
            TestEntity entity = mockData.get(id)
            if (entity == null) {
                return ServiceResult.NotFound.of("TestEntity not found: $id")
            }
            return ServiceResult.Success.of(entity)
        }

        @Override
        ServiceResult<TestEntity> save(TestEntity entity) {
            if (entity.id == null) {
                entity.id = nextId++
            }
            mockData.put(entity.id, entity)
            return ServiceResult.Success.of(entity)
        }

        @Override
        ServiceResult<TestEntity> update(TestEntity entity) {
            if (entity.id == null || !mockData.containsKey(entity.id)) {
                return ServiceResult.NotFound.of("TestEntity not found for update: ${entity.id}")
            }
            mockData.put(entity.id, entity)
            return ServiceResult.Success.of(entity)
        }

        @Override
        ServiceResult<Boolean> deleteById(Long id) {
            if (!mockData.containsKey(id)) {
                return ServiceResult.NotFound.of("TestEntity not found for deletion: $id")
            }
            mockData.remove(id)
            return ServiceResult.Success.of(true)
        }
    }

    def testService = new TestStandardService()

    // ===== TDD Tests for findAllActive() =====

    def "findAllActive should return empty list when no entities exist"() {
        when: "calling findAllActive on empty service"
        def result = testService.findAllActive()

        then: "should return successful result with empty list"
        result instanceof ServiceResult.Success
        result.data.isEmpty()
    }

    def "findAllActive should return only active entities"() {
        given: "entities with mixed active status"
        def activeEntity = new TestEntity(name: "Active Entity", active: true)
        def inactiveEntity = new TestEntity(name: "Inactive Entity", active: false)
        testService.save(activeEntity)
        testService.save(inactiveEntity)

        when: "calling findAllActive"
        def result = testService.findAllActive()

        then: "should return only active entities"
        result instanceof ServiceResult.Success
        result.data.size() == 1
        result.data[0].name == "Active Entity"
        result.data[0].active
    }

    // ===== TDD Tests for findById() =====

    def "findById should return entity when found"() {
        given: "an existing entity"
        def entity = new TestEntity(name: "Test Entity")
        def saveResult = testService.save(entity)
        def savedId = saveResult.data.id

        when: "finding by valid ID"
        def result = testService.findById(savedId)

        then: "should return successful result with entity"
        result instanceof ServiceResult.Success
        result.data.name == "Test Entity"
        result.data.id == savedId
    }

    def "findById should return NotFound when entity does not exist"() {
        when: "finding by non-existent ID"
        def result = testService.findById(999L)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message == "TestEntity not found: 999"
    }

    // ===== TDD Tests for save() =====

    def "save should create new entity with generated ID"() {
        given: "a new entity without ID"
        def entity = new TestEntity(name: "New Entity")

        when: "saving the entity"
        def result = testService.save(entity)

        then: "should return successful result with generated ID"
        result instanceof ServiceResult.Success
        result.data.id != null
        result.data.name == "New Entity"
    }

    def "save should preserve entity with existing ID"() {
        given: "an entity with existing ID"
        def entity = new TestEntity(id: 100L, name: "Existing Entity")

        when: "saving the entity"
        def result = testService.save(entity)

        then: "should return successful result with same ID"
        result instanceof ServiceResult.Success
        result.data.id == 100L
        result.data.name == "Existing Entity"
    }

    // ===== TDD Tests for update() =====

    def "update should modify existing entity"() {
        given: "an existing entity"
        def entity = new TestEntity(name: "Original Name")
        def saveResult = testService.save(entity)
        def savedEntity = saveResult.data

        when: "updating the entity"
        savedEntity.name = "Updated Name"
        def result = testService.update(savedEntity)

        then: "should return successful result with updated data"
        result instanceof ServiceResult.Success
        result.data.name == "Updated Name"
        result.data.id == savedEntity.id
    }

    def "update should return NotFound for non-existent entity"() {
        given: "an entity with non-existent ID"
        def entity = new TestEntity(id: 999L, name: "Non-existent")

        when: "attempting to update"
        def result = testService.update(entity)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message == "TestEntity not found for update: 999"
    }

    def "update should return NotFound for entity without ID"() {
        given: "an entity without ID"
        def entity = new TestEntity(name: "No ID")

        when: "attempting to update"
        def result = testService.update(entity)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message == "TestEntity not found for update: null"
    }

    // ===== TDD Tests for deleteById() =====

    def "deleteById should remove existing entity"() {
        given: "an existing entity"
        def entity = new TestEntity(name: "To Delete")
        def saveResult = testService.save(entity)
        def savedId = saveResult.data.id

        when: "deleting by ID"
        def result = testService.deleteById(savedId)

        then: "should return successful result"
        result instanceof ServiceResult.Success
        result.data == true

        and: "entity should no longer exist"
        def findResult = testService.findById(savedId)
        findResult instanceof ServiceResult.NotFound
    }

    def "deleteById should return NotFound for non-existent entity"() {
        when: "attempting to delete non-existent entity"
        def result = testService.deleteById(999L)

        then: "should return NotFound result"
        result instanceof ServiceResult.NotFound
        result.message == "TestEntity not found for deletion: 999"
    }

    // ===== TDD Tests for Interface Contract =====

    def "service should implement all required CRUD operations"() {
        expect: "all interface methods are implemented"
        testService.respondsTo("findAllActive")
        testService.respondsTo("findById", Long)
        testService.respondsTo("save", TestEntity)
        testService.respondsTo("update", TestEntity)
        testService.respondsTo("deleteById", Long)
    }

    def "all operations should return ServiceResult types"() {
        given: "a test entity"
        def entity = new TestEntity(name: "Test")
        def saveResult = testService.save(entity)
        def entityId = saveResult.data.id

        expect: "all operations return ServiceResult"
        testService.findAllActive() instanceof ServiceResult
        testService.findById(entityId) instanceof ServiceResult
        testService.save(entity) instanceof ServiceResult
        testService.update(entity) instanceof ServiceResult
        testService.deleteById(entityId) instanceof ServiceResult
    }
}