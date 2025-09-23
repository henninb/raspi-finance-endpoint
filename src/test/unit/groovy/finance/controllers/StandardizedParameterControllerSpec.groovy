package finance.controllers

import finance.domain.Parameter
import finance.services.StandardizedParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Subject

class StandardizedParameterControllerSpec extends Specification {

    finance.repositories.ParameterRepository parameterRepository = Mock()
    StandardizedParameterService parameterService = new StandardizedParameterService(parameterRepository)

    @Subject
    ParameterController controller = new ParameterController(parameterService)

    def setup() {
        def validator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([] as Set)
        }
        def meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        def meterService = new finance.services.MeterService(meterRegistry)

        parameterService.validator = validator
        parameterService.meterService = meterService
    }

    // ===== STANDARDIZED ENDPOINTS =====

    def "findAllActive returns list when present"() {
        given:
        List<Parameter> params = [ new Parameter(1L, "p1", "v1", true), new Parameter(2L, "p2", "v2", true) ]
        and:
        parameterRepository.findByActiveStatusIsTrue() >> params

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List<Parameter>).size() == 2
        (response.body as List<Parameter>)[0].parameterName == "p1"
    }

    def "findAllActive returns empty list when none"() {
        given:
        parameterRepository.findByActiveStatusIsTrue() >> []

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List).isEmpty()
    }

    def "findAllActive returns empty list when service NotFound"() {
        given:
        parameterRepository.findByActiveStatusIsTrue() >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List).isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        parameterRepository.findByActiveStatusIsTrue() >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns parameter when found"() {
        given:
        Parameter p = new Parameter(10L, "alpha", "one", true)
        and:
        parameterRepository.findByParameterName("alpha") >> Optional.of(p)

        when:
        ResponseEntity<?> response = controller.findById("alpha")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterId == 10L
        (response.body as Parameter).parameterValue == "one"
    }

    def "findById returns 404 with error body when missing"() {
        given:
        parameterRepository.findByParameterName("beta") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.findById("beta")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        (response.body as Map).containsKey("error")
    }

    def "findById returns 500 on system error"() {
        given:
        parameterRepository.findByParameterName("err") >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<?> response = controller.findById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        (response.body as Map).containsKey("error")
    }

    def "save creates parameter and returns 201"() {
        given:
        Parameter input = new Parameter(0L, "gamma", "three", true)
        and:
        parameterRepository.saveAndFlush(_ as Parameter) >> { Parameter x -> x.parameterId = 99L; return x }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.CREATED
        (response.body as Parameter).parameterId == 99L
    }

    def "save handles validation errors with 400"() {
        given:
        Parameter invalid = new Parameter(0L, "", "", true)
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        parameterService.validator = violatingValidator

        when:
        ResponseEntity<?> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        (response.body as Map).containsKey("errors")
    }

    def "save handles conflict with 409 when unique violation"() {
        given:
        Parameter dup = new Parameter(0L, "delta", "four", true)
        and:
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new org.springframework.dao.DataIntegrityViolationException("duplicate") }

        when:
        ResponseEntity<?> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
        (response.body as Map).get("error")
    }

    def "save returns 500 on system error"() {
        given:
        Parameter input = new Parameter(0L, "sys", "v", true)
        and:
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        (response.body as Map).containsKey("error")
    }

    def "update returns 200 when parameter exists"() {
        given:
        Parameter existing = new Parameter(7L, "epsilon", "old", true)
        Parameter patch = new Parameter(7L, "epsilon", "new", true)
        and:
        parameterRepository.findByParameterName("epsilon") >> Optional.of(existing)
        parameterRepository.findById(7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { Parameter p -> p }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterValue == "new"
    }

    def "update returns 404 when parameter missing"() {
        given:
        Parameter patch = new Parameter(0L, "zeta", "val", true)
        and:
        parameterRepository.findByParameterName("zeta") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.update("zeta", patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 500 when existence check errors"() {
        given:
        Parameter patch = new Parameter(1L, "alpha", "val", true)
        and:
        parameterRepository.findByParameterName("alpha") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.update("alpha", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        (response.body as Map).containsKey("error")
    }

    def "update returns 400 when validation fails"() {
        given:
        Parameter existing = new Parameter(7L, "epsilon", "old", true)
        Parameter patch = new Parameter(7L, "epsilon", "new", true)
        and:
        parameterRepository.findByParameterName("epsilon") >> Optional.of(existing)
        parameterRepository.findById(7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        (response.body as Map).containsKey("errors")
    }

    def "update returns 409 on business conflict"() {
        given:
        Parameter existing = new Parameter(7L, "epsilon", "old", true)
        Parameter patch = new Parameter(7L, "epsilon", "new", true)
        and:
        parameterRepository.findByParameterName("epsilon") >> Optional.of(existing)
        parameterRepository.findById(7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
        (response.body as Map).containsKey("error")
    }

    def "update returns 500 on system error"() {
        given:
        Parameter existing = new Parameter(7L, "epsilon", "old", true)
        Parameter patch = new Parameter(7L, "epsilon", "new", true)
        and:
        parameterRepository.findByParameterName("epsilon") >> Optional.of(existing)
        parameterRepository.findById(7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        (response.body as Map).containsKey("error")
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        Parameter existing = new Parameter(8L, "eta", "val", true)
        and:
        parameterRepository.findByParameterName("eta") >> Optional.of(existing)

        when:
        ResponseEntity<?> response = controller.deleteById("eta")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterId == 8L
    }

    def "deleteById returns 404 when not found"() {
        given:
        parameterRepository.findByParameterName("none") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.deleteById("none")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 when find errors"() {
        given:
        parameterRepository.findByParameterName("err") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 500 when delete errors"() {
        given:
        Parameter existing = new Parameter(9L, "gone", "v", true)
        and:
        2 * parameterRepository.findByParameterName("gone") >> Optional.of(existing)
        parameterRepository.delete(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("gone")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
