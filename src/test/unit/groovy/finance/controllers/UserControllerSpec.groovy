package finance.controllers

import finance.domain.User
import finance.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll

class UserControllerSpec extends Specification {

    def userService = GroovyMock(UserService)
    def controller = new UserController(userService)

    void "signUp maps generic Exception to 500"() {
        given:
        def bindingResult = Mock(BindingResult)
        bindingResult.hasErrors() >> false
        def u = new User(username: 'err', password: 'Passw0rd!')
        userService.signUp(_) >> { throw new RuntimeException('boom') }

        when:
        controller.signUp(u, bindingResult)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }

    void "signUp returns 400 when BindingResult has validation errors"() {
        given:
        def bindingResult = Mock(BindingResult)
        bindingResult.hasErrors() >> true
        bindingResult.getFieldErrors() >> [new FieldError("user", "password", "Password must be at least 8 characters")]
        def u = new User(username: 'bad', password: 'weak')

        when:
        def result = controller.signUp(u, bindingResult)

        then:
        result.statusCode == HttpStatus.BAD_REQUEST
        0 * userService.signUp(_)
    }

    @Unroll
    void "signUp rejects pre-encoded BCrypt password with prefix '#prefix'"() {
        given:
        def bindingResult = Mock(BindingResult)
        bindingResult.hasErrors() >> false
        def encodedPassword = "${prefix}10\$abcdefghijklmnopqrstuv.WXYZ01234567890123456789012"
        def u = new User(username: 'attacker', password: encodedPassword)

        when:
        def result = controller.signUp(u, bindingResult)

        then:
        result.statusCode == HttpStatus.BAD_REQUEST
        0 * userService.signUp(_)

        where:
        prefix << ["\$2a\$", "\$2b\$", "\$2y\$"]
    }
}
