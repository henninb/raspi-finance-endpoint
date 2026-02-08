package finance.controllers

import finance.domain.Category
import finance.services.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedCategoryControllerSpec extends Specification {

    finance.repositories.CategoryRepository categoryRepository = Mock()
    finance.repositories.TransactionRepository transactionRepository = Mock()
    CategoryService categoryService = new CategoryService(categoryRepository, transactionRepository)

    @Subject
    CategoryController controller = new CategoryController(categoryService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        categoryService.validator = validator
        categoryService.meterService = meterService
    }

    private static Category cat(Long id = 0L, String name = "cat1", boolean active = true) {
        new Category(categoryId: id, owner: "test_owner", categoryName: name, activeStatus: active)
    }

    // ===== STANDARDIZED: findAllActive =====
    def "findAllActive returns list with counts"() {
        given:
        Category c1 = cat(1L, "groceries")
        Category c2 = cat(2L, "utilities")
        and:
        categoryRepository.findByActiveStatusOrderByCategoryName(true) >> [c1, c2]
        transactionRepository.countByCategoryNameIn(["groceries", "utilities"]) >> [
            ["groceries", 3L] as Object[],
            ["utilities", 1L] as Object[]
        ]

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        List<Category> body = response.body as List<Category>
        body.size() == 2
        body[0].categoryCount == 3L || body[1].categoryCount == 3L
    }

    def "findAllActive returns 404 when service returns NotFound"() {
        given:
        // Force service to map to NotFound by throwing JPA EntityNotFoundException
        categoryRepository.findByActiveStatusOrderByCategoryName(true) >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        // After standardization: empty body with status code only
        response.body == null
    }

    def "findAllActive returns 500 on system error"() {
        given:
        categoryRepository.findByActiveStatusOrderByCategoryName(true) >> { throw new RuntimeException("db error") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: findById =====
    def "findById returns category when found"() {
        given:
        Category c = cat(10L, "groceries")
        and:
        categoryRepository.findByCategoryName("groceries") >> Optional.of(c)
        transactionRepository.countByCategoryName("groceries") >> 5L

        when:
        ResponseEntity<?> response = controller.findById("groceries")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Category).categoryId == 10L
        (response.body as Category).categoryCount == 5L
    }

    def "findById returns 404 when missing"() {
        given:
        categoryRepository.findByCategoryName("missing") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.findById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        // After standardization: empty body with status code only
        response.body == null
    }

    def "findById returns 500 on system error"() {
        given:
        categoryRepository.findByCategoryName("boom") >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<?> response = controller.findById("boom")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: save =====
    def "save creates category returns 201"() {
        given:
        Category input = cat(0L, "newcat")
        and:
        categoryRepository.saveAndFlush(_ as Category) >> { Category c -> c.categoryId = 100L; return c }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        (response.body as Category).categoryId == 100L
    }

    def "save returns 400 on validation error"() {
        given:
        Category invalid = cat(0L, "")
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        categoryService.validator = violatingValidator

        when:
        ResponseEntity<?> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        // After standardization: empty body with status code only
        response.body == null
    }

    def "save returns 409 on duplicate"() {
        given:
        Category input = cat(0L, "dup")
        and:
        categoryRepository.saveAndFlush(_ as Category) >> { throw new org.springframework.dao.DataIntegrityViolationException("duplicate") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CONFLICT
        // After standardization: empty body with status code only
        response.body == null
    }

    def "save returns 500 on system error"() {
        given:
        Category input = cat(0L, "syscat")
        and:
        categoryRepository.saveAndFlush(_ as Category) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: update =====
    def "update returns 200 when exists"() {
        given:
        Category existing = cat(5L, "old")
        Category patch = cat(5L, "new")
        and:
        categoryRepository.findByCategoryId(5L) >> Optional.of(existing)
        categoryRepository.saveAndFlush(_ as Category) >> { Category c -> c }

        when:
        ResponseEntity<?> response = controller.update("new", patch)

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Category).categoryName == "new"
    }

    def "update returns 404 when missing"() {
        given:
        Category patch = cat(999L, "nope")
        and:
        categoryRepository.findByCategoryId(999L) >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.update("nope", patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        // After standardization: empty body with status code only
        response.body == null
    }

    def "update returns 500 on system error"() {
        given:
        Category existing = cat(6L, "uold")
        Category patch = cat(6L, "unew")
        and:
        categoryRepository.findByCategoryId(6L) >> Optional.of(existing)
        categoryRepository.saveAndFlush(_ as Category) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.update("unew", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== STANDARDIZED: deleteById =====
    def "deleteById returns 200 with deleted category when found"() {
        given:
        Category existing = cat(7L, "groceries")
        and:
        2 * categoryRepository.findByCategoryName("groceries") >> Optional.of(existing)

        when:
        ResponseEntity<?> response = controller.deleteById("groceries")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Category).categoryId == 7L
    }

    def "deleteById returns 404 when missing"() {
        given:
        categoryRepository.findByCategoryName("missing") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.deleteById("missing")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        // After standardization: empty body with status code only
        response.body == null
    }

    def "deleteById returns 500 on system error during find"() {
        given:
        categoryRepository.findByCategoryName("err") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 500 on system error during delete"() {
        given:
        Category existing = cat(9L, "del")
        and:
        2 * categoryRepository.findByCategoryName("del") >> Optional.of(existing)
        categoryRepository.delete(_ as Category) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("del")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== DEFENSIVE PROGRAMMING TESTS =====
    // These tests verify that our defensive else clauses handle unexpected service responses

    def "update handles null service response gracefully"() {
        given:
        Category input = cat(1L, "test")
        and:
        // Mock the service to return null (simulating unexpected behavior)
        CategoryService mockService = Mock()
        mockService.update(_ as Category) >> null
        CategoryController controllerWithMockedService = new CategoryController(mockService)

        when:
        ResponseEntity<?> response = controllerWithMockedService.update("test", input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        // After standardization: empty body with status code only (else clause handles null)
        response.body == null
    }

    // Note: Testing "unexpected result types" is complex with Spock's type system
    // The key success is that null-handling tests above prove our defensive else clauses work correctly

    def "save handles null service response gracefully"() {
        given:
        Category input = cat(0L, "test")
        and:
        CategoryService mockService = Mock()
        mockService.save(_ as Category) >> null
        CategoryController controllerWithMockedService = new CategoryController(mockService)

        when:
        ResponseEntity<?> response = controllerWithMockedService.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        // After standardization: empty body with status code only (else clause handles null)
        response.body == null
    }

    def "findById handles null service response gracefully"() {
        given:
        CategoryService mockService = Mock()
        mockService.findByCategoryNameStandardized(_ as String) >> null
        CategoryController controllerWithMockedService = new CategoryController(mockService)

        when:
        ResponseEntity<?> response = controllerWithMockedService.findById("test")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        // After standardization: empty body with status code only (else clause handles null)
        response.body == null
    }

    def "findAllActive handles null service response gracefully"() {
        given:
        CategoryService mockService = Mock()
        mockService.findAllActive() >> null
        CategoryController controllerWithMockedService = new CategoryController(mockService)

        when:
        ResponseEntity<?> response = controllerWithMockedService.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        // After standardization: empty body with status code only (else clause handles null)
        response.body == null
    }
}
