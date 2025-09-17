package finance.controllers

import finance.domain.User
import finance.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification

class UserControllerSpec extends Specification {

    def userService = GroovyMock(UserService)
    def controller = new UserController(userService)

    void "signIn echoes user and returns 200"() {
        given:
        def u = new User(username: 'alice', password: 'secret')

        when:
        def resp = controller.signIn(u)

        then:
        resp.statusCode == HttpStatus.OK
        resp.body.username == 'alice'
    }


    void "signUp maps generic Exception to 500"() {
        given:
        def u = new User(username: 'err', password: 'pw')
        userService.signUp(u) >> { throw new RuntimeException('boom') }

        when:
        controller.signUp(u)

        then:
        def ex = thrown(ResponseStatusException)
        ex.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
    }
}
