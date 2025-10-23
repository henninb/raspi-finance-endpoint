package finance.controllers

import finance.domain.User
import finance.repositories.UserRepository
import finance.services.TokenBlacklistService
import finance.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Unroll
import jakarta.servlet.http.HttpServletResponse
import org.springframework.validation.BindingResult
import finance.domain.LoginRequest
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class LoginControllerSpec extends Specification {
    private UserRepository userRepository = Mock()
    private UserService userService = new UserService(userRepository, new BCryptPasswordEncoder())
    private TokenBlacklistService tokenBlacklistService = Mock()
    private LoginController loginController = new LoginController(userService, tokenBlacklistService)
    private HttpServletResponse response = Mock()
    private BindingResult bindingResult = Mock()

    def setup() {
        loginController.jwtKey = "test_jwt_key_for_login_controller_spec_test_jwt_key_for_login_controller_spec"
        loginController.activeProfile = "dev"
    }

    def "login should return OK on successful authentication"() {
        given:
        def loginRequest = new LoginRequest("testuser", "password123")
        def user = new User(username: "testuser", password: new BCryptPasswordEncoder().encode("password123"))
        userRepository.findByUsername("testuser") >> Optional.of(user)

        when:
        ResponseEntity<Map<String, String>> result = loginController.login(loginRequest, bindingResult, response)

        then:
        result.statusCode == HttpStatus.OK
        result.body["message"] == "Login successful"
    }

    def "login should return UNAUTHORIZED for invalid credentials"() {
        given:
        def loginRequest = new LoginRequest("wronguser", "wrongpassword")
        userRepository.findByUsername("wronguser") >> Optional.empty()

        when:
        ResponseEntity<Map<String, String>> result = loginController.login(loginRequest, bindingResult, response)

        then:
        result.statusCode == HttpStatus.UNAUTHORIZED
        result.body["error"] == "Invalid credentials"
    }

    def "login should return BAD_REQUEST for validation errors"() {
        given:
        def loginRequest = new LoginRequest("", "")
        bindingResult.hasErrors() >> true
        bindingResult.getFieldErrors() >> []

        when:
        ResponseEntity<Map<String, String>> result = loginController.login(loginRequest, bindingResult, response)

        then:
        result.statusCode == HttpStatus.BAD_REQUEST
    }

    def "logout should clear token and return NO_CONTENT"() {
        given:
        def request = Mock(jakarta.servlet.http.HttpServletRequest)
        def key = Keys.hmacShaKeyFor(loginController.jwtKey.bytes)
        def token = Jwts.builder()
            .claim("username", "testuser")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()
        request.getCookies() >> null
        request.getHeader("Cookie") >> "token=${token}"
        request.getHeader("Authorization") >> null

        when:
        ResponseEntity<Void> result = loginController.logout(request, response)

        then:
        1 * tokenBlacklistService.blacklistToken(token, _)
        result.statusCode == HttpStatus.NO_CONTENT
    }

    def "register should return CREATED on successful registration"() {
        given:
        def newUser = new User(username: "newuser", password: "Password123!", firstName: "first", lastName: "last")
        userRepository.findByUsername("newuser") >> Optional.empty()
        userRepository.saveAndFlush(_) >> newUser

        when:
        ResponseEntity<Map<String, String>> result = loginController.register(newUser, bindingResult, response)

        then:
        result.statusCode == HttpStatus.CREATED
        result.body["message"] == "Registration successful"
    }

    def "register should return CONFLICT if username already exists"() {
        given:
        def existingUser = new User(username: "existinguser", password: "Password123!", firstName: "first", lastName: "last")
        userRepository.findByUsername("existinguser") >> Optional.of(existingUser)

        when:
        ResponseEntity<Map<String, String>> result = loginController.register(existingUser, bindingResult, response)

        then:
        result.statusCode == HttpStatus.CONFLICT
        result.body["error"] == "Username already exists"
    }

    @Unroll
    def "register should return BAD_REQUEST for invalid password '#password'"() {
        given:
        def invalidUser = new User(username: "testuser", password: password, firstName: "first", lastName: "last")

        when:
        ResponseEntity<Map<String, String>> result = loginController.register(invalidUser, bindingResult, response)

        then:
        result.statusCode == HttpStatus.BAD_REQUEST
        result.body["error"] == "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"

        where:
        password << ["weak", "password123", "Password!", "PASSWORD123"]
    }

    def "getCurrentUser should return user details for valid token"() {
        given:
        def user = new User(username: "testuser", password: "hashed_password")
        def key = Keys.hmacShaKeyFor(loginController.jwtKey.bytes)
        def token = Jwts.builder().claim("username", "testuser").setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis() + 3600000)).signWith(key).compact()
        userRepository.findByUsername("testuser") >> Optional.of(user)

        when:
        ResponseEntity<Object> result = loginController.getCurrentUser(token)

        then:
        result.statusCode == HttpStatus.OK
        result.body == user
    }

    def "getCurrentUser should return UNAUTHORIZED for missing token"() {
        when:
        ResponseEntity<Object> result = loginController.getCurrentUser(null)

        then:
        result.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "getCurrentUser should return UNAUTHORIZED for invalid token"() {
        when:
        ResponseEntity<Object> result = loginController.getCurrentUser("invalid_token")

        then:
        result.statusCode == HttpStatus.UNAUTHORIZED
    }
}
