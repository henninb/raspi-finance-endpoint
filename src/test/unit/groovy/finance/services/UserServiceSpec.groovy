package finance.services

import finance.domain.User
import finance.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

import java.util.*

class UserServiceSpec extends BaseServiceSpec {

    UserRepository userRepositoryMock = GroovyMock(UserRepository)
    PasswordEncoder passwordEncoderMock = GroovyMock(PasswordEncoder)

    UserService userService

    def setup() {
        userService = new UserService(userRepositoryMock, passwordEncoderMock)
        userService.meterService = meterService
    }

    def "signIn - success with valid credentials"() {
        given:
        def username = "testuser"
        def rawPassword = "password123"
        def hashedPassword = "\$2a\$10\$hashedPasswordExample"
        def inputUser = new User(username: username, password: rawPassword)
        def dbUser = new User(username: username, password: hashedPassword)

        when:
        def result = userService.signIn(inputUser)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.of(dbUser)
        1 * passwordEncoderMock.matches(rawPassword, hashedPassword) >> true

        result.isPresent()
        result.get().username == username
    }

    def "signIn - failure with invalid password"() {
        given:
        def username = "testuser"
        def rawPassword = "wrongpassword"
        def hashedPassword = "\$2a\$10\$hashedPasswordExample"
        def inputUser = new User(username: username, password: rawPassword)
        def dbUser = new User(username: username, password: hashedPassword)

        when:
        def result = userService.signIn(inputUser)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.of(dbUser)
        1 * passwordEncoderMock.matches(rawPassword, hashedPassword) >> false

        !result.isPresent()
    }

    def "signIn - failure with non-existent user"() {
        given:
        def username = "nonexistentuser"
        def rawPassword = "password123"
        def inputUser = new User(username: username, password: rawPassword)

        when:
        def result = userService.signIn(inputUser)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.empty()
        1 * passwordEncoderMock.matches(rawPassword, '$2a$12$dummy.hash.to.prevent.timing.attacks.with.constant.time.processing') >> false

        !result.isPresent()
    }

    def "signUp - success with new username"() {
        given:
        def username = "newuser"
        def rawPassword = "password123"
        def hashedPassword = "\$2a\$10\$hashedPasswordExample"
        def inputUser = new User(username: username, password: rawPassword)
        def savedUser = new User(userId: 1L, username: username, password: hashedPassword)

        when:
        def result = userService.signUp(inputUser)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.empty()
        1 * passwordEncoderMock.encode(rawPassword) >> hashedPassword
        1 * userRepositoryMock.saveAndFlush(inputUser) >> savedUser

        result.userId == 1L
        result.username == username
        inputUser.password == hashedPassword // password should be hashed
    }

    def "signUp - failure with existing username"() {
        given:
        def username = "existinguser"
        def rawPassword = "password123"
        def inputUser = new User(username: username, password: rawPassword)
        def existingUser = new User(userId: 1L, username: username, password: "existingHash")

        when:
        userService.signUp(inputUser)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.of(existingUser)
        0 * passwordEncoderMock.encode(*_)
        0 * userRepositoryMock.saveAndFlush(*_)

        thrown(IllegalArgumentException)
    }

    def "findUserByUsername - success with existing user"() {
        given:
        def username = "testuser"
        def hashedPassword = "\$2a\$10\$hashedPasswordExample"
        def dbUser = new User(userId: 1L, username: username, password: hashedPassword)

        when:
        def result = userService.findUserByUsername(username)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.of(dbUser)

        result != null
        result.username == username
        result.password == "" // password should be cleared for security
    }

    def "findUserByUsername - returns null for non-existent user"() {
        given:
        def username = "nonexistentuser"

        when:
        def result = userService.findUserByUsername(username)

        then:
        1 * userRepositoryMock.findByUsername(username) >> Optional.empty()

        result == null
    }
}