package finance.controllers

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

import java.util.Optional
import java.util.concurrent.ExecutionException

class StandardizedBaseControllerSpec extends Specification {

    // Concrete subclass that bridges Groovy closures into handleCrudOperation etc.
    static class TestController extends StandardizedBaseController {
        ResponseEntity crud(String name, Object id, Closure op) {
            handleCrudOperation(name, id, { -> op.call() } as kotlin.jvm.functions.Function0)
        }

        ResponseEntity create(String type, Object id, Closure op) {
            handleCreateOperation(type, id, { -> op.call() } as kotlin.jvm.functions.Function0)
        }

        ResponseEntity delete(String type, Object id, Closure findOp, Closure delOp) {
            handleDeleteOperation(
                type,
                id,
                { -> findOp.call() } as kotlin.jvm.functions.Function0,
                { -> delOp.call() } as kotlin.jvm.functions.Function0
            )
        }
    }

    TestController controller = new TestController()

    // ===== handleCrudOperation =====
    def "handleCrudOperation returns 200 on success"() {
        when:
        ResponseEntity resp = controller.crud("test", 1L) { "result" }

        then:
        resp.statusCode == HttpStatus.OK
        resp.body == "result"
    }

    def "handleCrudOperation throws CONFLICT on DataIntegrityViolationException"() {
        when:
        controller.crud("test", 1L) { throw new DataIntegrityViolationException("dup") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "handleCrudOperation throws BAD_REQUEST on ValidationException"() {
        when:
        controller.crud("test", 1L) { throw new ValidationException("bad") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCrudOperation throws BAD_REQUEST on IllegalArgumentException"() {
        when:
        controller.crud("test", 1L) { throw new IllegalArgumentException("bad arg") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCrudOperation throws NOT_FOUND on EntityNotFoundException"() {
        when:
        controller.crud("test", 1L) { throw new EntityNotFoundException("not found") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "handleCrudOperation throws NOT_FOUND on ExecutionException wrapping EntityNotFoundException"() {
        when:
        controller.crud("test", 1L) { throw new ExecutionException(new EntityNotFoundException("wrapped not found")) }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "handleCrudOperation throws INTERNAL_SERVER_ERROR on ExecutionException with non-EntityNotFoundException cause"() {
        when:
        controller.crud("test", 1L) { throw new ExecutionException(new RuntimeException("wrapped error")) }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "handleCrudOperation re-throws ResponseStatusException as-is"() {
        when:
        controller.crud("test", 1L) { throw new ResponseStatusException(HttpStatus.GONE, "gone") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.GONE
    }

    def "handleCrudOperation throws INTERNAL_SERVER_ERROR on generic RuntimeException"() {
        when:
        controller.crud("test", 1L) { throw new RuntimeException("unexpected") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== handleCreateOperation =====
    def "handleCreateOperation returns 201 on success"() {
        when:
        ResponseEntity resp = controller.create("Entity", 1L) { "created" }

        then:
        resp.statusCode == HttpStatus.CREATED
        resp.body == "created"
    }

    def "handleCreateOperation throws CONFLICT on DataIntegrityViolationException"() {
        when:
        controller.create("Entity", 1L) { throw new DataIntegrityViolationException("dup") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "handleCreateOperation throws BAD_REQUEST on ValidationException"() {
        when:
        controller.create("Entity", 1L) { throw new ValidationException("invalid") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCreateOperation throws BAD_REQUEST on IllegalArgumentException"() {
        when:
        controller.create("Entity", 1L) { throw new IllegalArgumentException("bad") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCreateOperation re-throws ResponseStatusException as-is"() {
        when:
        controller.create("Entity", 1L) { throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.UNPROCESSABLE_ENTITY
    }

    def "handleCreateOperation throws INTERNAL_SERVER_ERROR on generic RuntimeException"() {
        when:
        controller.create("Entity", 1L) { throw new RuntimeException("system failure") }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    // ===== handleDeleteOperation =====
    def "handleDeleteOperation returns 200 with deleted entity when found"() {
        given:
        String entity = "found_entity"

        when:
        ResponseEntity resp = controller.delete("Entity", 1L, { Optional.of(entity) }, { null })

        then:
        resp.statusCode == HttpStatus.OK
        resp.body == entity
    }

    def "handleDeleteOperation throws NOT_FOUND when entity not found"() {
        when:
        controller.delete("Entity", 99L, { Optional.empty() }, { null })

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "handleDeleteOperation throws INTERNAL_SERVER_ERROR when delete operation fails"() {
        given:
        String entity = "found_entity"

        when:
        controller.delete("Entity", 1L, { Optional.of(entity) }, { throw new RuntimeException("db error") })

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "handleDeleteOperation re-throws ResponseStatusException from find"() {
        when:
        controller.delete("Entity", 1L, { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden") }, { null })

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.FORBIDDEN
    }
}
