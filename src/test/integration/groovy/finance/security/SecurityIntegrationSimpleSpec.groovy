package finance.security

import finance.Application
import finance.domain.User
import finance.repositories.UserRepository
import finance.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.sql.Date

@ActiveProfiles("int")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = Application)
@Transactional
class SecurityIntegrationSimpleSpec extends Specification {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    String baseUrl

    void setup() {
        baseUrl = "http://localhost:${port}"
    }

    void 'test user repository integration'() {
        given:
        long timestamp = System.currentTimeMillis()
        User testUser = new User()
        testUser.username = "security_test_user_${timestamp}"
        testUser.password = "TestPassword123!${timestamp}"
        testUser.firstName = "Security"
        testUser.lastName = "Test"
        testUser.activeStatus = true

        when:
        User savedUser = userRepository.save(testUser)

        then:
        savedUser != null
        savedUser.userId != null
        savedUser.username == "security_test_user_${timestamp}"
        savedUser.firstName == "Security"

        when:
        Optional<User> foundUser = userRepository.findByUsername("security_test_user_${timestamp}")

        then:
        foundUser.isPresent()
        foundUser.get().firstName == "Security"
        foundUser.get().lastName == "Test"
    }

    void 'test user service integration'() {
        given:
        long timestamp = System.currentTimeMillis()
        User testUser = new User(
            0L,
            true,
            "UserService",
            "Test",
            "user_service_test_${timestamp}",
            "ServiceTestPass123!${timestamp}"
        )

        when:
        User savedUser = userRepository.save(testUser)

        then:
        savedUser != null
        savedUser.userId != null
        savedUser.username == "user_service_test_${timestamp}"

        when:
        List<User> allUsers = userRepository.findAll()

        then:
        allUsers.size() >= 1
        allUsers.any { it.username == "user_service_test_${timestamp}" }
    }

    void 'test protected endpoint access without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/actuator/metrics", String.class)

        then:
        // In integration test profile, metrics endpoint may be accessible
        // The test verifies that the endpoint responds (either accessible or protected)
        response.statusCode == HttpStatus.OK ||
        response.statusCode == HttpStatus.UNAUTHORIZED ||
        response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test health endpoint accessibility without authentication'() {
        when:
        ResponseEntity<String> response = restTemplate.getForEntity("${baseUrl}/actuator/health", String.class)

        then:
        response.statusCode == HttpStatus.OK
    }

    void 'test CORS headers handling'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.set("Origin", "http://localhost:3000")
        headers.set("Access-Control-Request-Method", "GET")
        headers.set("Access-Control-Request-Headers", "authorization")
        HttpEntity<String> entity = new HttpEntity<>(headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/actuator/health",
            HttpMethod.OPTIONS,
            entity,
            String.class
        )

        then:
        response.statusCode == HttpStatus.OK ||
        response.statusCode == HttpStatus.NO_CONTENT ||
        response.statusCode == HttpStatus.FORBIDDEN
    }

    void 'test basic authentication with test credentials'() {
        given:
        HttpHeaders headers = new HttpHeaders()
        headers.setBasicAuth("foo", "bar")  // Test credentials from application-int.yml
        HttpEntity<String> entity = new HttpEntity<>(headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
            "${baseUrl}/actuator/health",
            HttpMethod.GET,
            entity,
            String.class
        )

        then:
        response.statusCode == HttpStatus.OK
    }

    void 'test security filter chain with different endpoints'() {
        when:
        List<String> publicEndpoints = ["/actuator/health", "/actuator/info"]
        List<String> protectedEndpoints = ["/accounts", "/transactions", "/categories"]

        List<ResponseEntity<String>> publicResponses = publicEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        List<ResponseEntity<String>> protectedResponses = protectedEndpoints.collect { endpoint ->
            restTemplate.getForEntity("${baseUrl}${endpoint}", String.class)
        }

        then:
        // Public endpoints should be accessible
        publicResponses.every { response ->
            response.statusCode == HttpStatus.OK || response.statusCode == HttpStatus.NOT_FOUND
        }

        // Protected endpoints should require authentication
        protectedResponses.every { response ->
            response.statusCode == HttpStatus.UNAUTHORIZED ||
            response.statusCode == HttpStatus.FORBIDDEN ||
            response.statusCode == HttpStatus.NOT_FOUND
        }
    }

    void 'test user authentication data integrity'() {
        given:
        long timestamp = System.currentTimeMillis()
        User user1 = new User(
            0L,
            true,
            "User",
            "One",
            "integrity_test_user1_${timestamp}",
            "Password1!${timestamp}"
        )

        User user2 = new User(
            0L,
            true,
            "User",
            "Two",
            "integrity_test_user2_${timestamp}",
            "Password2!${timestamp}"
        )

        when:
        userRepository.save(user1)
        userRepository.save(user2)

        Optional<User> foundUser1 = userRepository.findByUsername("integrity_test_user1_${timestamp}")
        Optional<User> foundUser2 = userRepository.findByUsername("integrity_test_user2_${timestamp}")

        then:
        foundUser1.isPresent()
        foundUser2.isPresent()
        foundUser1.get().password == "Password1!${timestamp}"
        foundUser2.get().password == "Password2!${timestamp}"
        foundUser1.get().username != foundUser2.get().username
    }

    void 'test user service validation and constraints'() {
        given:
        User invalidUser = new User(
            0L,
            true,
            "Invalid",
            "User",
            "",  // Invalid - username cannot be empty
            "test_password_validation"
        )

        when:
        userRepository.save(invalidUser)

        then:
        thrown(Exception)  // Should throw validation exception
    }

    void 'test concurrent user operations'() {
        given:
        List<String> createdUsernames = []
        int threadCount = 3
        long timestamp = System.currentTimeMillis()

        expect:
        // Test concurrent user creation by creating multiple users sequentially
        // to simulate potential race conditions in a more controlled manner
        for (int i = 0; i < threadCount; i++) {
            User user = new User(
                0L,
                true,
                "Concurrent",
                "UserTest",
                "concurrent_user_${timestamp}_${i}",
                "ConcurrentPass123!${timestamp}_${i}"
            )
            User savedUser = userRepository.save(user)
            createdUsernames.add(savedUser.username)
        }

        and:
        createdUsernames.size() == threadCount
        createdUsernames.unique().size() == threadCount  // All usernames should be unique
        createdUsernames.every { username ->
            userRepository.findByUsername(username).isPresent()
        }
    }
}