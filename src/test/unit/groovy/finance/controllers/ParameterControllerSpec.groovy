package finance.controllers

import finance.domain.Parameter
import finance.services.ParameterService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject

class StandardizedParameterControllerSpec extends Specification {

    static final String TEST_OWNER = "test_owner"

    finance.repositories.ParameterRepository parameterRepository = Mock()
    ParameterService parameterService = new ParameterService(parameterRepository)

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

        // Set up SecurityContext for TenantContext.getCurrentOwner()
        def auth = new UsernamePasswordAuthenticationToken(TEST_OWNER, null, [])
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private static Parameter param(Long id = 0L, String name = "p1", String value = "v1", boolean active = true) {
        new Parameter(parameterId: id, owner: TEST_OWNER, parameterName: name, parameterValue: value, activeStatus: active)
    }

    // ===== STANDARDIZED ENDPOINTS =====

    def "findAllActive returns list when present"() {
        given:
        List<Parameter> params = [param(1L, "p1", "v1"), param(2L, "p2", "v2")]
        and:
        parameterRepository.findByOwnerAndActiveStatusIsTrue(TEST_OWNER) >> params

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List<Parameter>).size() == 2
        (response.body as List<Parameter>)[0].parameterName == "p1"
    }

    def "findAllActive returns empty list when none"() {
        given:
        parameterRepository.findByOwnerAndActiveStatusIsTrue(TEST_OWNER) >> []

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List).isEmpty()
    }

    def "findAllActive returns empty list when service NotFound"() {
        given:
        parameterRepository.findByOwnerAndActiveStatusIsTrue(TEST_OWNER) >> { throw new jakarta.persistence.EntityNotFoundException("none") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.OK
        (response.body as List).isEmpty()
    }

    def "findAllActive returns 500 on system error"() {
        given:
        parameterRepository.findByOwnerAndActiveStatusIsTrue(TEST_OWNER) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.findAllActive()

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "findById returns parameter when found"() {
        given:
        Parameter p = param(10L, "alpha", "one")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "alpha") >> Optional.of(p)

        when:
        ResponseEntity<?> response = controller.findById("alpha")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterId == 10L
        (response.body as Parameter).parameterValue == "one"
    }

    def "findById returns 404 with error body when missing"() {
        given:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "beta") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.findById("beta")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body == null
    }

    def "findById returns 500 on system error"() {
        given:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "err") >> { throw new RuntimeException("boom") }

        when:
        ResponseEntity<?> response = controller.findById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "save creates parameter and returns 201"() {
        given:
        Parameter input = param(0L, "gamma", "three")
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
        Parameter invalid = param(0L, "", "")
        and:
        def violatingValidator = Mock(jakarta.validation.Validator) {
            validate(_ as Object) >> ([Mock(jakarta.validation.ConstraintViolation)] as Set)
        }
        parameterService.validator = violatingValidator

        when:
        ResponseEntity<?> response = controller.save(invalid)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "save handles conflict with 409 when unique violation"() {
        given:
        Parameter dup = param(0L, "delta", "four")
        and:
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new org.springframework.dao.DataIntegrityViolationException("duplicate") }

        when:
        ResponseEntity<?> response = controller.save(dup)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "save returns 500 on system error"() {
        given:
        Parameter input = param(0L, "sys", "v")
        and:
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.save(input)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 200 when parameter exists"() {
        given:
        Parameter existing = param(7L, "epsilon", "old")
        Parameter patch = param(7L, "epsilon", "new")
        and:
        // Controller first calls findByParameterNameStandardized -> findByOwnerAndParameterName
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "epsilon") >> Optional.of(existing)
        // Then calls update -> findByOwnerAndParameterId
        parameterRepository.findByOwnerAndParameterId(TEST_OWNER, 7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { Parameter p -> p }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterValue == "new"
    }

    def "update returns 404 when parameter missing"() {
        given:
        Parameter patch = param(0L, "zeta", "val")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "zeta") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.update("zeta", patch)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "update returns 500 when existence check errors"() {
        given:
        Parameter patch = param(1L, "alpha", "val")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "alpha") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.update("alpha", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "update returns 400 when validation fails"() {
        given:
        Parameter existing = param(7L, "epsilon", "old")
        Parameter patch = param(7L, "epsilon", "new")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "epsilon") >> Optional.of(existing)
        parameterRepository.findByOwnerAndParameterId(TEST_OWNER, 7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new jakarta.validation.ConstraintViolationException("bad", [] as Set) }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body == null
    }

    def "update returns 409 on business conflict"() {
        given:
        Parameter existing = param(7L, "epsilon", "old")
        Parameter patch = param(7L, "epsilon", "new")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "epsilon") >> Optional.of(existing)
        parameterRepository.findByOwnerAndParameterId(TEST_OWNER, 7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new org.springframework.dao.DataIntegrityViolationException("dup") }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body == null
    }

    def "update returns 500 on system error"() {
        given:
        Parameter existing = param(7L, "epsilon", "old")
        Parameter patch = param(7L, "epsilon", "new")
        and:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "epsilon") >> Optional.of(existing)
        parameterRepository.findByOwnerAndParameterId(TEST_OWNER, 7L) >> Optional.of(existing)
        parameterRepository.saveAndFlush(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.update("epsilon", patch)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body == null
    }

    def "deleteById returns 200 with deleted entity when found"() {
        given:
        Parameter existing = param(8L, "eta", "val")
        and:
        // findByParameterNameStandardized (find) + deleteByParameterNameStandardized (delete) both call findByOwnerAndParameterName
        2 * parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "eta") >> Optional.of(existing)

        when:
        ResponseEntity<?> response = controller.deleteById("eta")

        then:
        response.statusCode == HttpStatus.OK
        (response.body as Parameter).parameterId == 8L
    }

    def "deleteById returns 404 when not found"() {
        given:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "none") >> Optional.empty()

        when:
        ResponseEntity<?> response = controller.deleteById("none")

        then:
        response.statusCode == HttpStatus.NOT_FOUND
    }

    def "deleteById returns 500 when find errors"() {
        given:
        parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "err") >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("err")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "deleteById returns 500 when delete errors"() {
        given:
        Parameter existing = param(9L, "gone", "v")
        and:
        2 * parameterRepository.findByOwnerAndParameterName(TEST_OWNER, "gone") >> Optional.of(existing)
        parameterRepository.delete(_ as Parameter) >> { throw new RuntimeException("db") }

        when:
        ResponseEntity<?> response = controller.deleteById("gone")

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
