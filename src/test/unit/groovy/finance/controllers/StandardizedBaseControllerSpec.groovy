package finance.controllers

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

import java.util.concurrent.ExecutionException

class StandardizedBaseControllerSpec extends Specification {

    static class TestController extends StandardizedBaseController {
        // Expose protected helpers for testing via simple wrappers
        def callCrud(String op, Object id, Closure c) { handleCrudOperation(op, id) { c.call() } }
        def callCreate(String type, Object id, Closure c) { handleCreateOperation(type, id) { c.call() } }
        def callDelete(String type, Object id, Closure finder, Closure deleter) {
            handleDeleteOperation(type, id, { finder.call() }, { deleter.call() })
        }
    }

    TestController ctrl

    def setup() {
        ctrl = new TestController()
    }

    def "handleCrudOperation returns 200 on success"() {
        when:
        def resp = ctrl.callCrud('read', 123) { return 'ok' }

        then:
        resp.statusCode == HttpStatus.OK
        resp.body == 'ok'
    }

    def "handleCrudOperation maps DataIntegrityViolationException to 409"() {
        when:
        ctrl.callCrud('update', 'A1') { throw new DataIntegrityViolationException('dup') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "handleCrudOperation maps ValidationException to 400"() {
        when:
        ctrl.callCrud('create', 'A1') { throw new ValidationException('bad') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCrudOperation maps IllegalArgumentException to 400"() {
        when:
        ctrl.callCrud('create', 'A1') { throw new IllegalArgumentException('bad input') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCrudOperation maps EntityNotFoundException to 404"() {
        when:
        ctrl.callCrud('read', 'Z9') { throw new EntityNotFoundException('missing') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    // Note: Groovy wraps thrown exceptions from closures (InvokerInvocationException),
    // which prevents precise testing of Kotlin's ExecutionException branch here.

    def "handleCrudOperation maps unexpected Exception to 500"() {
        when:
        ctrl.callCrud('read', 'Z9') { throw new RuntimeException('boom') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "handleCreateOperation returns 201 on success"() {
        when:
        def resp = ctrl.callCreate('Thing', 'T1') { return 42 }

        then:
        resp.statusCode == HttpStatus.CREATED
        resp.body == 42
    }

    def "handleCreateOperation maps DataIntegrityViolationException to 409"() {
        when:
        ctrl.callCreate('Thing', 'T1') { throw new DataIntegrityViolationException('dup') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.CONFLICT
    }

    def "handleCreateOperation maps ValidationException to 400"() {
        when:
        ctrl.callCreate('Thing', 'T1') { throw new ValidationException('bad') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCreateOperation maps IllegalArgumentException to 400"() {
        when:
        ctrl.callCreate('Thing', 'T1') { throw new IllegalArgumentException('bad input') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.BAD_REQUEST
    }

    def "handleCreateOperation maps unexpected Exception to 500"() {
        when:
        ctrl.callCreate('Thing', 'T1') { throw new RuntimeException('boom') }

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "handleDeleteOperation returns 200 and entity on success"() {
        given:
        def entity = [id: 'E1']

        when:
        def resp = ctrl.callDelete('Thing', 'E1', { Optional.of(entity) }, { /* delete ok */ })

        then:
        resp.statusCode == HttpStatus.OK
        resp.body == entity
    }

    def "handleDeleteOperation throws 404 when not found"() {
        when:
        ctrl.callDelete('Thing', 'E1', { Optional.empty() }, { /* no-op */ })

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.NOT_FOUND
    }

    def "handleDeleteOperation maps unexpected Exception to 500"() {
        when:
        ctrl.callDelete('Thing', 'E1', { Optional.of([id: 'E1']) }, { throw new RuntimeException('boom') })

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
