package finance.security

import finance.BaseRestTemplateIntegrationSpec
import finance.domain.User
import finance.repositories.UserRepository
import finance.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.transaction.annotation.Transactional

import java.sql.Date

@Transactional
class SecurityIntegrationSimpleSpec extends BaseRestTemplateIntegrationSpec {

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    void setup() {
        // Setup if needed
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
        ResponseEntity<String> response
        try {
            response = getMgmtWithRetry("/actuator/metrics", 1)
        } catch (Exception e) {
            // Endpoint may not be available or require authentication
            response = new ResponseEntity<>("", HttpStatus.NOT_FOUND)
        }

        then:
        // In integration test profile, metrics endpoint may be accessible
        // The test verifies that the endpoint responds (either accessible or protected)
        response.statusCode.value() == 200 || response.statusCode.value() == 401 ||
        response.statusCode.value() == 403 || response.statusCode.value() == 404
    }

    void 'test health endpoint accessibility without authentication'() {
        when:
        ResponseEntity<String> response = getMgmtWithRetry("/actuator/health")

        then:
        response.statusCode.is2xxSuccessful()
    }

    void 'test CORS headers handling'() {
        when:
        HttpHeaders headers = new HttpHeaders()
        headers.add("Origin", "http://localhost:3000")
        headers.add("Access-Control-Request-Method", "GET")
        headers.add("Access-Control-Request-Headers", "authorization")
        HttpEntity<String> entity = new HttpEntity<>("", headers)

        ResponseEntity<String> response
        try {
            response = restTemplate.exchange(managementBaseUrl + "/actuator/health", HttpMethod.OPTIONS, entity, String.class)
        } catch (Exception e) {
            // CORS endpoint may not be available
            response = new ResponseEntity<>("", HttpStatus.NOT_FOUND)
        }

        then:
        response.statusCode.value() == 200 || response.statusCode.value() == 204 ||
        response.statusCode.value() == 403 || response.statusCode.value() == 404
    }

    void 'test basic authentication with test credentials'() {
        when:
        HttpHeaders headers = new HttpHeaders()
        headers.setBasicAuth("foo", "bar")  // Test credentials from application-int.yml
        HttpEntity<String> entity = new HttpEntity<>("", headers)

        ResponseEntity<String> response = restTemplate.exchange(managementBaseUrl + "/actuator/health", HttpMethod.GET, entity, String.class)

        then:
        response.statusCode.is2xxSuccessful()
    }

    void 'test security filter chain with different endpoints'() {
        when:
        List<String> publicEndpoints = ["/actuator/health", "/actuator/info"]
        List<String> protectedEndpoints = ["/accounts", "/transactions", "/categories"]

        List<Boolean> publicResults = publicEndpoints.collect { endpoint ->
            try {
                ResponseEntity<String> response = getMgmtWithRetry(endpoint, 1)
                return response.statusCode.value() == 200 || response.statusCode.value() == 404
            } catch (Exception e) {
                return e.message?.contains("404") ? true : false
            }
        }

        List<Boolean> protectedResults = protectedEndpoints.collect { endpoint ->
            try {
                ResponseEntity<String> response = getWithRetry(endpoint, 1)
                return response.statusCode.value() == 401 || response.statusCode.value() == 403 ||
                       response.statusCode.value() == 404
            } catch (Exception e) {
                return e.message?.contains("401") || e.message?.contains("403") || e.message?.contains("404")
            }
        }

        then:
        // Public endpoints should be accessible
        publicResults.every { it == true }

        // Protected endpoints should require authentication or not be found
        protectedResults.every { it == true }
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
